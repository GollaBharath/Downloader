package com.downloader.core.data.database

import androidx.room.*
import com.downloader.core.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadJobDao {
    @Query("SELECT * FROM download_jobs ORDER BY created_at DESC")
    fun getAllJobs(): Flow<List<DownloadJobEntity>>

    @Query("SELECT * FROM download_jobs WHERE status = :status ORDER BY created_at ASC")
    fun getJobsByStatus(status: String): Flow<List<DownloadJobEntity>>

    @Query("SELECT * FROM download_jobs WHERE id = :jobId")
    suspend fun getJob(jobId: String): DownloadJobEntity?

    @Query("SELECT * FROM download_jobs WHERE status = 'QUEUED' ORDER BY created_at ASC LIMIT 1")
    suspend fun getNextQueuedJob(): DownloadJobEntity?

    @Insert
    suspend fun insertJob(job: DownloadJobEntity): Long

    @Update
    suspend fun updateJob(job: DownloadJobEntity)

    @Query("DELETE FROM download_jobs WHERE id = :jobId")
    suspend fun deleteJob(jobId: String)

    @Query("DELETE FROM download_jobs WHERE status IN ('SUCCEEDED', 'CANCELLED') AND completed_at < :beforeTimestamp")
    suspend fun cleanupOldCompletedJobs(beforeTimestamp: Long)
}

@Dao
interface DownloadProgressDao {
    @Query("SELECT * FROM download_progress WHERE job_id = :jobId")
    suspend fun getProgress(jobId: String): DownloadProgressEntity?

    @Query("SELECT * FROM download_progress WHERE job_id = :jobId")
    fun observeProgress(jobId: String): Flow<DownloadProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProgress(progress: DownloadProgressEntity)

    @Query("DELETE FROM download_progress WHERE job_id = :jobId")
    suspend fun deleteProgress(jobId: String)

    @Query("DELETE FROM download_progress WHERE job_id IN (SELECT id FROM download_jobs WHERE status IN ('SUCCEEDED', 'FAILED', 'CANCELLED'))")
    suspend fun cleanupCompletedJobProgress()
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettings(): AppSettingsEntity?

    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observeSettings(): Flow<AppSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: AppSettingsEntity)
}

@Dao
interface MediaInfoCacheDao {
    @Query("SELECT * FROM media_info_cache WHERE url = :url")
    suspend fun getCachedInfo(url: String): MediaInfoCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCacheInfo(info: MediaInfoCacheEntity)

    @Query("DELETE FROM media_info_cache WHERE cached_at < :beforeTimestamp")
    suspend fun cleanupOldCache(beforeTimestamp: Long)

    @Query("DELETE FROM media_info_cache WHERE url = :url")
    suspend fun deleteCachedInfo(url: String)
}