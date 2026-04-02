package com.downloader.core.data.mapper

import com.downloader.core.domain.model.*
import com.downloader.core.data.entity.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

object DownloadJobMapper {
    fun toEntity(domain: DownloadJob): DownloadJobEntity {
        return DownloadJobEntity(
            id = domain.id,
            url = domain.url,
            title = domain.title,
            outputPath = domain.outputPath,
            format = domain.format,
            audioOnly = domain.audioOnly,
            includeSubtitles = domain.includeSubtitles,
            includeThumbnail = domain.includeThumbnail,
            status = domain.status.name,
            createdAt = domain.createdAt,
            completedAt = domain.completedAt,
            errorMessage = domain.errorMessage
        )
    }

    fun toDomain(entity: DownloadJobEntity): DownloadJob {
        return DownloadJob(
            id = entity.id,
            url = entity.url,
            title = entity.title,
            outputPath = entity.outputPath,
            format = entity.format,
            audioOnly = entity.audioOnly,
            includeSubtitles = entity.includeSubtitles,
            includeThumbnail = entity.includeThumbnail,
            status = DownloadStatus.valueOf(entity.status),
            createdAt = entity.createdAt,
            completedAt = entity.completedAt,
            errorMessage = entity.errorMessage
        )
    }

    fun toDomainList(entities: List<DownloadJobEntity>): List<DownloadJob> {
        return entities.map { toDomain(it) }
    }
}

object DownloadProgressMapper {
    fun toEntity(domain: DownloadProgress): DownloadProgressEntity {
        return DownloadProgressEntity(
            jobId = domain.jobId,
            phase = domain.phase.name,
            percentComplete = domain.percentComplete,
            downloadedBytes = domain.downloadedBytes,
            totalBytes = domain.totalBytes,
            speed = domain.speed,
            eta = domain.eta,
            currentFile = domain.currentFile,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun toDomain(entity: DownloadProgressEntity): DownloadProgress {
        return DownloadProgress(
            jobId = entity.jobId,
            phase = DownloadPhase.valueOf(entity.phase),
            percentComplete = entity.percentComplete,
            downloadedBytes = entity.downloadedBytes,
            totalBytes = entity.totalBytes,
            speed = entity.speed,
            eta = entity.eta,
            currentFile = entity.currentFile
        )
    }
}

object AppSettingsMapper {
    fun toEntity(domain: AppSettings): AppSettingsEntity {
        return AppSettingsEntity(
            id = 1,
            downloadFolderUri = domain.downloadFolderUri,
            defaultAudioOnly = domain.defaultAudioOnly,
            defaultIncludeSubtitles = domain.defaultIncludeSubtitles,
            defaultIncludeThumbnail = domain.defaultIncludeThumbnail,
            defaultVideoFormat = domain.defaultVideoFormat,
            defaultAudioFormat = domain.defaultAudioFormat,
            maxConcurrentDownloads = domain.maxConcurrentDownloads,
            filenameTemplate = domain.filenameTemplate
        )
    }

    fun toDomain(entity: AppSettingsEntity): AppSettings {
        return AppSettings(
            downloadFolderUri = entity.downloadFolderUri,
            defaultAudioOnly = entity.defaultAudioOnly,
            defaultIncludeSubtitles = entity.defaultIncludeSubtitles,
            defaultIncludeThumbnail = entity.defaultIncludeThumbnail,
            defaultVideoFormat = entity.defaultVideoFormat,
            defaultAudioFormat = entity.defaultAudioFormat,
            maxConcurrentDownloads = entity.maxConcurrentDownloads,
            filenameTemplate = entity.filenameTemplate
        )
    }

    fun getDefault(): AppSettings {
        return AppSettings(
            downloadFolderUri = null,
            defaultAudioOnly = false,
            defaultIncludeSubtitles = false,
            defaultIncludeThumbnail = false,
            defaultVideoFormat = "best",
            defaultAudioFormat = "best",
            maxConcurrentDownloads = 1,
            filenameTemplate = "%(title)s.%(ext)s"
        )
    }
}

object MediaInfoMapper {
    private val json = Json { ignoreUnknownKeys = true }

    fun toEntity(domain: MediaInfo): MediaInfoCacheEntity {
        return MediaInfoCacheEntity(
            url = domain.url,
            title = domain.title,
            description = domain.description,
            duration = domain.duration,
            thumbnail = domain.thumbnail,
            uploader = domain.uploader,
            uploadDate = domain.uploadDate,
            viewCount = domain.viewCount,
            formatsJson = json.encodeToString(domain.formats),
            cachedAt = System.currentTimeMillis()
        )
    }

    fun toDomain(entity: MediaInfoCacheEntity): MediaInfo {
        val formats = try {
            json.decodeFromString<List<FormatInfo>>(entity.formatsJson)
        } catch (e: Exception) {
            emptyList()
        }

        return MediaInfo(
            url = entity.url,
            title = entity.title,
            description = entity.description,
            duration = entity.duration,
            thumbnail = entity.thumbnail,
            uploader = entity.uploader,
            uploadDate = entity.uploadDate,
            viewCount = entity.viewCount,
            formats = formats
        )
    }
}