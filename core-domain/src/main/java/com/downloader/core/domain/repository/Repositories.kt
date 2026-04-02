package com.downloader.core.domain.repository

import com.downloader.core.domain.model.*
import kotlinx.coroutines.flow.Flow

interface DownloadJobRepository {
    suspend fun insertJob(job: DownloadJob): String
    suspend fun updateJob(job: DownloadJob)
    suspend fun deleteJob(jobId: String)
    suspend fun getJob(jobId: String): DownloadJob?
    fun getAllJobs(): Flow<List<DownloadJob>>
    fun getJobsByStatus(status: DownloadStatus): Flow<List<DownloadJob>>
    suspend fun getNextQueuedJob(): DownloadJob?
}

interface ProgressRepository {
    suspend fun updateProgress(progress: DownloadProgress)
    suspend fun getProgress(jobId: String): DownloadProgress?
    fun observeProgress(jobId: String): Flow<DownloadProgress?>
    suspend fun clearProgress(jobId: String)
}

interface SettingsRepository {
    suspend fun getSettings(): AppSettings
    suspend fun updateSettings(settings: AppSettings)
    fun observeSettings(): Flow<AppSettings>
}

interface MediaInfoRepository {
    suspend fun fetchMediaInfo(url: String): Result<MediaInfo>
    suspend fun getCachedMediaInfo(url: String): MediaInfo?
    suspend fun cacheMediaInfo(info: MediaInfo)
}