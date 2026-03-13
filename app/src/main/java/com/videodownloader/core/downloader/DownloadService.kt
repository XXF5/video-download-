package com.videodownloader.core.downloader

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.videodownloader.R
import com.videodownloader.VideoDownloaderApp
import com.videodownloader.data.models.DownloadStatus
import com.videodownloader.data.models.DownloadTask
import com.videodownloader.data.repository.DownloadRepository
import com.videodownloader.ui.activities.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Foreground Service for Downloads
 * Keeps downloads running even when app is in background
 */
@AndroidEntryPoint
class DownloadService : Service() {

    @Inject
    lateinit var downloadManager: DownloadManager
    
    @Inject
    lateinit var repository: DownloadRepository

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private var notificationBuilder: NotificationCompat.Builder? = null
    private var notificationManager: NotificationManagerCompat? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = VideoDownloaderApp.CHANNEL_DOWNLOAD_PROGRESS
        
        const val ACTION_START = "com.videodownloader.action.START"
        const val ACTION_PAUSE = "com.videodownloader.action.PAUSE"
        const val ACTION_CANCEL = "com.videodownloader.action.CANCEL"
        const val ACTION_PAUSE_ALL = "com.videodownloader.action.PAUSE_ALL"
        const val ACTION_RESUME_ALL = "com.videodownloader.action.RESUME_ALL"
        
        const val EXTRA_TASK_ID = "task_id"
    }

    inner class LocalBinder : Binder() {
        fun getService(): DownloadService = this@DownloadService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(this)
        setupNotification()
        observeDownloads()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
                if (taskId > 0) {
                    serviceScope.launch {
                        repository.getDownloadById(taskId)?.let { task ->
                            downloadManager.startDownload(task)
                        }
                    }
                }
            }
            ACTION_PAUSE -> {
                val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
                if (taskId > 0) {
                    serviceScope.launch {
                        downloadManager.pauseDownload(taskId)
                    }
                }
            }
            ACTION_CANCEL -> {
                val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
                if (taskId > 0) {
                    serviceScope.launch {
                        downloadManager.cancelDownload(taskId)
                    }
                }
            }
            ACTION_PAUSE_ALL -> {
                serviceScope.launch {
                    downloadManager.pauseAllDownloads()
                }
            }
            ACTION_RESUME_ALL -> {
                serviceScope.launch {
                    downloadManager.resumeAllPausedDownloads()
                }
            }
        }
        
        return START_STICKY
    }

    private fun setupNotification() {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(getString(R.string.downloading))
            .setContentText(getString(R.string.preparing_download))
            .setOngoing(true)
            .setProgress(100, 0, true)
            .setContentIntent(pendingIntent)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        startForeground(NOTIFICATION_ID, notificationBuilder?.build())
    }

    private fun observeDownloads() {
        serviceScope.launch {
            downloadManager.downloadQueue.collect { tasks ->
                updateNotification(tasks)
                
                // Stop service if no active downloads
                val activeCount = tasks.count { 
                    it.status in listOf(DownloadStatus.DOWNLOADING, DownloadStatus.PREPARING, DownloadStatus.RESUMING)
                }
                
                if (activeCount == 0) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    private fun updateNotification(tasks: List<DownloadTask>) {
        val activeTasks = tasks.filter {
            it.status in listOf(DownloadStatus.DOWNLOADING, DownloadStatus.PREPARING, DownloadStatus.RESUMING)
        }

        if (activeTasks.isEmpty()) {
            return
        }

        val firstTask = activeTasks.first()
        val speeds = downloadManager.getAllDownloadSpeeds()
        val totalSpeed = speeds.values.sum()

        notificationBuilder?.apply {
            if (activeTasks.size == 1) {
                setContentTitle(firstTask.title)
                setContentText("${firstTask.downloadedFormatted} / ${firstTask.totalFormatted} • ${firstTask.speedFormatted}")
                setProgress(100, firstTask.progressPercent, false)
            } else {
                setContentTitle(getString(R.string.downloading_count, activeTasks.size))
                setContentText(getString(R.string.total_speed, formatSpeed(totalSpeed)))
                setProgress(100, (activeTasks.sumOf { it.progressPercent } / activeTasks.size), false)
            }
            
            notificationManager?.notify(NOTIFICATION_ID, build())
        }
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec < 1024 -> "$bytesPerSec B/s"
            bytesPerSec < 1024 * 1024 -> String.format("%.1f KB/s", bytesPerSec / 1024.0)
            else -> String.format("%.1f MB/s", bytesPerSec / (1024.0 * 1024))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
