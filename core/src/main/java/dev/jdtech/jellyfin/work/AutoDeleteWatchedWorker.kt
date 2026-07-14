package dev.jdtech.jellyfin.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.FindroidSourceType
import dev.jdtech.jellyfin.models.toFindroidSource
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.utils.Downloader
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Deletes downloaded episodes that have been watched for longer than the configured threshold.
 * Scoped to the currently active server/user only, same rationale as [AutoDownloadWorker].
 */
@HiltWorker
class AutoDeleteWatchedWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val database: ServerDatabaseDao,
    private val jellyfinRepository: JellyfinRepository,
    private val downloader: Downloader,
    private val appPreferences: AppPreferences,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) {
            if (!appPreferences.getValue(appPreferences.autoDeleteWatched)) {
                return@withContext Result.success()
            }
            val serverId =
                appPreferences.getValue(appPreferences.currentServer)
                    ?: return@withContext Result.success()
            val hours = appPreferences.getValue(appPreferences.autoDeleteWatchedHours)
            val cutoff = LocalDateTime.now().minusHours(hours.toLong())

            val downloadedEpisodes =
                database.getEpisodesByServerId(serverId).filter { episode ->
                    database.getSources(episode.id).any { it.type == FindroidSourceType.LOCAL }
                }

            for (episodeDto in downloadedEpisodes) {
                try {
                    val episode = jellyfinRepository.getEpisode(episodeDto.id)
                    val lastPlayedDate = episode.lastPlayedDate
                    if (episode.played && lastPlayedDate != null && lastPlayedDate.isBefore(cutoff)) {
                        val source =
                            database
                                .getSources(episode.id)
                                .firstOrNull { it.type == FindroidSourceType.LOCAL }
                                ?: continue
                        downloader.deleteItem(episode, source.toFindroidSource(database))
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to evaluate auto-delete for episode ${episodeDto.id}")
                }
            }

            Result.success()
        }
}
