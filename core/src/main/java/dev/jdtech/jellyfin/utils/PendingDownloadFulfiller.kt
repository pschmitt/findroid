package dev.jdtech.jellyfin.utils

import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.PendingDownloadRequestDto
import dev.jdtech.jellyfin.repository.JellyfinRepository
import org.jellyfin.sdk.model.api.ItemFields
import timber.log.Timber

/**
 * Resolves one [PendingDownloadRequestDto] against the current Jellyfin library and, if the
 * requested season/episode has appeared, enqueues its download. Mirrors
 * [AutoDownloadRuleEvaluator]'s shape/dedup logic, but a pending request is a one-off "download
 * whichever episodes exist the moment this season/episode shows up" rather than a persistent
 * "keep following new episodes" rule (see [dev.jdtech.jellyfin.repository.AutoDownloadRuleRepository]
 * for that) - so once resolved, the caller ([dev.jdtech.jellyfin.work.PendingDownloadWorker])
 * deletes the row regardless of whether anything actually needed downloading.
 */
class PendingDownloadFulfiller {
    /**
     * Returns true when [request]'s target season/episode was found in the library (whether or
     * not a new download was actually needed - it may already be downloaded/queued by other
     * means) and the row should be deleted; false when it still isn't there yet and the request
     * should be left in place for the next cycle. [onFulfilled] is invoked with a display title
     * only when a new download was actually enqueued, so the caller can post a notification.
     */
    suspend fun fulfill(
        request: PendingDownloadRequestDto,
        database: ServerDatabaseDao,
        repository: JellyfinRepository,
        downloader: Downloader,
        onFulfilled: suspend (title: String) -> Unit,
    ): Boolean {
        val season =
            try {
                repository.getSeasons(request.seriesId).firstOrNull {
                    it.indexNumber == request.seasonNumber
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to check pending download request ${request.id}")
                return false
            } ?: return false

        val episodes =
            try {
                repository.getEpisodes(
                    seriesId = request.seriesId,
                    seasonId = season.id,
                    fields = listOf(ItemFields.MEDIA_SOURCES),
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch episodes for pending download request ${request.id}")
                return false
            }

        val requestedEpisodeNumber = request.episodeNumber
        val targetEpisodes =
            if (requestedEpisodeNumber != null) {
                val episode = episodes.firstOrNull { it.indexNumber == requestedEpisodeNumber } ?: return false
                listOf(episode)
            } else {
                // Whole-season request: only fulfilled once the season actually has episodes to
                // download - a bare season shell with none yet means "not really there", so leave
                // it pending rather than deleting the request for nothing.
                if (episodes.isEmpty()) return false
                episodes
            }

        var queuedAny = false
        val storageIndex = downloader.resolvePreferredStorageIndex()
        for (episode in targetEpisodes) {
            try {
                // A sources row already exists the moment a download is enqueued (before it
                // finishes), so its mere presence covers already downloaded/queued/running alike.
                if (database.getSources(episode.id).isNotEmpty()) continue
                val sourceId = episode.sources.firstOrNull()?.id ?: continue
                downloader.downloadItem(episode, sourceId, storageIndex = storageIndex)
                queuedAny = true
            } catch (e: Exception) {
                Timber.e(e, "Failed to queue pending download for episode ${episode.id}")
            }
        }

        if (queuedAny) {
            val title = if (requestedEpisodeNumber != null) targetEpisodes.first().name else season.name
            onFulfilled(title)
        }
        return true
    }
}
