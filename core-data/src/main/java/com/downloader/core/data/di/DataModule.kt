package com.downloader.core.data.di

import android.content.Context
import androidx.room.Room
import com.downloader.core.data.database.*
import com.downloader.core.data.repository.*
import com.downloader.core.domain.repository.*
import dagger.Module
import dagger.Provides
import dagger.Binds
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDownloaderDatabase(@ApplicationContext context: Context): DownloaderDatabase {
        return DownloaderDatabase.getDatabase(context)
    }

    @Provides
    fun provideDownloadJobDao(database: DownloaderDatabase): DownloadJobDao {
        return database.downloadJobDao()
    }

    @Provides
    fun provideDownloadProgressDao(database: DownloaderDatabase): DownloadProgressDao {
        return database.downloadProgressDao()
    }

    @Provides
    fun provideAppSettingsDao(database: DownloaderDatabase): AppSettingsDao {
        return database.appSettingsDao()
    }

    @Provides
    fun provideMediaInfoCacheDao(database: DownloaderDatabase): MediaInfoCacheDao {
        return database.mediaInfoCacheDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindDownloadJobRepository(
        impl: DownloadJobRepositoryImpl
    ): DownloadJobRepository

    @Binds
    abstract fun bindProgressRepository(
        impl: ProgressRepositoryImpl
    ): ProgressRepository

    @Binds
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    abstract fun bindMediaInfoRepository(
        impl: MediaInfoRepositoryImpl
    ): MediaInfoRepository

    @Binds
    abstract fun bindYtDlpService(
        impl: YtDlpServiceImpl
    ): YtDlpService
}