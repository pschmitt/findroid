package dev.jdtech.jellyfin.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jdtech.jellyfin.backup.BackupManager
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.security.SecureCredentialStore
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BackupModule {
    @Singleton
    @Provides
    fun provideBackupManager(
        application: Application,
        serverDatabase: ServerDatabaseDao,
        appPreferences: AppPreferences,
        secureCredentialStore: SecureCredentialStore,
    ): BackupManager {
        return BackupManager(
            context = application,
            database = serverDatabase,
            appPreferences = appPreferences,
            getSecret = secureCredentialStore::getString,
            // Blocking, not the fire-and-forget default - see SecureCredentialStore.putStringBlocking.
            putSecret = secureCredentialStore::putStringBlocking,
        )
    }
}
