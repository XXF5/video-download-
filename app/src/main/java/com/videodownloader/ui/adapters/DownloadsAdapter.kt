package com.videodownloader.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.videodownloader.R
import com.videodownloader.data.models.DownloadStatus
import com.videodownloader.data.models.DownloadTask
import com.videodownloader.databinding.ItemDownloadBinding

/**
 * Downloads Adapter - Displays download tasks in a list
 */
class DownloadsAdapter(
    private val onItemClick: (DownloadTask) -> Unit,
    private val onPauseClick: (DownloadTask) -> Unit,
    private val onResumeClick: (DownloadTask) -> Unit,
    private val onCancelClick: (DownloadTask) -> Unit,
    private val onRetryClick: (DownloadTask) -> Unit
) : ListAdapter<DownloadTask, DownloadsAdapter.DownloadViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadViewHolder {
        val binding = ItemDownloadBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DownloadViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DownloadViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DownloadViewHolder(
        private val binding: ItemDownloadBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: DownloadTask) {
            binding.apply {
                // Title
                tvTitle.text = task.title
                
                // Source/Quality
                tvSource.text = "${task.source} • ${task.quality}"
                
                // Thumbnail
                Glide.with(ivThumbnail)
                    .load(task.thumbnail)
                    .placeholder(R.drawable.ic_video_placeholder)
                    .into(ivThumbnail)
                
                // Status
                when (task.status) {
                    DownloadStatus.PENDING -> {
                        tvStatus.setText(R.string.pending)
                        progressBar.isVisible = false
                        tvProgress.isVisible = false
                        btnPause.isVisible = false
                        btnResume.isVisible = false
                        btnCancel.isVisible = true
                        btnRetry.isVisible = false
                    }
                    DownloadStatus.PREPARING -> {
                        tvStatus.setText(R.string.preparing)
                        progressBar.isIndeterminate = true
                        progressBar.isVisible = true
                        tvProgress.isVisible = false
                        btnPause.isVisible = false
                        btnResume.isVisible = false
                        btnCancel.isVisible = true
                        btnRetry.isVisible = false
                    }
                    DownloadStatus.DOWNLOADING, DownloadStatus.RESUMING -> {
                        tvStatus.text = "${task.downloadedFormatted} / ${task.totalFormatted} • ${task.speedFormatted}"
                        progressBar.isIndeterminate = false
                        progressBar.progress = task.progressPercent
                        progressBar.isVisible = true
                        tvProgress.text = "${task.progressPercent}%"
                        tvProgress.isVisible = true
                        btnPause.isVisible = true
                        btnResume.isVisible = false
                        btnCancel.isVisible = true
                        btnRetry.isVisible = false
                    }
                    DownloadStatus.PAUSED -> {
                        tvStatus.text = root.context.getString(R.string.paused_at, task.progressPercent)
                        progressBar.isIndeterminate = false
                        progressBar.progress = task.progressPercent
                        progressBar.isVisible = true
                        tvProgress.text = "${task.progressPercent}%"
                        tvProgress.isVisible = true
                        btnPause.isVisible = false
                        btnResume.isVisible = true
                        btnCancel.isVisible = true
                        btnRetry.isVisible = false
                    }
                    DownloadStatus.COMPLETED -> {
                        tvStatus.setText(R.string.completed)
                        progressBar.isVisible = false
                        tvProgress.isVisible = false
                        btnPause.isVisible = false
                        btnResume.isVisible = false
                        btnCancel.isVisible = false
                        btnRetry.isVisible = false
                    }
                    DownloadStatus.FAILED -> {
                        tvStatus.text = root.context.getString(R.string.failed_reason, task.error)
                        progressBar.isVisible = false
                        tvProgress.isVisible = false
                        btnPause.isVisible = false
                        btnResume.isVisible = false
                        btnCancel.isVisible = true
                        btnRetry.isVisible = true
                    }
                    DownloadStatus.CANCELLED -> {
                        tvStatus.setText(R.string.cancelled)
                        progressBar.isVisible = false
                        tvProgress.isVisible = false
                        btnPause.isVisible = false
                        btnResume.isVisible = false
                        btnCancel.isVisible = false
                        btnRetry.isVisible = false
                    }
                }
                
                // Click listeners
                root.setOnClickListener { onItemClick(task) }
                btnPause.setOnClickListener { onPauseClick(task) }
                btnResume.setOnClickListener { onResumeClick(task) }
                btnCancel.setOnClickListener { onCancelClick(task) }
                btnRetry.setOnClickListener { onRetryClick(task) }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DownloadTask>() {
        override fun areItemsTheSame(oldItem: DownloadTask, newItem: DownloadTask): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DownloadTask, newItem: DownloadTask): Boolean {
            return oldItem == newItem
        }
    }
}
