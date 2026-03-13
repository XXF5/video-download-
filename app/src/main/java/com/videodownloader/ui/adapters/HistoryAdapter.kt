package com.videodownloader.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.videodownloader.data.database.BrowserHistoryItem
import com.videodownloader.databinding.ItemHistoryBinding

class HistoryAdapter(
    private val onItemClick: (BrowserHistoryItem) -> Unit,
    private val onDeleteClick: (BrowserHistoryItem) -> Unit
) : ListAdapter<BrowserHistoryItem, HistoryAdapter.HistoryViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HistoryViewHolder(private val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: BrowserHistoryItem) {
            binding.apply {
                tvTitle.text = item.title.ifEmpty { item.url }
                tvUrl.text = item.url
                root.setOnClickListener { onItemClick(item) }
                btnDelete.setOnClickListener { onDeleteClick(item) }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<BrowserHistoryItem>() {
        override fun areItemsTheSame(oldItem: BrowserHistoryItem, newItem: BrowserHistoryItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: BrowserHistoryItem, newItem: BrowserHistoryItem) = oldItem == newItem
    }
}
