package com.videodownloader.ui.activities

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.videodownloader.databinding.ActivityVideoPlayerBinding
import kotlinx.coroutines.launch

/**
 * Video Player Activity with HLS/DASH Streaming Support
 */
class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoPlayerBinding
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupFullscreen()
        setupPlayer()
        setupControls()
    }

    private fun setupFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
        
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
    }

    private fun setupPlayer() {
        player = ExoPlayer.Builder(this)
            .build()
            .apply {
                binding.videoView.player = this
                playWhenReady = true
                
                // Get video URL from intent
                val videoUrl = intent.getStringExtra("video_url")
                    ?: intent.data?.toString()
                
                val videoTitle = intent.getStringExtra("video_title") ?: "Video"
                val isHLS = intent.getBooleanExtra("is_hls", false)
                val isDASH = intent.getBooleanExtra("is_dash", false)
                
                videoUrl?.let { url ->
                    val mediaItem = createMediaItem(url, isHLS, isDASH)
                    setMediaItem(mediaItem)
                    prepare()
                }
            }
    }

    private fun createMediaItem(url: String, isHLS: Boolean = false, isDASH: Boolean = false): MediaItem {
        return when {
            isHLS || url.contains(".m3u8") -> {
                MediaItem.Builder()
                    .setUri(url)
                    .setMimeType(MimeTypes.APPLICATION_M3U8)
                    .build()
            }
            isDASH || url.contains(".mpd") -> {
                MediaItem.Builder()
                    .setUri(url)
                    .setMimeType(MimeTypes.APPLICATION_MPD)
                    .build()
            }
            else -> {
                MediaItem.fromUri(url)
            }
        }
    }

    private fun setupControls() {
        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        // Lock orientation button
        binding.btnOrientation.setOnClickListener {
            requestedOrientation = if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }
        
        // PiP button (for Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.btnPip.visibility = View.VISIBLE
            binding.btnPip.setOnClickListener {
                enterPictureInPictureMode()
            }
        } else {
            binding.btnPip.visibility = View.GONE
        }
        
        // Fullscreen toggle
        binding.btnFullscreen.setOnClickListener {
            toggleFullscreen()
        }
    }

    private fun toggleFullscreen() {
        if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onStop() {
        super.onStop()
        player?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setupFullscreen()
        }
    }
}
