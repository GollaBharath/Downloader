package com.downloader.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "download_jobs")
data class DownloadJobEntity(
    @PrimaryKey
    val id: String,
    val url: String,
    val title: String?,
    @ColumnInfo(name = "output_path")
    val outputPath: String,
    val format: String?,
    @ColumnInfo(name = "audio_only")
    val audioOnly: Boolean = false,
    @ColumnInfo(name = "include_subtitles")
    val includeSubtitles: Boolean = false,
    @ColumnInfo(name = "include_thumbnail")
    val includeThumbnail: Boolean = false,
    val status: String = "QUEUED", // Store enum as string
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,
    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null
)

@Entity(tableName = "download_progress")
data class DownloadProgressEntity(
    @PrimaryKey
    @ColumnInfo(name = "job_id")
    val jobId: String,
    val phase: String, // Store enum as string
    @ColumnInfo(name = "percent_complete")
    val percentComplete: Float = 0f,
    @ColumnInfo(name = "downloaded_bytes")
    val downloadedBytes: Long = 0L,
    @ColumnInfo(name = "total_bytes")
    val totalBytes: Long? = null,
    val speed: String? = null,
    val eta: String? = null,
    @ColumnInfo(name = "current_file")
    val currentFile: String? = null,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1, // Single row table
    @ColumnInfo(name = "download_folder_uri")
    val downloadFolderUri: String?,
    @ColumnInfo(name = "default_audio_only")
    val defaultAudioOnly: Boolean = false,
    @ColumnInfo(name = "default_include_subtitles")
    val defaultIncludeSubtitles: Boolean = false,
    @ColumnInfo(name = "default_include_thumbnail")
    val defaultIncludeThumbnail: Boolean = false,
    @ColumnInfo(name = "default_video_format")
    val defaultVideoFormat: String = "best",
    @ColumnInfo(name = "default_audio_format")
    val defaultAudioFormat: String = "best",
    @ColumnInfo(name = "max_concurrent_downloads")
    val maxConcurrentDownloads: Int = 1,
    @ColumnInfo(name = "filename_template")
    val filenameTemplate: String = "%(title)s.%(ext)s"
)

@Entity(tableName = "media_info_cache")
data class MediaInfoCacheEntity(
    @PrimaryKey
    val url: String,
    val title: String?,
    val description: String?,
    val duration: String?,
    val thumbnail: String?,
    val uploader: String?,
    @ColumnInfo(name = "upload_date")
    val uploadDate: String?,
    @ColumnInfo(name = "view_count")
    val viewCount: Long?,
    @ColumnInfo(name = "formats_json")
    val formatsJson: String, // JSON string of FormatInfo list
    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
)