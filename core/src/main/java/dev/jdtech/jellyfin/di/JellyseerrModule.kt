package dev.jdtech.jellyfin.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jdtech.jellyfin.api.pvr.PvrCredentialKeys
import dev.jdtech.jellyfin.repository.JellyseerrRepository
import dev.jdtech.jellyfin.repository.JellyseerrRepositoryImpl
import dev.jdtech.jellyfin.security.SecureCredentialStore
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object JellyseerrModule {
    @Singleton
    @Provides
    fun provideJellyseerrRepository(
        appPreferences: AppPreferences,
        secureCredentialStore: SecureCredentialStore,
    ): JellyseerrRepository {
        return JellyseerrRepositoryImpl(
            appPreferences = appPreferences,
            jellyseerrApiKeyProvider = {
                secureCredentialStore.getString(PvrCredentialKeys.JELLYSEERR_API_KEY)
            },
        )
    }
}
