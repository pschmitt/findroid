package dev.jdtech.jellyfin.repository

import dev.jdtech.jellyfin.api.pvr.JellyseerrApi
import dev.jdtech.jellyfin.api.pvr.SeerrSearchResult
import dev.jdtech.jellyfin.models.SeerrMediaStatus
import dev.jdtech.jellyfin.models.SeerrMediaType
import dev.jdtech.jellyfin.models.SeerrRequestItem
import dev.jdtech.jellyfin.models.SeerrSearchItem
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

/**
 * Same lambda-injection pattern as the other PVR repositories ([jellyseerrApiKeyProvider]
 * resolves the secret from `SecureCredentialStore` in `core`). Constructed via
 * `dev.jdtech.jellyfin.di.JellyseerrModule` (a Hilt `@Provides`) rather than an `@Inject`
 * constructor, since `data` has no Hilt plugin.
 */
class JellyseerrRepositoryImpl(
    private val appPreferences: AppPreferences,
    private val jellyseerrApiKeyProvider: () -> String?,
) : JellyseerrRepository {

    override suspend fun search(query: String): Result<List<SeerrSearchItem>> = runAction { api ->
        api.search(query)
            .results
            .mapNotNull { it.toSearchItem() }
    }

    override suspend fun request(item: SeerrSearchItem): Result<Unit> = runAction { api ->
        api.createRequest(
            mediaType =
                when (item.mediaType) {
                    SeerrMediaType.MOVIE -> JellyseerrApi.MEDIA_TYPE_MOVIE
                    SeerrMediaType.TV -> JellyseerrApi.MEDIA_TYPE_TV
                },
            tmdbId = item.tmdbId,
        )
    }

    override suspend fun getRecentRequests(limit: Int): Result<List<SeerrRequestItem>> =
        runAction { api ->
            coroutineScope {
                // The request resource only carries provider ids - titles/posters need one detail
                // lookup each. The list is small (a page of recent requests), so the parallel
                // fan-out stays cheap.
                api.getRequests(take = limit)
                    .results
                    .map { request ->
                        async {
                            try {
                                when (request.media.mediaType) {
                                    JellyseerrApi.MEDIA_TYPE_MOVIE -> {
                                        val details = api.getMovieDetails(request.media.tmdbId)
                                        SeerrRequestItem(
                                            id = request.id,
                                            tmdbId = request.media.tmdbId,
                                            mediaType = SeerrMediaType.MOVIE,
                                            title = details.title.ifBlank { "TMDB ${request.media.tmdbId}" },
                                            posterUrl = details.posterPath?.toPosterUrl(),
                                            mediaStatus = SeerrMediaStatus.fromCode(request.media.status),
                                        )
                                    }
                                    JellyseerrApi.MEDIA_TYPE_TV -> {
                                        val details = api.getTvDetails(request.media.tmdbId)
                                        SeerrRequestItem(
                                            id = request.id,
                                            tmdbId = request.media.tmdbId,
                                            mediaType = SeerrMediaType.TV,
                                            title = details.name.ifBlank { "TMDB ${request.media.tmdbId}" },
                                            posterUrl = details.posterPath?.toPosterUrl(),
                                            mediaStatus = SeerrMediaStatus.fromCode(request.media.status),
                                        )
                                    }
                                    else -> null
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                // A single failed detail lookup shouldn't take down the whole list.
                                Timber.w(e, "Failed to resolve request ${request.id} details")
                                null
                            }
                        }
                    }
                    .awaitAll()
                    .filterNotNull()
            }
        }

    private fun SeerrSearchResult.toSearchItem(): SeerrSearchItem? {
        val type =
            when (mediaType) {
                JellyseerrApi.MEDIA_TYPE_MOVIE -> SeerrMediaType.MOVIE
                JellyseerrApi.MEDIA_TYPE_TV -> SeerrMediaType.TV
                // Person results (actors/directors) aren't requestable - drop them.
                else -> return null
            }
        return SeerrSearchItem(
            tmdbId = id,
            mediaType = type,
            title = (title ?: name).orEmpty().ifBlank { "TMDB $id" },
            year = (releaseDate ?: firstAirDate)?.take(4)?.toIntOrNull(),
            overview = overview?.takeIf { it.isNotBlank() },
            posterUrl = posterPath?.toPosterUrl(),
            status = SeerrMediaStatus.fromCode(mediaInfo?.status),
        )
    }

    private fun String.toPosterUrl(): String = "$TMDB_IMAGE_BASE$this"

    private suspend fun <T> runAction(block: suspend (JellyseerrApi) -> T): Result<T> {
        val api =
            api() ?: return Result.failure(IllegalStateException("Jellyseerr is not configured"))
        return try {
            Result.success(block(api))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Jellyseerr action failed")
            Result.failure(mapPvrSearchError("Jellyseerr", e))
        }
    }

    private fun api(): JellyseerrApi? {
        if (!appPreferences.getValue(appPreferences.jellyseerrEnabled)) return null
        val baseUrl = appPreferences.getValue(appPreferences.jellyseerrBaseUrl)
        val apiKey = jellyseerrApiKeyProvider()
        if (baseUrl.isNullOrBlank() || apiKey.isNullOrBlank()) return null
        return JellyseerrApi(baseUrl, apiKey)
    }

    private companion object {
        // Jellyseerr's own web UI loads posters straight from TMDB; w342 is plenty for list rows.
        const val TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/w342"
    }
}
