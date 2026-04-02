package com.downloader.engine.di

import android.content.Context
import com.downloader.core.domain.repository.SettingsRepository
import com.downloader.engine.binary.BinaryManager
import com.downloader.engine.binary.DownloadStorageResolver
import com.downloader.engine.command.FFmpegCommandBuilder
import com.downloader.engine.command.YtDlpCommandBuilder
import com.downloader.engine.executor.DownloadExecutor
import com.downloader.engine.parser.YtDlpProgressParser
import com.downloader.engine.process.ProcessRunner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EngineModule {

    @Provides
    @Singleton
    fun provideBinaryManager(
        @ApplicationContext context: Context
    ): BinaryManager = BinaryManager(context)

    @Provides
    @Singleton
    fun provideDownloadStorageResolver(
        @ApplicationContext context: Context
    ): DownloadStorageResolver = DownloadStorageResolver(context)

    @Provides
    @Singleton
    fun provideProcessRunner(): ProcessRunner = ProcessRunner()

    @Provides
    @Singleton
    fun provideYtDlpCommandBuilder(): YtDlpCommandBuilder = YtDlpCommandBuilder()

    @Provides
    @Singleton
    fun provideFFmpegCommandBuilder(): FFmpegCommandBuilder = FFmpegCommandBuilder()

    @Provides
    @Singleton
    fun provideYtDlpProgressParser(): YtDlpProgressParser = YtDlpProgressParser()
    
    @Provides
    @Singleton
    fun provideDownloadExecutor(
        settingsRepository: SettingsRepository,
        storageResolver: DownloadStorageResolver
    ): DownloadExecutor = DownloadExecutor(
        settingsRepository,
        storageResolver
    )
}