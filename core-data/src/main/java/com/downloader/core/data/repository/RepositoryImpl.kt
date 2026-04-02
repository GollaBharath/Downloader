package com.downloader.core.data.repository

import com.downloader.core.data.database.*
import com.downloader.core.data.mapper.*
import com.downloader.core.domain.model.*
import com.downloader.core.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadJobRepositoryImpl @Inject constructor(
    private val dao: DownloadJobDao
) : DownloadJobRepository {

    override suspend fun insertJob(job: DownloadJob): String {
        dao.insertJob(DownloadJobMapper.toEntity(job))
        return job.id
    }

    override suspend fun updateJob(job: DownloadJob) {
        dao.updateJob(DownloadJobMapper.toEntity(job))
    }

    override suspend fun deleteJob(jobId: String) {
        dao.deleteJob(jobId)
    }

    override suspend fun getJob(jobId: String): DownloadJob? {
        return dao.getJob(jobId)?.let { DownloadJobMapper.toDomain(it) }
    }

    override fun getAllJobs(): Flow<List<DownloadJob>> {
        return dao.getAllJobs().map { entities ->
            DownloadJobMapper.toDomainList(entities)
        }
    }

    override fun getJobsByStatus(status: DownloadStatus): Flow<List<DownloadJob>> {
        return dao.getJobsByStatus(status.name).map { entities ->
            DownloadJobMapper.toDomainList(entities)
        }
    }

    override suspend fun getNextQueuedJob(): DownloadJob? {
        return dao.getNextQueuedJob()?.let { DownloadJobMapper.toDomain(it) }
    }
}

@Singleton
class ProgressRepositoryImpl @Inject constructor(
    private val dao: DownloadProgressDao
) : ProgressRepository {

    override suspend fun updateProgress(progress: DownloadProgress) {
        dao.insertOrUpdateProgress(DownloadProgressMapper.toEntity(progress))
    }

    override suspend fun getProgress(jobId: String): DownloadProgress? {
        return dao.getProgress(jobId)?.let { DownloadProgressMapper.toDomain(it) }
    }

    override fun observeProgress(jobId: String): Flow<DownloadProgress?> {
        return dao.observeProgress(jobId).map { entity ->
            entity?.let { DownloadProgressMapper.toDomain(it) }
        }
    }

    override suspend fun clearProgress(jobId: String) {
        dao.deleteProgress(jobId)
    }
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dao: AppSettingsDao
) : SettingsRepository {

    override suspend fun getSettings(): AppSettings {
        return dao.getSettings()?.let { AppSettingsMapper.toDomain(it) }
            ?: AppSettingsMapper.getDefault()
    }

    override suspend fun updateSettings(settings: AppSettings) {
        dao.insertOrUpdateSettings(AppSettingsMapper.toEntity(settings))
    }

    override fun observeSettings(): Flow<AppSettings> {
        return dao.observeSettings().map { entity ->
            entity?.let { AppSettingsMapper.toDomain(it) }
                ?: AppSettingsMapper.getDefault()
        }
    }
}

@Singleton
class MediaInfoRepositoryImpl @Inject constructor(
    private val dao: MediaInfoCacheDao,
    private val ytDlpService: YtDlpService
) : MediaInfoRepository {

    override suspend fun fetchMediaInfo(url: String): Result<MediaInfo> {
        return try {
            Result.success(ytDlpService.fetchMediaInfo(url))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCachedMediaInfo(url: String): MediaInfo? {
        return dao.getCachedInfo(url)?.let { MediaInfoMapper.toDomain(it) }
    }

    override suspend fun cacheMediaInfo(info: MediaInfo) {
        dao.insertOrUpdateCacheInfo(MediaInfoMapper.toEntity(info))
    }
}

interface YtDlpService {
    suspend fun fetchMediaInfo(url: String): MediaInfo
}