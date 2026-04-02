package com.downloader.engine.parser

import com.downloader.core.domain.model.DownloadPhase
import com.downloader.core.domain.model.DownloadProgress
import javax.inject.Inject
import javax.inject.Singleton
import java.util.regex.Pattern

@Singleton
class YtDlpProgressParser @Inject constructor() {

    companion object {
        // Regex patterns for different yt-dlp output formats
        private val DOWNLOAD_PROGRESS_PATTERN = Pattern.compile(
            "\\[download\\]\\s+(\\d+\\.\\d+)%\\s+of\\s+([\\d.]+\\w+)\\s+at\\s+([\\d.]+\\w+/s)\\s+ETA\\s+(\\d{2}:\\d{2})"
        )
        
        private val DOWNLOAD_FINISHED_PATTERN = Pattern.compile(
            "\\[download\\]\\s+100%\\s+of\\s+([\\d.]+\\w+)\\s+in\\s+(\\d{2}:\\d{2})"
        )
        
        private val POST_PROCESS_PATTERN = Pattern.compile(
            "\\[ffmpeg\\]\\s+(.*)"
        )
        
        private val ERROR_PATTERN = Pattern.compile(
            "ERROR:\\s+(.*)"
        )
        
        private val INFO_PATTERN = Pattern.compile(
            "\\[info\\]\\s+(.*)"
        )
    }

    fun parseProgressLine(jobId: String, line: String): DownloadProgress? {
        val trimmedLine = line.trim()
        
        return when {
            // Download progress
            DOWNLOAD_PROGRESS_PATTERN.matcher(trimmedLine).matches() -> {
                parseDownloadProgress(jobId, trimmedLine)
            }
            
            // Download finished
            DOWNLOAD_FINISHED_PATTERN.matcher(trimmedLine).matches() -> {
                parseDownloadFinished(jobId, trimmedLine)
            }
            
            // Post-processing
            POST_PROCESS_PATTERN.matcher(trimmedLine).matches() -> {
                parsePostProcessing(jobId, trimmedLine)
            }
            
            // Info messages (like format selection)
            INFO_PATTERN.matcher(trimmedLine).matches() -> {
                parseInfo(jobId, trimmedLine)
            }
            
            else -> null
        }
    }

    private fun parseDownloadProgress(jobId: String, line: String): DownloadProgress? {
        val matcher = DOWNLOAD_PROGRESS_PATTERN.matcher(line)
        return if (matcher.find()) {
            val percent = matcher.group(1)?.toFloatOrNull() ?: 0f
            val totalSize = matcher.group(2) ?: return null
            val speed = matcher.group(3) ?: return null
            val eta = matcher.group(4) ?: return null
            
            DownloadProgress(
                jobId = jobId,
                phase = DownloadPhase.DOWNLOADING,
                percentComplete = percent,
                downloadedBytes = calculateDownloadedBytes(percent, totalSize),
                totalBytes = parseSizeToBytes(totalSize),
                speed = speed,
                eta = eta
            )
        } else null
    }

    private fun parseDownloadFinished(jobId: String, line: String): DownloadProgress? {
        val matcher = DOWNLOAD_FINISHED_PATTERN.matcher(line)
        return if (matcher.find()) {
            val totalSize = matcher.group(1) ?: return null
            val totalBytes = parseSizeToBytes(totalSize)
            
            DownloadProgress(
                jobId = jobId,
                phase = DownloadPhase.COMPLETED,
                percentComplete = 100f,
                downloadedBytes = totalBytes ?: 0L,
                totalBytes = totalBytes,
                speed = null,
                eta = null
            )
        } else null
    }

    private fun parsePostProcessing(jobId: String, line: String): DownloadProgress? {
        return DownloadProgress(
            jobId = jobId,
            phase = DownloadPhase.POST_PROCESSING,
            percentComplete = 90f, // Assume post-processing is near completion
            currentFile = extractPostProcessingInfo(line)
        )
    }

    private fun parseInfo(jobId: String, line: String): DownloadProgress? {
        return if (line.contains("Downloading", ignoreCase = true)) {
            DownloadProgress(
                jobId = jobId,
                phase = DownloadPhase.FETCHING_INFO,
                percentComplete = 0f
            )
        } else null
    }

    private fun extractPostProcessingInfo(line: String): String? {
        val matcher = POST_PROCESS_PATTERN.matcher(line)
        return if (matcher.find()) {
            matcher.group(1)
        } else null
    }

    private fun parseSizeToBytes(sizeString: String): Long? {
        try {
            val regex = "(\\d+\\.?\\d*)([KMGT]?i?B?)".toRegex(RegexOption.IGNORE_CASE)
            val match = regex.find(sizeString) ?: return null
            
            val number = match.groupValues[1].toDoubleOrNull() ?: return null
            val unit = match.groupValues[2].uppercase()
            
            val multiplier = when (unit) {
                "B", "" -> 1L
                "KB", "KIB" -> 1024L
                "MB", "MIB" -> 1024L * 1024L
                "GB", "GIB" -> 1024L * 1024L * 1024L
                "TB", "TIB" -> 1024L * 1024L * 1024L * 1024L
                else -> 1L
            }
            
            return (number * multiplier).toLong()
        } catch (e: Exception) {
            return null
        }
    }

    private fun calculateDownloadedBytes(percent: Float, totalSizeString: String): Long {
        val totalBytes = parseSizeToBytes(totalSizeString) ?: return 0L
        return ((percent / 100f) * totalBytes).toLong()
    }

    fun isErrorLine(line: String): Boolean {
        return ERROR_PATTERN.matcher(line.trim()).matches()
    }

    fun extractError(line: String): String? {
        val matcher = ERROR_PATTERN.matcher(line.trim())
        return if (matcher.find()) {
            matcher.group(1)
        } else null
    }
}