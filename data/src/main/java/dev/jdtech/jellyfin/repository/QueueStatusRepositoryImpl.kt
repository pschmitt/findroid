package dev.jdtech.jellyfin.repository

import dev.jdtech.jellyfin.api.pvr.RadarrApi
import dev.jdtech.jellyfin.api.pvr.SonarrApi
import dev.jdtech.jellyfin.api.pvr.SonarrQueueItem
import dev.jdtech.jellyfin.api.pvr.SonarrSeries
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.models.PvrQueueEntry
import dev.jdtech.jellyfin.models.QueueStatus
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jellyfin.sdk.model.api.BaseItemKind
import timber.log.Timber

/**
 * [sonarrApiKeyProvider]/[radarrApiKeyProvider] resolve the current secret from
 * `SecureCredentialStore` - passed in as plain lambdas (rather than depending on
 * `SecureCredentialStore` directly) because that type lives in `core`, which depends on `data`,
 * not the other way around. Everything else this needs (`AppPreferences`, the PVR API clients,
 * `JellyfinRepository`) is already reachable from `data`.
 *
 * Constructed via [dev.jdtech.jellyfin.di.QueueStatusModule] (a Hilt `@Provides`, mirroring
 * `AutoDownloadRuleModule`) rather than an `@Inject` constructor, since `data` has no Hilt plugin.
 *
 * Match candidates are fetched from the *live* Jellyfin library via [JellyfinRepository] rather
 * than the local Room cache (which only holds downloaded items) - same reasoning as
 * `CalendarRepositoryImpl`, which hit this exact bug first. To keep the per-poll request count
 * bounded, only shows/seasons actually referenced by the current queue get their episodes
 * fetched.
 */
