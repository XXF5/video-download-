package com.videodownloader.ui.activities

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.videodownloader.R
import com.videodownloader.data.models.*
import com.videodownloader.databinding.ActivityMainBinding
import com.videodownloader.databinding.DialogQualitySelectorBinding
import com.videodownloader.databinding.ItemVideoFormatBinding
import com.videodownloader.ui.adapters.DownloadsAdapter
import com.videodownloader.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

/**
 * Main Activity - Entry point of the app
 * Features:
 * - Quick Browser Access (Like 1DM)
 * - URL input with paste button
 * - Quick video detection
 * - Quality selection
 * - Download management
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var downloadsAdapter: DownloadsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        setupObservers()
        handleIntent(intent)
    }

    private fun setupUI() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)
        
        // ===== BROWSER BUTTON - MAIN FEATURE =====
        binding.browserCard.setOnClickListener {
            // Open browser like 1DM
            val intent = Intent(this, BrowserActivity::class.java)
            startActivity(intent)
        }
        
        // Downloads card
        binding.downloadsCard.setOnClickListener {
            startActivity(Intent(this, DownloadsActivity::class.java))
        }
        
        // History card
        binding.historyCard.setOnClickListener {
            startActivity(Intent(this, BrowserActivity::class.java).apply {
                putExtra("show_history", true)
            })
        }
        
        // View all downloads
        binding.btnViewAllDownloads.setOnClickListener {
            startActivity(Intent(this, DownloadsActivity::class.java))
        }
        
        // Setup URL input
        binding.etUrl.doOnTextChanged { text, _, _, _ ->
            binding.btnAnalyze.isEnabled = !text.isNullOrBlank()
        }
        
        // Paste button
        binding.btnPaste.setOnClickListener {
            pasteFromClipboard()
        }
        
        // Analyze button
        binding.btnAnalyze.setOnClickListener {
            val url = binding.etUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                analyzeUrl(url)
            }
        }
        
        // Setup downloads list
        setupDownloadsList()
    }

    private fun setupDownloadsList() {
        downloadsAdapter = DownloadsAdapter(
            onItemClick = { task -> onDownloadItemClick(task) },
            onPauseClick = { task -> viewModel.pauseDownload(task.id) },
            onResumeClick = { task -> viewModel.resumeDownload(task.id) },
            onCancelClick = { task -> viewModel.cancelDownload(task.id) },
            onRetryClick = { task -> viewModel.retryDownload(task.id) }
        )
        
        binding.rvDownloads.adapter = downloadsAdapter
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.videoInfo.collect { videoInfo ->
                videoInfo?.let { showQualitySelector(it) }
            }
        }
        
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.btnAnalyze.isEnabled = !isLoading
            }
        }
        
        lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show()
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.downloads.collect { downloads ->
                downloadsAdapter.submitList(downloads.take(5))
                
                if (downloads.isEmpty()) {
                    binding.tvNoDownloads.visibility = View.VISIBLE
                    binding.rvDownloads.visibility = View.GONE
                } else {
                    binding.tvNoDownloads.visibility = View.GONE
                    binding.rvDownloads.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun pasteFromClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.let { text ->
            if (text.startsWith("http://") || text.startsWith("https://")) {
                binding.etUrl.setText(text)
            } else {
                Toast.makeText(this, R.string.invalid_url, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun analyzeUrl(url: String) {
        viewModel.analyzeUrl(url)
    }

    private fun showQualitySelector(videoInfo: VideoInfo) {
        val dialog = BottomSheetDialog(this)
        val dialogBinding = DialogQualitySelectorBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        
        dialogBinding.tvTitle.text = videoInfo.title
        dialogBinding.tvSource.text = videoInfo.source
        
        Glide.with(this)
            .load(videoInfo.thumbnail)
            .placeholder(R.drawable.ic_video_placeholder)
            .into(dialogBinding.ivThumbnail)
        
        videoInfo.formats.sortedByDescending { it.height }.forEach { format ->
            val formatBinding = ItemVideoFormatBinding.inflate(layoutInflater, dialogBinding.formatsContainer, false)
            
            formatBinding.tvQuality.text = format.displayQuality
            formatBinding.tvExtension.text = format.extension.uppercase()
            formatBinding.tvSize.text = format.fileSizeFormatted
            
            formatBinding.root.setOnClickListener {
                startDownload(videoInfo, format)
                dialog.dismiss()
            }
            
            dialogBinding.formatsContainer.addView(formatBinding.root)
        }
        
        dialog.show()
    }

    private fun startDownload(videoInfo: VideoInfo, format: VideoFormat) {
        val fileName = sanitizeFileName(videoInfo.title) + "." + format.extension
        val downloadDir = getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
        val filePath = File(downloadDir, fileName).absolutePath
        
        val task = DownloadTask(
            videoId = videoInfo.id,
            url = format.url,
            originalUrl = videoInfo.originalUrl,
            title = videoInfo.title,
            thumbnail = videoInfo.thumbnail,
            quality = format.quality.quality,
            formatId = format.formatId,
            extension = format.extension,
            filePath = filePath,
            fileName = fileName,
            fileSize = format.fileSize,
            source = videoInfo.source,
            isAudioOnly = format.isAudioOnly
        )
        
        viewModel.startDownload(task)
        Toast.makeText(this, R.string.download_started, Toast.LENGTH_SHORT).show()
        binding.etUrl.text?.clear()
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(100)
    }

    private fun onDownloadItemClick(task: DownloadTask) {
        when (task.status) {
            DownloadStatus.COMPLETED -> {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(task.filePath), "video/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, getString(R.string.open_with)))
            }
            else -> {}
        }
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.let { url ->
                        binding.etUrl.setText(url)
                        analyzeUrl(url)
                    }
                }
            }
            Intent.ACTION_VIEW -> {
                intent.data?.toString()?.let { url ->
                    binding.etUrl.setText(url)
                    analyzeUrl(url)
                }
            }
        }
    }
}
