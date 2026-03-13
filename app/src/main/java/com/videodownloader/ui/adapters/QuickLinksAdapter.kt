package com.videodownloader.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.videodownloader.databinding.ItemQuickLinkBinding

data class QuickLink(val name: String, val url: String, val icon: Int)

class QuickLinksAdapter(
    private val onItemClick: (QuickLink) -> Unit
) : ListAdapter<QuickLink, QuickLinksAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemQuickLinkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemQuickLinkBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: QuickLink) {
            binding.ivIcon.setImageResource(item.icon)
            binding.tvName.text = item.name
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<QuickLink>() {
        override fun areItemsTheSame(oldItem: QuickLink, newItem: QuickLink) = oldItem.url == newItem.url
        override fun areContentsTheSame(oldItem: QuickLink, newItem: QuickLink) = oldItem == newItem
    }
}
