package com.videodownloader.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.videodownloader.data.database.BrowserHistoryItem
import com.videodownloader.databinding.ItemRecentHistoryBinding

class RecentHistoryAdapter(
    private val onItemClick: (BrowserHistoryItem) -> Unit
) : ListAdapter<BrowserHistoryItem, RecentHistoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemRecentHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: BrowserHistoryItem) {
            binding.tvTitle.text = item.title.ifEmpty { item.url }
            binding.tvUrl.text = item.url
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<BrowserHistoryItem>() {
        override fun areItemsTheSame(oldItem: BrowserHistoryItem, newItem: BrowserHistoryItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: BrowserHistoryItem, newItem: BrowserHistoryItem) = oldItem == newItem
    }
}
