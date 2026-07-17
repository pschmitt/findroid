package dev.jdtech.jellyfin.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jdtech.jellyfin.api.pvr.PvrCredentialKeys
import dev.jdtech.jellyfin.repository.SeerrRepository
import dev.jdtech.jellyfin.repository.SeerrRepositoryImpl
import dev.jdtech.jellyfin.security.SecureCredentialStore
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SeerrModule {
    @Singleton
    @Provides
    fun provideSeerrRepository(
        appPreferences: AppPreferences,
        secureCredentialStore: SecureCredentialStore,
    ): SeerrRepository {
        return SeerrRepositoryImpl(
            appPreferences = appPreferences,
            seerrApiKeyProvider = {
                secureCredentialStore.getString(PvrCredentialKeys.SEERR_API_KEY)
            },
        )
    }
}
