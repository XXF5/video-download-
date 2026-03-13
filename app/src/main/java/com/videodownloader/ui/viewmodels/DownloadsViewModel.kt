package com.videodownloader.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videodownloader.core.downloader.DownloadManager
import com.videodownloader.data.models.DownloadStatus
import com.videodownloader.data.models.DownloadTask
import com.videodownloader.data.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val downloadManager: DownloadManager
) : ViewModel() {

    private val _filter = MutableStateFlow<DownloadStatus?>(null)
    
    val filteredDownloads: StateFlow<List<DownloadTask>> = combine(
        downloadRepository.getAllDownloads(),
        _filter
    ) { downloads, filter ->
        if (filter != null) {
            downloads.filter { it.status == filter }
        } else {
            downloads
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setFilter(status: DownloadStatus?) {
        _filter.value = status
    }

    fun pauseDownload(id: Long) {
        viewModelScope.launch {
            downloadManager.pauseDownload(id)
        }
    }

    fun resumeDownload(id: Long) {
        viewModelScope.launch {
            downloadManager.resumeDownload(id)
        }
    }

    fun cancelDownload(id: Long) {
        viewModelScope.launch {
            downloadManager.cancelDownload(id)
        }
    }

    fun retryDownload(id: Long) {
        viewModelScope.launch {
            downloadManager.retryDownload(id)
        }
    }

    fun deleteDownload(id: Long) {
        viewModelScope.launch {
            downloadManager.deleteDownload(id)
        }
    }

    fun clearCompleted() {
        viewModelScope.launch {
            downloadManager.cleanupCompletedDownloads()
        }
    }

    fun clearFailed() {
        viewModelScope.launch {
            downloadManager.cleanupFailedDownloads()
        }
    }
}
