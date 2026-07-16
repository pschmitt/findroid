package dev.jdtech.jellyfin.di

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jdtech.jellyfin.api.pvr.PvrCredentialKeys
import dev.jdtech.jellyfin.repository.SonarrSearchRepository
import dev.jdtech.jellyfin.repository.SonarrSearchRepositoryImpl
import dev.jdtech.jellyfin.security.SecureCredentialStore
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.work.AutomaticSearchWorker
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SonarrSearchModule {
    @Singleton
    @Provides
    fun provideSonarrSearchRepository(
        appPreferences: AppPreferences,
        secureCredentialStore: SecureCredentialStore,
        workManager: WorkManager,
    ): SonarrSearchRepository {
        return SonarrSearchRepositoryImpl(
            appPreferences = appPreferences,
            sonarrApiKeyProvider = { secureCredentialStore.getString(PvrCredentialKeys.SONARR_API_KEY) },
            scheduleCompletionCheck = { episodeId, commandId ->
                val request =
                    OneTimeWorkRequestBuilder<AutomaticSearchWorker>()
                        .setInputData(
                            workDataOf(
                                AutomaticSearchWorker.KEY_EPISODE_ID to episodeId,
                                AutomaticSearchWorker.KEY_COMMAND_ID to commandId,
                            )
                        )
                        .build()
                workManager.enqueue(request)
            },
        )
    }
}
