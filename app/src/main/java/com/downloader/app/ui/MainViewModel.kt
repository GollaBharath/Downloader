package com.downloader.app.ui

import android.content.Context
import com.downloader.engine.service.DownloadService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.downloader.core.domain.model.DownloadJob
import com.downloader.core.domain.model.DownloadStatus
import com.downloader.core.domain.model.MediaInfo
import com.downloader.core.domain.repository.DownloadJobRepository
import com.downloader.core.domain.repository.MediaInfoRepository
import com.downloader.core.domain.repository.SettingsRepository
import com.downloader.engine.binary.DownloadStorageResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class MainUiState(
    val url: String = "",
    val isLoadingInfo: Boolean = false,
    val mediaInfo: MediaInfo? = null,
    val jobs: List<DownloadJob> = emptyList(),
    val downloadFolderUri: String? = null,
    val error: String? = null,
    val infoMessage: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val downloadJobRepository: DownloadJobRepository,
    private val mediaInfoRepository: MediaInfoRepository,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val storageResolver = DownloadStorageResolver(appContext)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            downloadJobRepository.getAllJobs().collect { jobs ->
                _uiState.update { it.copy(jobs = jobs) }
            }
        }

        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _uiState.update { it.copy(downloadFolderUri = settings.downloadFolderUri) }
            }
        }
    }

    fun setInitialUrl(url: String) {
        if (_uiState.value.url.isBlank() && url.startsWith("http")) {
            _uiState.update { it.copy(url = url) }
        }
    }

    fun onUrlChanged(url: String) {
        _uiState.update { it.copy(url = url, error = null, infoMessage = null) }
    }

    fun fetchMediaInfo() {
        val url = _uiState.value.url.trim()
        if (!url.startsWith("http")) {
            _uiState.update { it.copy(error = "Enter a valid URL") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingInfo = true, error = null, infoMessage = null) }

            val result = mediaInfoRepository.fetchMediaInfo(url)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoadingInfo = false,
                        mediaInfo = result.getOrNull(),
                        infoMessage = "Metadata loaded"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoadingInfo = false,
                        mediaInfo = null,
                        error = result.exceptionOrNull()?.message ?: "Failed to fetch metadata"
                    )
                }
            }
        }
    }

    fun enqueueDownload(audioOnly: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value
            val url = current.url.trim()
            if (!url.startsWith("http")) {
                _uiState.update { it.copy(error = "Enter a valid URL") }
                return@launch
            }

            val settings = settingsRepository.getSettings()
            val jobId = UUID.randomUUID().toString()
            val resolvedOutputPath = storageResolver.resolveOutputDirectory(settings.downloadFolderUri)
            val job = DownloadJob(
                id = jobId,
                url = url,
                title = current.mediaInfo?.title,
                outputPath = resolvedOutputPath,
                format = null,
                audioOnly = audioOnly,
                includeSubtitles = settings.defaultIncludeSubtitles,
                includeThumbnail = settings.defaultIncludeThumbnail,
                status = DownloadStatus.QUEUED
            )

            downloadJobRepository.insertJob(job)
            DownloadService.startDownload(appContext, jobId)

            _uiState.update { it.copy(infoMessage = "Added to queue", error = null) }
        }
    }

    fun updateDownloadFolderUri(uri: String?) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings()
            settingsRepository.updateSettings(current.copy(downloadFolderUri = uri))
            _uiState.update {
                it.copy(
                    downloadFolderUri = uri,
                    infoMessage = if (uri == null) "Download folder cleared" else "Download folder updated",
                    error = null
                )
            }
        }
    }

    fun cancelJob(jobId: String) {
        DownloadService.cancelDownload(appContext, jobId)
    }

    fun retryJob(jobId: String) {
        viewModelScope.launch {
            val job = downloadJobRepository.getJob(jobId) ?: return@launch
            val retried = job.copy(
                status = DownloadStatus.QUEUED,
                errorMessage = null,
                completedAt = null
            )
            downloadJobRepository.updateJob(retried)
            DownloadService.startDownload(appContext, jobId)
            _uiState.update { it.copy(infoMessage = "Retry queued", error = null) }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(error = null, infoMessage = null) }
    }
}
