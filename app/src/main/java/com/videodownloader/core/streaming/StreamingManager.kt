package com.videodownloader.core.streaming

import android.content.Context
import android.content.Intent
import com.videodownloader.data.database.StreamingSessionDao
import com.videodownloader.data.models.StreamingSession
import com.videodownloader.data.models.VideoFormat
import com.videodownloader.ui.activities.VideoPlayerActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Streaming Manager - Handles video streaming with HLS/DASH support
 */
@Singleton
class StreamingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val streamingSessionDao: StreamingSessionDao
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    fun startStreaming(
        videoUrl: String,
        title: String = "",
        thumbnail: String = "",
        quality: String = "",
        source: String = ""
    ) {
        val intent = Intent(context, VideoPlayerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("video_url", videoUrl)
            putExtra("video_title", title)
            putExtra("video_thumbnail", thumbnail)
            putExtra("video_quality", quality)
            putExtra("video_source", source)
            putExtra("is_streaming", true)
        }
        context.startActivity(intent)
        
        scope.launch {
            streamingSessionDao.insertSession(
                StreamingSession(
                    videoId = System.currentTimeMillis().toString(),
                    url = videoUrl,
                    title = title,
                    thumbnail = thumbnail,
                    quality = quality,
                    source = source,
                    startedAt = System.currentTimeMillis()
                )
            )
        }
    }
    
    fun startStreaming(format: VideoFormat, title: String = "", source: String = "") {
        startStreaming(
            videoUrl = format.url,
            title = title,
            quality = format.quality.quality,
            source = source
        )
    }
    
    fun getRecentSessions(limit: Int = 20) = streamingSessionDao.getRecentSessions(limit)
    
    suspend fun clearSessions() = streamingSessionDao.clearAllSessions()
}
