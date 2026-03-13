package com.videodownloader.core.downloader

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.videodownloader.R
import com.videodownloader.VideoDownloaderApp
import com.videodownloader.data.models.DownloadStatus
import com.videodownloader.data.models.DownloadTask
import com.videodownloader.data.repository.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Advanced Download Manager - Heart of the Video Downloader
 * Features:
 * - Multi-threaded downloads
 * - Pause/Resume support
 * - Speed calculation
 * - Progress tracking
 * - Concurrent downloads
 * - Auto retry
 */
@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DownloadRepository
) {
    companion object {
        const val TAG = "DownloadManager"
        const val MAX_CONCURRENT_DOWNLOADS = 3
        const val BUFFER_SIZE = 8192
        const val MAX_RETRIES = 3
        const val RETRY_DELAY_MS = 2000L
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val downloadJobs = ConcurrentHashMap<Long, Job>()
    private val downloadSpeeds = ConcurrentHashMap<Long, Long>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Download queue state
    private val _downloadQueue = MutableStateFlow<List<DownloadTask>>(emptyList())
    val downloadQueue: StateFlow<List<DownloadTask>> = _downloadQueue.asStateFlow()

    // Active downloads count
    private val _activeDownloadsCount = MutableStateFlow(0)
    val activeDownloadsCount: StateFlow<Int> = _activeDownloadsCount.asStateFlow()

    init {
        // Observe downloads
        scope.launch {
            repository.getAllDownloads().collect { downloads ->
                _downloadQueue.value = downloads
            }
        }
        
        scope.launch {
            repository.getActiveDownloadsCount().collect { count ->
                _activeDownloadsCount.value = count
            }
        }
    }

    /**
     * Start a download
     */
    suspend fun startDownload(task: DownloadTask): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                // Insert into database
                val id = repository.insertDownload(task.copy(status = DownloadStatus.PENDING))
                
                // Start download job
                startDownloadJob(id)
                
                Result.success(id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start download", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Pause a download
     */
    suspend fun pauseDownload(id: Long) {
        withContext(Dispatchers.IO) {
            downloadJobs[id]?.cancel()
            downloadJobs.remove(id)
            
            repository.getDownloadById(id)?.let { task ->
                repository.updateStatus(id, DownloadStatus.PAUSED)
            }
        }
    }

    /**
     * Resume a download
     */
    suspend fun resumeDownload(id: Long) {
        withContext(Dispatchers.IO) {
            repository.getDownloadById(id)?.let { task ->
                if (task.status == DownloadStatus.PAUSED) {
                    repository.updateStatus(id, DownloadStatus.RESUMING)
                    startDownloadJob(id)
                }
            }
        }
    }

    /**
     * Cancel a download
     */
    suspend fun cancelDownload(id: Long) {
        withContext(Dispatchers.IO) {
            downloadJobs[id]?.cancel()
            downloadJobs.remove(id)
            
            repository.updateStatus(id, DownloadStatus.CANCELLED)
            
            // Delete partial file
            repository.getDownloadById(id)?.let { task ->
                if (task.filePath.isNotEmpty()) {
                    File(task.filePath).delete()
                }
            }
        }
    }

    /**
     * Retry a failed download
     */
    suspend fun retryDownload(id: Long) {
        withContext(Dispatchers.IO) {
            repository.getDownloadById(id)?.let { task ->
                repository.updateStatus(id, DownloadStatus.PENDING)
                startDownloadJob(id)
            }
        }
    }

    /**
     * Delete a download
     */
    suspend fun deleteDownload(id: Long, deleteFile: Boolean = true) {
        withContext(Dispatchers.IO) {
            downloadJobs[id]?.cancel()
            downloadJobs.remove(id)
            
            repository.getDownloadById(id)?.let { task ->
                if (deleteFile && task.filePath.isNotEmpty()) {
                    File(task.filePath).delete()
                }
            }
            
            repository.deleteDownloadById(id)
        }
    }

    /**
     * Start download job
     */
    private fun startDownloadJob(id: Long) {
        val job = scope.launch {
            var retryCount = 0
            
            while (retryCount < MAX_RETRIES) {
                try {
                    val task = repository.getDownloadById(id) ?: return@launch
                    
                    // Wait if max concurrent downloads reached
                    while (_activeDownloadsCount.value >= MAX_CONCURRENT_DOWNLOADS) {
                        delay(1000)
                    }
                    
                    // Update status
                    repository.updateStatus(id, DownloadStatus.DOWNLOADING)
                    
                    // Execute download
                    downloadFile(task)
                    
                    // If we reach here, download completed
                    break
                    
                } catch (e: CancellationException) {
                    // Download was cancelled/paused
                    throw e
                    
                } catch (e: Exception) {
                    retryCount++
                    Log.e(TAG, "Download failed (attempt $retryCount): ${e.message}")
                    
                    if (retryCount >= MAX_RETRIES) {
                        repository.updateStatus(id, DownloadStatus.FAILED, e.message ?: "Unknown error")
                    } else {
                        delay(RETRY_DELAY_MS)
                    }
                }
            }
        }
        
        downloadJobs[id] = job
    }

    /**
     * Download file with progress tracking
     */
    private suspend fun downloadFile(task: DownloadTask) {
        withContext(Dispatchers.IO) {
            val outputFile = File(task.filePath)
            val tempFile = File("${task.filePath}.tmp")
            
            // Check for partial download
            val existingBytes = if (tempFile.exists()) tempFile.length() else 0L
            
            val request = Request.Builder()
                .url(task.url)
                .apply {
                    if (existingBytes > 0) {
                        header("Range", "bytes=$existingBytes-")
                    }
                }
                .build()

            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }

            val contentLength = response.body?.contentLength() ?: 0L
            val totalSize = if (existingBytes > 0) existingBytes + contentLength else contentLength

            // Update file size
            repository.updateDownload(task.id, task.copy(fileSize = totalSize))

            // Create parent directories
            outputFile.parentFile?.mkdirs()

            // Start download
            val startTime = System.currentTimeMillis()
            var lastUpdateTime = startTime
            var bytesReadSinceUpdate = 0L
            var totalBytesRead = existingBytes

            response.body?.byteStream()?.use { input ->
                FileOutputStream(tempFile, existingBytes > 0).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        // Check if cancelled
                        if (!isActive) {
                            response.close()
                            throw CancellationException()
                        }
                        
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        bytesReadSinceUpdate += bytesRead
                        
                        // Update progress every 500ms
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastUpdateTime >= 500) {
                            val elapsed = currentTime - startTime
                            val speed = if (elapsed > 0) (totalBytesRead * 1000 / elapsed) else 0L
                            val timeRemaining = if (speed > 0) (totalSize - totalBytesRead) / speed else 0L
                            
                            repository.updateProgress(
                                id = task.id,
                                downloadedBytes = totalBytesRead,
                                speed = speed,
                                timeRemaining = timeRemaining
                            )
                            
                            downloadSpeeds[task.id] = speed
                            lastUpdateTime = currentTime
                            bytesReadSinceUpdate = 0
                        }
                    }
                }
            }

            // Move temp file to final location
            tempFile.renameTo(outputFile)
            
            // Mark as completed
            repository.updateStatus(task.id, DownloadStatus.COMPLETED)
            downloadSpeeds.remove(task.id)
        }
    }

    /**
     * Get download speed for a task
     */
    fun getDownloadSpeed(id: Long): Long {
        return downloadSpeeds[id] ?: 0L
    }

    /**
     * Get all download speeds
     */
    fun getAllDownloadSpeeds(): Map<Long, Long> {
        return downloadSpeeds.toMap()
    }

    /**
     * Cleanup completed downloads
     */
    suspend fun cleanupCompletedDownloads() {
        repository.deleteCompletedDownloads()
    }

    /**
     * Cleanup failed downloads
     */
    suspend fun cleanupFailedDownloads() {
        repository.deleteFailedDownloads()
    }

    /**
     * Pause all downloads
     */
    suspend fun pauseAllDownloads() {
        repository.getActiveDownloads().forEach { task ->
            pauseDownload(task.id)
        }
    }

    /**
     * Resume all paused downloads
     */
    suspend fun resumeAllPausedDownloads() {
        repository.getPausedDownloads().forEach { task ->
            resumeDownload(task.id)
        }
    }

    /**
     * Cancel all downloads
     */
    suspend fun cancelAllDownloads() {
        downloadJobs.keys.toList().forEach { id ->
            cancelDownload(id)
        }
    }
}
