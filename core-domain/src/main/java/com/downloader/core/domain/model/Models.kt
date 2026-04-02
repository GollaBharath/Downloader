package com.downloader.core.domain.model

import kotlinx.serialization.Serializable

enum class DownloadStatus {
    QUEUED,
    RUNNING, 
    SUCCEEDED,
    FAILED,
    CANCELLED
}

enum class DownloadPhase {
    QUEUED,
    FETCHING_INFO,
    DOWNLOADING,
    POST_PROCESSING,
    MERGING,
    COMPLETED,
    FAILED
}

data class DownloadJob(
    val id: String,
    val url: String,
    val title: String?,
    val outputPath: String,
    val format: String?,
    val audioOnly: Boolean = false,
    val includeSubtitles: Boolean = false,
    val includeThumbnail: Boolean = false,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val errorMessage: String? = null
)

data class DownloadProgress(
    val jobId: String,
    val phase: DownloadPhase,
    val percentComplete: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long? = null,
    val speed: String? = null, // e.g., "1.2MiB/s"
    val eta: String? = null,   // e.g., "00:02:30"
    val currentFile: String? = null,
    val errorMessage: String? = null
)

data class MediaInfo(
    val url: String,
    val title: String?,
    val description: String?,
    val duration: String?, // e.g., "00:10:30"
    val thumbnail: String?,
    val uploader: String?,
    val uploadDate: String?,
    val viewCount: Long?,
    val formats: List<FormatInfo> = emptyList(),
    val isPlaylist: Boolean = false,
    val playlistEntries: List<MediaInfo>? = null
)

@Serializable
data class FormatInfo(
    val formatId: String,
    val ext: String, // mp4, mkv, mp3, etc.
    val resolution: String? = null, // 1920x1080, audio only, etc.
    val codec: String? = null,
    val filesize: Long? = null,
    val fps: Int? = null,
    val audioBitrate: Int? = null,
    val videoBitrate: Int? = null,
    val isAudioOnly: Boolean = false,
    val isVideoOnly: Boolean = false
)

data class AppSettings(
    val downloadFolderUri: String?,
    val defaultAudioOnly: Boolean = false,
    val defaultIncludeSubtitles: Boolean = false,
    val defaultIncludeThumbnail: Boolean = false,
    val defaultVideoFormat: String = "best",
    val defaultAudioFormat: String = "best",
    val maxConcurrentDownloads: Int = 1,
    val filenameTemplate: String = "%(title)s.%(ext)s"
)