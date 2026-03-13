package com.videodownloader.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videodownloader.core.downloader.DownloadManager
import com.videodownloader.core.extractor.VideoExtractor
import com.videodownloader.data.models.*
import com.videodownloader.data.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main ViewModel
 * Handles UI logic for MainActivity
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val videoExtractor: VideoExtractor,
    private val downloadManager: DownloadManager,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    // UI State
    private val _videoInfo = MutableStateFlow<VideoInfo?>(null)
    val videoInfo: StateFlow<VideoInfo?> = _videoInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Downloads
    val downloads: StateFlow<List<DownloadTask>> = downloadRepository.getAllDownloads()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val activeDownloadsCount: StateFlow<Int> = downloadRepository.getActiveDownloadsCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    /**
     * Analyze URL and extract video information
     */
    fun analyzeUrl(url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _videoInfo.value = null

            val result = videoExtractor.extract(url)

            result.fold(
                onSuccess = { info ->
                    _videoInfo.value = info
                    _isLoading.value = false
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to analyze URL"
                    _isLoading.value = false
                }
            )
        }
    }

    /**
     * Start a download
     */
    fun startDownload(task: DownloadTask) {
        viewModelScope.launch {
            downloadManager.startDownload(task)
        }
    }

    /**
     * Pause a download
     */
    fun pauseDownload(id: Long) {
        viewModelScope.launch {
            downloadManager.pauseDownload(id)
        }
    }

    /**
     * Resume a download
     */
    fun resumeDownload(id: Long) {
        viewModelScope.launch {
            downloadManager.resumeDownload(id)
        }
    }

    /**
     * Cancel a download
     */
    fun cancelDownload(id: Long) {
        viewModelScope.launch {
            downloadManager.cancelDownload(id)
        }
    }

    /**
     * Retry a failed download
     */
    fun retryDownload(id: Long) {
        viewModelScope.launch {
            downloadManager.retryDownload(id)
        }
    }

    /**
     * Clear error
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Clear video info
     */
    fun clearVideoInfo() {
        _videoInfo.value = null
    }
}
