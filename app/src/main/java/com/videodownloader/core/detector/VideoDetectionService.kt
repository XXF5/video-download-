package com.videodownloader.core.detector

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.videodownloader.R
import com.videodownloader.VideoDownloaderApp
import com.videodownloader.core.extractor.VideoExtractor
import com.videodownloader.data.models.DetectedVideo
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import javax.inject.Inject

/**
 * Video Detection Service
 * Monitors clipboard and detects videos automatically
 */
@AndroidEntryPoint
class VideoDetectionService : Service() {

    @Inject
    lateinit var videoExtractor: VideoExtractor
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastDetectedUrl: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createDetectionNotification())
        
        // Handle URL from intent
        intent?.getStringExtra("url")?.let { url ->
            detectVideo(url)
        }
        
        return START_STICKY
    }

    private fun detectVideo(url: String) {
        if (url == lastDetectedUrl) return
        
        serviceScope.launch {
            try {
                val videos = videoExtractor.detectVideos(url)
                if (videos.isNotEmpty()) {
                    lastDetectedUrl = url
                    showDetectedNotification(videos.first())
                }
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    private fun createDetectionNotification(): Notification {
        return NotificationCompat.Builder(this, VideoDownloaderApp.CHANNEL_VIDEO_DETECTED)
            .setSmallIcon(R.drawable.ic_video)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Monitoring for videos...")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showDetectedNotification(video: DetectedVideo) {
        val notification = NotificationCompat.Builder(this, VideoDownloaderApp.CHANNEL_VIDEO_DETECTED)
            .setSmallIcon(R.drawable.ic_video)
            .setContentTitle("Video Detected!")
            .setContentText(video.title.ifEmpty { "Tap to download" })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(video.timestamp.toInt(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val NOTIFICATION_ID = 2001
    }
}
