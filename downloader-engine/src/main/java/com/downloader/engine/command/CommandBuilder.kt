package com.downloader.engine.command

import com.downloader.core.domain.model.DownloadJob
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtDlpCommandBuilder @Inject constructor() {

    fun buildInfoCommand(url: String, ytDlpCommandPrefix: List<String>): List<String> {
        return ytDlpCommandPrefix + listOf(
            "--dump-json",
            "--no-warnings",
            "--ignore-errors",
            url
        )
    }

    fun buildListFormatsCommand(url: String, ytDlpCommandPrefix: List<String>): List<String> {
        return ytDlpCommandPrefix + listOf(
            "--list-formats",
            "--no-warnings",
            url
        )
    }

    fun buildDownloadCommand(
        job: DownloadJob,
        ytDlpCommandPrefix: List<String>,
        ffmpegPath: String,
        outputDir: String
    ): List<String> {
        val command = mutableListOf<String>().apply {
            addAll(ytDlpCommandPrefix)
            
            // Basic options
            add("--no-warnings")
            add("--ignore-errors")
            add("--no-playlist")  // For MVP, handle single videos only
            
            // FFmpeg path
            add("--ffmpeg-location")
            add(ffmpegPath)
            
            // Output template
            add("--output")
            val filename = sanitizeFilename(job.title ?: "%(title)s")
            add("$outputDir/$filename.%(ext)s")
            
            // Format selection
            job.format?.let { format ->
                add("--format")
                add(format)
            } ?: run {
                if (job.audioOnly) {
                    add("--format")
                    add("bestaudio/best")
                } else {
                    add("--format")
                    add("best[height<=1080]")
                }
            }
            
            // Audio-only specific options
            if (job.audioOnly) {
                add("--extract-audio")
                add("--audio-format")
                add("mp3")
                add("--audio-quality")
                add("192K")
            }
            
            // Subtitle options
            if (job.includeSubtitles) {
                add("--write-subs")
                add("--write-auto-subs")
                add("--sub-langs")
                add("en,en-US")
            }
            
            // Thumbnail options
            if (job.includeThumbnail) {
                add("--write-thumbnail")
            }
            
            // Progress and hooks
            add("--newline")
            add("--progress-template")
            add("download:%(progress.downloaded_bytes)s/%(progress.total_bytes)s %(progress.speed)s %(progress.eta)s")
            
            // URL (must be last)
            add(job.url)
        }
        
        return command.toList()
    }

    fun buildVersionCommand(ytDlpCommandPrefix: List<String>): List<String> {
        return ytDlpCommandPrefix + listOf("--version")
    }

    private fun sanitizeFilename(filename: String): String {
        // Remove or replace invalid characters for filesystem
        return filename
            .replace(Regex("[<>:\"/\\\\|?*]"), "_")
            .replace(Regex("\\s+"), "_")
            .take(100) // Limit length
    }
}

@Singleton  
class FFmpegCommandBuilder @Inject constructor() {

    fun buildVersionCommand(ffmpegPath: String): List<String> {
        return listOf(ffmpegPath, "-version")
    }

    fun buildMergeCommand(
        videoPath: String,
        audioPath: String,
        outputPath: String,
        ffmpegPath: String
    ): List<String> {
        return listOf(
            ffmpegPath,
            "-i", videoPath,
            "-i", audioPath,
            "-c", "copy",
            "-y", // Overwrite output
            outputPath
        )
    }

    fun buildConvertCommand(
        inputPath: String,
        outputPath: String,
        format: String,
        ffmpegPath: String
    ): List<String> {
        return listOf(
            ffmpegPath,
            "-i", inputPath,
            "-f", format,
            "-y", // Overwrite output
            outputPath
        )
    }
}