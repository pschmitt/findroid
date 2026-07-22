package dev.jdtech.jellyfin.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.repository.PendingDownloadRequestRepository
import dev.jdtech.jellyfin.repository.PendingDownloadRequestRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PendingDownloadRequestModule {
    @Singleton
    @Provides
    fun providePendingDownloadRequestRepository(
        serverDatabase: ServerDatabaseDao
    ): PendingDownloadRequestRepository {
        return PendingDownloadRequestRepositoryImpl(serverDatabase)
    }
}
