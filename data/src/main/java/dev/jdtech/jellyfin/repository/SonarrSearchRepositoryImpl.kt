package dev.jdtech.jellyfin.repository

import dev.jdtech.jellyfin.api.pvr.PvrApiException
import dev.jdtech.jellyfin.api.pvr.SonarrApi
import dev.jdtech.jellyfin.api.pvr.SonarrRelease
import dev.jdtech.jellyfin.models.AutomaticSearchOutcome
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * [sonarrApiKeyProvider] resolves the current secret from `SecureCredentialStore` - passed in as a
 * plain lambda (rather than depending on `SecureCredentialStore` directly) because that type
 * lives in `core`, which depends on `data`, not the other way around. Same pattern as
 * [SeasonEpisodesRepositoryImpl]/[CalendarRepositoryImpl]/`QueueStatusRepositoryImpl`.
 *
 * [scheduleCompletionCheck] enqueues `dev.jdtech.jellyfin.work.AutomaticSearchWorker` - also a
 * lambda, for the same reason: WorkManager/notifications are `core`-layer concerns `data` can't
 * depend on directly.
 *
 * Constructed via `dev.jdtech.jellyfin.di.SonarrSearchModule` (a Hilt `@Provides`) rather than an
 * `@Inject` constructor, since `data` has no Hilt plugin.
 */
class SonarrSearchRepositoryImpl(
    private val appPreferences: AppPreferences,
    private val sonarrApiKeyProvider: () -> String?,
    private val scheduleCompletionCheck: (episodeId: Int, commandId: Int) -> Unit,
) : SonarrSearchRepository {
    // Keyed by Sonarr episode id. This repository is a Hilt singleton (see SonarrSearchModule),
    // so the cache lives for the app process, for as long as AppPreferences.pvrReleaseCacheMinutes
    // (Settings > Integrations) says - long enough to avoid re-hitting a slow indexer chain when
    // the user briefly re-opens the release picker for the same episode.
    private val releaseCache = ConcurrentHashMap<Int, CachedReleases>()

    private data class CachedReleases(val releases: List<SonarrRelease>, val fetchedAtMs: Long)

    override suspend fun resolveEpisodeId(
        seriesTvdbId: String,
        seasonNumber: Int,
        episodeNumber: Int,
    ): Int? {
        val api = api() ?: return null
        val seriesId = api.getSeries().firstOrNull { it.tvdbId.toString() == seriesTvdbId }?.id ?: return null
        return api
            .getEpisodes(seriesId)
            .firstOrNull { it.seasonNumber == seasonNumber && it.episodeNumber == episodeNumber }
            ?.id
    }

    override suspend fun searchEpisode(episodeId: Int): Result<Unit> {
        val result = runAction { it.searchEpisode(episodeId) }
        result.onSuccess { commandId ->
            // An automatic search may grab something new - drop any cached manual-search results
            // for this episode so a follow-up manual search reflects that.
            releaseCache.remove(episodeId)
            scheduleCompletionCheck(episodeId, commandId)
        }
        return result.map {}
    }

    override suspend fun awaitAutomaticSearchResult(
        episodeId: Int,
        commandId: Int,
    ): Result<AutomaticSearchOutcome> = runAction { api ->
        var status = api.getCommandStatus(commandId)
        val deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MS
        while (status.status !in TERMINAL_COMMAND_STATUSES && System.currentTimeMillis() < deadline) {
            delay(POLL_INTERVAL_MS)
            status = api.getCommandStatus(commandId)
        }
        val episode = api.getEpisodeById(episodeId)
        AutomaticSearchOutcome(
            succeeded = status.status == "completed",
            seriesTitle = episode.series?.title,
            seasonNumber = episode.seasonNumber,
            episodeNumber = episode.episodeNumber,
            episodeTitle = episode.title,
        )
    }

    override suspend fun getReleases(episodeId: Int): Result<List<SonarrRelease>> {
        val cacheTtlMs = appPreferences.getValue(appPreferences.pvrReleaseCacheMinutes) * 60_000L
        releaseCache[episodeId]?.let { cached ->
            if (System.currentTimeMillis() - cached.fetchedAtMs < cacheTtlMs) {
                return Result.success(cached.releases)
            }
        }
        val timeoutMs = appPreferences.getValue(appPreferences.pvrSearchTimeout)
        return runAction { it.getReleases(episodeId, readTimeoutMs = timeoutMs) }
            .onSuccess { releases ->
                releaseCache[episodeId] = CachedReleases(releases, System.currentTimeMillis())
            }
    }

    override suspend fun grabRelease(release: SonarrRelease): Result<Unit> =
        runAction { it.grabRelease(release.guid, release.indexerId) }
            .onSuccess {
                // Don't know which episode this release belonged to from here - clear everything
                // rather than risk showing a stale list that still offers an already-grabbed release.
                releaseCache.clear()
            }

    private suspend fun <T> runAction(block: suspend (SonarrApi) -> T): Result<T> {
        val api = api() ?: return Result.failure(IllegalStateException("Sonarr is not configured"))
        return try {
            Result.success(block(api))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Sonarr search action failed")
            Result.failure(mapError(e))
        }
    }

    /**
     * Turns raw network exceptions into messages worth showing the user directly (as a toast) -
     * [SocketTimeoutException] in particular is common with slow indexers (e.g. Prowlarr proxying
     * several trackers for an interactive search) and "timeout" alone isn't an obvious cause.
     */
    private fun mapError(e: Exception): Throwable =
        when {
            e is SocketTimeoutException ->
                IOException("Sonarr timed out - it or one of its indexers may be slow to respond", e)
            e is UnknownHostException -> IOException("Could not reach Sonarr - check the server URL", e)
            // A reverse proxy in front of Sonarr (or Sonarr's own proxy to an indexer) gave up
            // before Sonarr finished polling indexers - distinct from SocketTimeoutException,
            // which is *this* client giving up, so it needs its own message pointing at the
            // proxy's timeout setting rather than Sonarr/the network.
            e is PvrApiException && e.httpCode in GATEWAY_ERROR_CODES ->
                IOException(
                    "Sonarr's reverse proxy timed out (HTTP ${e.httpCode}) before the search finished - " +
                        "try again, or raise the proxy's timeout if this keeps happening",
                    e,
                )
            else -> e
        }

    private fun api(): SonarrApi? {
        if (!appPreferences.getValue(appPreferences.sonarrEnabled)) return null
        val baseUrl = appPreferences.getValue(appPreferences.sonarrBaseUrl)
        val apiKey = sonarrApiKeyProvider()
        if (baseUrl.isNullOrBlank() || apiKey.isNullOrBlank()) return null
        return SonarrApi(baseUrl, apiKey)
    }

    private companion object {
        val GATEWAY_ERROR_CODES = setOf(502, 503, 504)

        // Sonarr's terminal command states - see https://sonarr.tv/docs/api - anything else
        // (queued/started) means the search is still in progress.
        val TERMINAL_COMMAND_STATUSES = setOf("completed", "failed", "aborted", "cancelled", "orphaned")

        const val POLL_INTERVAL_MS = 5_000L
        // Well above any realistic search duration - a safety net against polling forever if
        // Sonarr's command never reaches a terminal state for some reason.
        const val AWAIT_TIMEOUT_MS = 15 * 60 * 1000L
    }
}
