package com.downloader.core.domain.usecase

import com.downloader.core.domain.model.*
import com.downloader.core.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class EnqueueDownloadUseCase(
    private val jobRepository: DownloadJobRepository,
    private val mediaInfoRepository: MediaInfoRepository
) {
    suspend operator fun invoke(
        url: String,
        audioOnly: Boolean = false,
        includeSubtitles: Boolean = false,
        includeThumbnail: Boolean = false,
        formatId: String? = null,
        outputPath: String
    ): Result<String> {
        return try {
            // Create job with basic info first
            val job = DownloadJob(
                id = generateJobId(),
                url = url,
                title = null, // Will be filled after fetching info
                outputPath = outputPath,
                format = formatId,
                audioOnly = audioOnly,
                includeSubtitles = includeSubtitles,
                includeThumbnail = includeThumbnail,
                status = DownloadStatus.QUEUED
            )
            
            val jobId = jobRepository.insertJob(job)
            Result.success(jobId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun generateJobId(): String = java.util.UUID.randomUUID().toString()
}

class GetDownloadJobsUseCase(
    private val jobRepository: DownloadJobRepository
) {
    operator fun invoke(): Flow<List<DownloadJob>> = jobRepository.getAllJobs()
    
    fun getByStatus(status: DownloadStatus): Flow<List<DownloadJob>> = 
        jobRepository.getJobsByStatus(status)
}

class CancelDownloadUseCase(
    private val jobRepository: DownloadJobRepository
) {
    suspend operator fun invoke(jobId: String): Result<Unit> {
        return try {
            val job = jobRepository.getJob(jobId)
            if (job != null) {
                jobRepository.updateJob(job.copy(status = DownloadStatus.CANCELLED))
                Result.success(Unit)
            } else {
                Result.failure(Exception("Job not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class RetryDownloadUseCase(
    private val jobRepository: DownloadJobRepository
) {
    suspend operator fun invoke(jobId: String): Result<Unit> {
        return try {
            val job = jobRepository.getJob(jobId)
            if (job != null && job.status == DownloadStatus.FAILED) {
                jobRepository.updateJob(
                    job.copy(
                        status = DownloadStatus.QUEUED,
                        errorMessage = null,
                        completedAt = null
                    )
                )
                Result.success(Unit)
            } else {
                Result.failure(Exception("Job not found or not retryable"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GetNextQueuedJobUseCase(
    private val jobRepository: DownloadJobRepository
) {
    suspend operator fun invoke(): DownloadJob? {
        return jobRepository.getNextQueuedJob()
    }
}

class ObserveDownloadProgressUseCase(
    private val progressRepository: ProgressRepository
) {
    operator fun invoke(jobId: String): Flow<DownloadProgress?> {
        return progressRepository.observeProgress(jobId)
    }
}

class UpdateDownloadProgressUseCase(
    private val progressRepository: ProgressRepository
) {
    suspend operator fun invoke(progress: DownloadProgress) {
        progressRepository.updateProgress(progress)
    }
}

class FetchMediaInfoUseCase(
    private val mediaInfoRepository: MediaInfoRepository,
    private val jobRepository: DownloadJobRepository
) {
    suspend operator fun invoke(url: String, jobId: String? = null): Result<MediaInfo> {
        return try {
            // Check cache first
            val cachedInfo = mediaInfoRepository.getCachedMediaInfo(url)
            if (cachedInfo != null) {
                return Result.success(cachedInfo)
            }
            
            // Fetch from network
            val result = mediaInfoRepository.fetchMediaInfo(url)
            if (result.isSuccess) {
                val info = result.getOrThrow()
                mediaInfoRepository.cacheMediaInfo(info)
                
                // Update job title if jobId provided
                if (jobId != null) {
                    val job = jobRepository.getJob(jobId)
                    if (job != null) {
                        jobRepository.updateJob(job.copy(title = info.title))
                    }
                }
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GetAppSettingsUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): AppSettings = settingsRepository.getSettings()
    
    fun observe(): Flow<AppSettings> = settingsRepository.observeSettings()
}

class UpdateAppSettingsUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(settings: AppSettings) {
        settingsRepository.updateSettings(settings)
    }
}