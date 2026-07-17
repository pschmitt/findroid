package dev.jdtech.jellyfin.work

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.jdtech.jellyfin.backup.BackupManager
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Writes a backup to the user-chosen SAF folder, encrypted with the configured auto-backup
 * password if one is set (see AppPreferences.autoBackupPassword), same as an unencrypted export
 * when left blank.
 */
@HiltWorker
class AutoBackupWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backupManager: BackupManager,
    private val appPreferences: AppPreferences,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) {
            val folderUriString =
                appPreferences.getValue(appPreferences.autoBackupFolderUri)
                    ?: return@withContext Result.failure()
            val folder =
                DocumentFile.fromTreeUri(applicationContext, Uri.parse(folderUriString))
                    ?: return@withContext Result.failure()

            try {
                // Human-friendly local timestamp with UTC offset, e.g.
                // "findroid-backup-2026-07-17T08:58:03+02:00.frb". SAF/Drive accept ':' in
                // display names, so the offset can stay in its readable form.
                val timestamp =
                    ZonedDateTime.now()
                        .truncatedTo(ChronoUnit.SECONDS)
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                val fileName = "findroid-backup-$timestamp.frb"
                val file =
                    folder.createFile("application/octet-stream", fileName)
                        ?: return@withContext Result.failure()

                val password = appPreferences.getValue(appPreferences.autoBackupPassword)
                val envelope = backupManager.buildBackup()
                backupManager.writeBackup(envelope, file.uri, password = password)
                appPreferences.setValue(appPreferences.lastBackupTimestamp, System.currentTimeMillis())
                Result.success()
            } catch (e: Exception) {
                Timber.e(e, "Auto-backup failed")
                Result.retry()
            }
        }
}
