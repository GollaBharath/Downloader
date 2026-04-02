package com.downloader.engine.executor

import com.downloader.core.domain.model.DownloadJob
import com.downloader.core.domain.model.DownloadPhase
import com.downloader.core.domain.model.DownloadProgress
import com.downloader.core.domain.repository.SettingsRepository
import com.downloader.engine.binary.DownloadStorageResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject
import java.io.File
import java.lang.reflect.InvocationTargetException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadExecutor @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val storageResolver: DownloadStorageResolver
) {

    suspend fun executeDownload(job: DownloadJob): Flow<DownloadProgress> = flow {
        try {
            val settings = settingsRepository.getSettings()
            val outputDir = storageResolver.resolveOutputDirectory(settings.downloadFolderUri)

            val outputFile = File(outputDir)
            if (!outputFile.exists()) {
                outputFile.mkdirs()
            }

            emit(DownloadProgress(
                jobId = job.id,
                phase = DownloadPhase.FETCHING_INFO,
                percentComplete = 0f
            ))

            emit(DownloadProgress(
                jobId = job.id,
                phase = DownloadPhase.DOWNLOADING,
                percentComplete = 10f
            ))

            val payload = invokePythonDownload(
                url = job.url,
                outputDir = outputDir,
                audioOnly = job.audioOnly,
                formatId = job.format
            )
            val result = JSONObject(payload)
            val currentFilePath = result.optString("filepath", "").takeIf { it.isNotBlank() }

            emit(DownloadProgress(
                jobId = job.id,
                phase = DownloadPhase.POST_PROCESSING,
                percentComplete = 90f,
                currentFile = currentFilePath
            ))

            emit(DownloadProgress(
                jobId = job.id,
                phase = DownloadPhase.COMPLETED,
                percentComplete = 100f,
                currentFile = currentFilePath
            ))

        } catch (e: Exception) {
            emit(createFailedProgress(job.id, "Execution failed: ${describeError(e)}"))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun cancelDownload(jobId: String): Boolean {
        // Cancellation for Python-embedded downloads is not wired yet in MVP.
        return false
    }

    suspend fun pauseDownload(jobId: String): Boolean {
        // For MVP, pause is not implemented - would require more complex process management
        return false
    }

    suspend fun resumeDownload(jobId: String): Boolean {
        // For MVP, resume is not implemented - would require download resumption logic
        return false
    }

    fun isDownloadActive(jobId: String): Boolean {
        return false
    }

    private fun invokePythonDownload(
        url: String,
        outputDir: String,
        audioOnly: Boolean,
        formatId: String?
    ): String {
        try {
            val pythonClass = Class.forName("com.chaquo.python.Python")
            val isStarted = pythonClass.getMethod("isStarted").invoke(null) as Boolean
            if (!isStarted) {
                throw IllegalStateException("Python runtime is not started")
            }

            val python = pythonClass.getMethod("getInstance").invoke(null)
            val module = pythonClass.getMethod("getModule", String::class.java)
                .invoke(python, "yt_dlp_bridge")
            val callAttr = module.javaClass.methods.first {
                it.name == "callAttr" && it.parameterTypes.size == 2
            }

            return callAttr.invoke(
                module,
                "download_media",
                arrayOf<Any?>(url, outputDir, audioOnly, formatId)
            )?.toString() ?: throw IllegalStateException("yt-dlp did not return download result")
        } catch (ite: InvocationTargetException) {
            val target = ite.targetException
            throw IllegalStateException(describeError(target), target)
        } catch (t: Throwable) {
            throw IllegalStateException(describeError(t), t)
        }
    }

    private fun describeError(t: Throwable): String {
        val root = generateSequence(t) { it.cause }.last()
        val message = root.message?.takeIf { it.isNotBlank() }
            ?: t.message?.takeIf { it.isNotBlank() }
            ?: "Unknown error"
        return "${root.javaClass.simpleName}: $message"
    }

    private fun createFailedProgress(jobId: String, error: String): DownloadProgress {
        return DownloadProgress(
            jobId = jobId,
            phase = DownloadPhase.FAILED,
            percentComplete = 0f,
            errorMessage = error
        )
    }
}