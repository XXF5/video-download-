package com.videodownloader.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.videodownloader.R
import com.videodownloader.data.models.DownloadStatus
import com.videodownloader.data.models.DownloadTask
import com.videodownloader.databinding.ActivityDownloadsBinding
import com.videodownloader.ui.adapters.DownloadsAdapter
import com.videodownloader.ui.viewmodels.DownloadsViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels
import kotlinx.coroutines.launch

/**
 * Downloads Activity - Shows all downloads
 */
@AndroidEntryPoint
class DownloadsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDownloadsBinding
    private val viewModel: DownloadsViewModel by viewModels()
    private lateinit var adapter: DownloadsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDownloadsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.downloads)
        
        adapter = DownloadsAdapter(
            onItemClick = { /* Open video */ },
            onPauseClick = { viewModel.pauseDownload(it.id) },
            onResumeClick = { viewModel.resumeDownload(it.id) },
            onCancelClick = { viewModel.cancelDownload(it.id) },
            onRetryClick = { viewModel.retryDownload(it.id) }
        )
        
        binding.rvDownloads.layoutManager = LinearLayoutManager(this)
        binding.rvDownloads.adapter = adapter
        
        // Filter chips
        binding.chipAll.setOnClickListener { viewModel.setFilter(null) }
        binding.chipDownloading.setOnClickListener { viewModel.setFilter(DownloadStatus.DOWNLOADING) }
        binding.chipCompleted.setOnClickListener { viewModel.setFilter(DownloadStatus.COMPLETED) }
        binding.chipFailed.setOnClickListener { viewModel.setFilter(DownloadStatus.FAILED) }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.filteredDownloads.collect { downloads ->
                adapter.submitList(downloads)
                
                binding.emptyView.isVisible = downloads.isEmpty()
                binding.rvDownloads.isVisible = downloads.isNotEmpty()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