class QueueStatusRepositoryImpl(
    private val appPreferences: AppPreferences,
    private val jellyfinRepository: JellyfinRepository,
    private val sonarrApiKeyProvider: () -> String?,
    private val radarrApiKeyProvider: () -> String?,
    private val scope: CoroutineScope,
) : QueueStatusRepository {

    private val _queueEntries = MutableStateFlow<List<PvrQueueEntry>>(emptyList())
    private val refreshMutex = Mutex()
    private val pollingStarted = AtomicBoolean(false)

    override fun getQueueEntriesFlow(): Flow<List<PvrQueueEntry>> =
        _queueEntries.onStart { ensurePollingStarted() }

    override fun getQueueStatusFlow(): Flow<Map<UUID, QueueStatus>> =
        getQueueEntriesFlow().map { it.toQueueStatusMap() }.distinctUntilChanged()

    override fun getQueueStatusFlow(itemId: UUID): Flow<QueueStatus?> =
        getQueueStatusFlow().map { it[itemId] }.distinctUntilChanged()

    override suspend fun refreshNow() {
        // Serializes concurrent callers (poll loop, WorkManager backstop, a manual pull-to-refresh)
        // so two overlapping fetches can't race to publish a stale result after a fresher one.
        refreshMutex.withLock { _queueEntries.value = fetchQueueEntries() }
    }

    private fun ensurePollingStarted() {
        if (!pollingStarted.compareAndSet(false, true)) return
        scope.launch {
            while (isActive) {
                try {
                    refreshNow()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.w(e, "Queue status poll iteration failed")
                }
                val intervalMinutes =
                    appPreferences
                        .getValue(appPreferences.pvrPollIntervalMinutes)
                        .coerceAtLeast(MIN_POLL_INTERVAL_MINUTES)
                delay(intervalMinutes * 60_000L)
            }
        }
    }

    private suspend fun fetchQueueEntries(): List<PvrQueueEntry> = coroutineScope {
        // Each service is independently try/caught inside its own fetch function - a failure in
        // one must never blank out or crash the other's contribution to the merged list.
        val sonarrDeferred = async { fetchSonarrEntries() }
        val radarrDeferred = async { fetchRadarrEntries() }
        sonarrDeferred.await() + radarrDeferred.await()
    }

    private suspend fun fetchSonarrEntries(): List<PvrQueueEntry> {
        if (!appPreferences.getValue(appPreferences.sonarrEnabled)) return emptyList()
        val baseUrl = appPreferences.getValue(appPreferences.sonarrBaseUrl)
        val apiKey = sonarrApiKeyProvider()
        if (baseUrl.isNullOrBlank() || apiKey.isNullOrBlank()) return emptyList()

        return try {
            val api = SonarrApi(baseUrl, apiKey)
            val queue = api.getQueue()
            if (queue.isEmpty()) return emptyList()
            val series = api.getSeries()
            val (shows, episodesByShowId) = loadQueueReferencedShowsAndEpisodes(series, queue)
            matchSonarr(series, queue, shows, episodesByShowId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Failed to refresh Sonarr queue status")
            emptyList()
        }
    }

    private suspend fun fetchRadarrEntries(): List<PvrQueueEntry> {
        if (!appPreferences.getValue(appPreferences.radarrEnabled)) return emptyList()
        val baseUrl = appPreferences.getValue(appPreferences.radarrBaseUrl)
        val apiKey = radarrApiKeyProvider()
        if (baseUrl.isNullOrBlank() || apiKey.isNullOrBlank()) return emptyList()

        return try {
            val api = RadarrApi(baseUrl, apiKey)
            val queue = api.getQueue()
            if (queue.isEmpty()) return emptyList()
            val movies = api.getMovie()
            matchRadarr(movies, queue, loadJellyfinMovies())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Failed to refresh Radarr queue status")
            emptyList()
        }
    }

    /**
     * Fetches the Jellyfin shows and episodes the current [queue] can possibly match against:
     * the full show list is one request, but episodes are fetched only for the shows *and
     * seasons* the queue references, since episode listing is one request per season and a
     * long-running show can have dozens of seasons irrelevant to the queue.
     */
    private suspend fun loadQueueReferencedShowsAndEpisodes(
        series: List<SonarrSeries>,
        queue: List<SonarrQueueItem>,
    ): Pair<List<FindroidShow>, Map<UUID, List<FindroidEpisode>>> = coroutineScope {
        val tvdbIdBySeriesId: Map<Int, String> =
            series.filter { it.tvdbId != 0 }.associate { it.id to it.tvdbId.toString() }
        val seasonNumbersByTvdbId: Map<String, Set<Int>> =
            queue
                .mapNotNull { item -> tvdbIdBySeriesId[item.seriesId]?.let { it to item.seasonNumber } }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, seasonNumbers) -> seasonNumbers.toSet() }
        if (seasonNumbersByTvdbId.isEmpty()) {
            return@coroutineScope emptyList<FindroidShow>() to emptyMap()
        }

        val shows =
            jellyfinRepository
                .getItems(includeTypes = listOf(BaseItemKind.SERIES), recursive = true)
                .filterIsInstance<FindroidShow>()
                .filter { it.tvdbId in seasonNumbersByTvdbId.keys }

        val episodesByShowId =
            shows
                .map { show ->
                    async {
                        val queuedSeasonNumbers = seasonNumbersByTvdbId[show.tvdbId].orEmpty()
                        val episodes =
                            jellyfinRepository
                                .getSeasons(show.id)
                                .filter { it.indexNumber in queuedSeasonNumbers }
                                .map { season ->
                                    async {
                                        jellyfinRepository.getEpisodes(
                                            seriesId = show.id,
                                            seasonId = season.id,
                                        )
                                    }
                                }
                                .awaitAll()
                                .flatten()
                        show.id to episodes
                    }
                }
                .awaitAll()
                .toMap()
        shows to episodesByShowId
    }

    private suspend fun loadJellyfinMovies(): List<FindroidMovie> =
        jellyfinRepository
            .getItems(includeTypes = listOf(BaseItemKind.MOVIE), recursive = true)
            .filterIsInstance<FindroidMovie>()

    private companion object {
        // Guards against a misconfigured 0 (or negative) pvrPollIntervalMinutes hammering
        // Sonarr/Radarr - the WorkManager backstop has its own, coarser floor (see
        // QueueStatusScheduler), since WorkManager itself enforces a 15-minute minimum.
        const val MIN_POLL_INTERVAL_MINUTES = 1
    }
}
