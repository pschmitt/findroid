package dev.jdtech.jellyfin.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.models.AutomaticSearchOutcome
import dev.jdtech.jellyfin.repository.SonarrSearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Waits for a Sonarr automatic search (triggered from the Season/Episode/Calendar screens via
 * [SonarrSearchRepository.searchEpisode]) to actually finish, then posts a notification - the
 * search can easily outlive the ViewModel that triggered it (the user has usually moved on long
 * before Sonarr/Prowlarr answers), so this can't just be a coroutine in that ViewModel's scope.
 */
@HiltWorker
class AutomaticSearchWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sonarrSearchRepository: SonarrSearchRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) {
            val episodeId = inputData.getInt(KEY_EPISODE_ID, -1)
            val commandId = inputData.getInt(KEY_COMMAND_ID, -1)
            if (episodeId == -1 || commandId == -1) return@withContext Result.failure()

            sonarrSearchRepository
                .awaitAutomaticSearchResult(episodeId, commandId)
                .onSuccess { notify(it, commandId) }
                .onFailure { Timber.w(it, "Failed to check automatic search result") }

            Result.success()
        }

    private fun notify(outcome: AutomaticSearchOutcome, commandId: Int) {
        createNotificationChannel()

        val episodeCode = "S%02dE%02d".format(outcome.seasonNumber, outcome.episodeNumber)
        val title = outcome.seriesTitle?.let { "$it $episodeCode" } ?: episodeCode
        val text =
            applicationContext.getString(
                if (outcome.succeeded) {
                    CoreR.string.automatic_search_notification_found
                } else {
                    CoreR.string.automatic_search_notification_not_found
                }
            )

        val notification =
            NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(CoreR.drawable.ic_search)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(openAppPendingIntent())
                .build()

        // commandId is unique per triggered search, so concurrent searches get their own
        // notification instead of overwriting each other.
        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID_BASE + commandId, notification)
    }

    private fun openAppPendingIntent(): PendingIntent? {
        val intent =
            applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
                ?: return null
        return PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(CoreR.string.automatic_search_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        applicationContext
            .getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    companion object {
        const val KEY_EPISODE_ID = "KEY_EPISODE_ID"
        const val KEY_COMMAND_ID = "KEY_COMMAND_ID"

        private const val CHANNEL_ID = "automatic_search"
        private const val NOTIFICATION_ID_BASE = 279_413_000
    }
}
