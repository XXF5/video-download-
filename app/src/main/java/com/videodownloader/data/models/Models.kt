package com.videodownloader.data.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

enum class VideoQuality(val quality: String, val resolution: String, val height: Int) {
    Q_4320P("4320p", "8K Ultra HD", 4320),
    Q_2160P("2160p", "4K Ultra HD", 2160),
    Q_1440P("1440p", "2K QHD", 1440),
    Q_1080P("1080p", "Full HD", 1080),
    Q_720P("720p", "HD", 720),
    Q_480P("480p", "SD", 480),
    Q_360P("360p", "Low", 360),
    Q_240P("240p", "Very Low", 240),
    Q_144P("144p", "Minimal", 144),
    AUDIO_ONLY("audio", "Audio Only", 0);
    companion object {
        fun fromHeight(height: Int): VideoQuality = when {
            height >= 4320 -> Q_4320P
            height >= 2160 -> Q_2160P
            height >= 1440 -> Q_1440P
            height >= 1080 -> Q_1080P
            height >= 720 -> Q_720P
            height >= 480 -> Q_480P
            height >= 360 -> Q_360P
            height >= 240 -> Q_240P
            else -> Q_144P
        }
        fun fromString(q: String): VideoQuality = values().find { it.quality == q } ?: Q_720P
    }
}

enum class DownloadStatus { PENDING, PREPARING, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED }

@Entity(tableName = "browser_tabs")
@Parcelize
data class BrowserTab(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String = "",
    val favicon: String = "",
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastVisited: Long = System.currentTimeMillis(),
    val isActive: Boolean = false
) : Parcelable

@Entity(tableName = "browser_history")
data class BrowserHistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String = "",
    val favicon: String = "",
    val visitedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmarks")
data class BookmarkItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String = "",
    val favicon: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloads")
data class DownloadTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val videoId: String = "",
    val url: String,
    val originalUrl: String = url,
    val title: String,
    val thumbnail: String = "",
    val quality: String = "",
    val formatId: String = "",
    val extension: String = "mp4",
    val filePath: String = "",
    val fileName: String = "",
    val fileSize: Long = 0,
    val downloadedBytes: Long = 0,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val progress: Int = 0,
    val speed: Long = 0,
    val timeRemaining: Long = 0,
    val error: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val source: String = "",
    val isAudioOnly: Boolean = false
)

@Entity(tableName = "streaming_sessions")
data class StreamingSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val videoId: String = "",
    val url: String,
    val title: String = "",
    val thumbnail: String = "",
    val quality: String = "",
    val source: String = "",
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long = 0,
    val watchDuration: Long = 0,
    val lastPosition: Long = 0
)

@Parcelize
data class VideoInfo(
    val id: String = System.currentTimeMillis().toString(),
    val url: String,
    val title: String,
    val description: String = "",
    val duration: Long = 0,
    val thumbnail: String = "",
    val uploader: String = "",
    val formats: List<VideoFormat> = emptyList(),
    val source: String = "",
    val originalUrl: String = url
) : Parcelable

@Parcelize
data class VideoFormat(
    val formatId: String,
    val url: String,
    val quality: VideoQuality,
    val extension: String,
    val fileSize: Long = 0,
    val bitrate: Long = 0,
    val height: Int = 0,
    val isAudioOnly: Boolean = false,
    val isHLS: Boolean = false
) : Parcelable

@Parcelize
data class DetectedVideo(
    val url: String,
    val title: String = "",
    val thumbnail: String = "",
    val duration: String = "",
    val quality: String = "",
    val size: String = "",
    val source: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isHLS: Boolean = false
) : Parcelable
