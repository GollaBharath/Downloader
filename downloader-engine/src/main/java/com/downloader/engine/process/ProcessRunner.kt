package com.downloader.engine.process

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

sealed class ProcessResult {
    data class Output(val line: String) : ProcessResult()
    data class Error(val line: String) : ProcessResult()
    data class Exit(val code: Int) : ProcessResult()
}

@Singleton
class ProcessRunner @Inject constructor() {

    suspend fun executeCommand(
        command: List<String>,
        workingDirectory: File? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val processBuilder = ProcessBuilder(command).apply {
                workingDirectory?.let { directory(it) }
                redirectErrorStream(false)
            }

            val process = processBuilder.start()
            
            val output = StringBuilder()
            val error = StringBuilder()
            
            // Read stdout
            val outputReader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            // Read output streams
            outputReader.useLines { lines ->
                lines.forEach { line ->
                    output.appendLine(line)
                }
            }
            
            errorReader.useLines { lines ->
                lines.forEach { line ->
                    error.appendLine(line)
                }
            }
            
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                Result.success(output.toString())
            } else {
                Result.failure(ProcessException("Command failed with exit code $exitCode: ${error.toString()}"))
            }
        } catch (e: Exception) {
            Result.failure(ProcessException("Failed to execute command: ${e.message}", e))
        }
    }

    fun executeCommandStreaming(
        command: List<String>,
        workingDirectory: File? = null,
        onProcessStarted: ((Process) -> Unit)? = null
    ): Flow<ProcessResult> = flow {
        try {
            val processBuilder = ProcessBuilder(command).apply {
                workingDirectory?.let { directory(it) }
                redirectErrorStream(false)
            }

            val process = processBuilder.start()
            onProcessStarted?.invoke(process)
            
            val outputReader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            // Start reading output streams in parallel
            val outputJob = coroutineScope {
                async {
                    outputReader.useLines { lines ->
                        lines.forEach { line ->
                            emit(ProcessResult.Output(line))
                        }
                    }
                }
            }
            
            val errorJob = coroutineScope {
                async {
                    errorReader.useLines { lines ->
                        lines.forEach { line ->
                            emit(ProcessResult.Error(line))
                        }
                    }
                }
            }
            
            // Wait for both streams to complete
            outputJob.await()
            errorJob.await()
            
            // Wait for process to complete
            val exitCode = process.waitFor()
            emit(ProcessResult.Exit(exitCode))
            
        } catch (e: Exception) {
            emit(ProcessResult.Error("Process execution failed: ${e.message}"))
            emit(ProcessResult.Exit(-1))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun killProcess(process: Process): Boolean = withContext(Dispatchers.IO) {
        try {
            process.destroyForcibly()
            process.waitFor()
            true
        } catch (e: Exception) {
            false
        }
    }
}

class ProcessException(message: String, cause: Throwable? = null) : Exception(message, cause)