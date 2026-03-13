package com.videodownloader

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class VideoDownloaderApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Download Progress Channel
            val downloadChannel = NotificationChannel(
                CHANNEL_DOWNLOAD_PROGRESS,
                getString(R.string.channel_download_progress),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_download_progress_desc)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(downloadChannel)

            // Download Complete Channel
            val completeChannel = NotificationChannel(
                CHANNEL_DOWNLOAD_COMPLETE,
                getString(R.string.channel_download_complete),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_download_complete_desc)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(completeChannel)

            // Video Detected Channel
            val detectedChannel = NotificationChannel(
                CHANNEL_VIDEO_DETECTED,
                getString(R.string.channel_video_detected),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_video_detected_desc)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(detectedChannel)
        }
    }

    companion object {
        const val CHANNEL_DOWNLOAD_PROGRESS = "download_progress"
        const val CHANNEL_DOWNLOAD_COMPLETE = "download_complete"
        const val CHANNEL_VIDEO_DETECTED = "video_detected"
    }
}
