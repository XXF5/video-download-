package com.videodownloader.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.videodownloader.data.models.DetectedVideo
import com.videodownloader.databinding.ItemDetectedVideoBinding

class DetectedVideosAdapter(
    private val onDownloadClick: (DetectedVideo) -> Unit,
    private val onStreamClick: (DetectedVideo) -> Unit
) : ListAdapter<DetectedVideo, DetectedVideosAdapter.VideoViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemDetectedVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VideoViewHolder(private val binding: ItemDetectedVideoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(video: DetectedVideo) {
            binding.apply {
                tvTitle.text = video.title.ifEmpty { "Video #${position + 1}" }
                tvQuality.text = video.quality.ifEmpty { "Auto" }
                tvSource.text = video.source.ifEmpty { "Web" }
                btnDownload.setOnClickListener { onDownloadClick(video) }
                btnStream.setOnClickListener { onStreamClick(video) }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DetectedVideo>() {
        override fun areItemsTheSame(oldItem: DetectedVideo, newItem: DetectedVideo) = oldItem.url == newItem.url
        override fun areContentsTheSame(oldItem: DetectedVideo, newItem: DetectedVideo) = oldItem == newItem
    }
}
