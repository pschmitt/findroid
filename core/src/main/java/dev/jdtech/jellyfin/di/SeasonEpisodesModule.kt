package dev.jdtech.jellyfin.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jdtech.jellyfin.api.pvr.PvrCredentialKeys
import dev.jdtech.jellyfin.repository.SeasonEpisodesRepository
import dev.jdtech.jellyfin.repository.SeasonEpisodesRepositoryImpl
import dev.jdtech.jellyfin.security.SecureCredentialStore
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SeasonEpisodesModule {
    @Singleton
    @Provides
    fun provideSeasonEpisodesRepository(
        appPreferences: AppPreferences,
        secureCredentialStore: SecureCredentialStore,
    ): SeasonEpisodesRepository {
        return SeasonEpisodesRepositoryImpl(
            appPreferences = appPreferences,
            sonarrApiKeyProvider = { secureCredentialStore.getString(PvrCredentialKeys.SONARR_API_KEY) },
        )
    }
}
