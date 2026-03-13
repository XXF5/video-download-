package com.videodownloader.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.videodownloader.data.models.BrowserTab
import com.videodownloader.databinding.ItemTabBinding

class TabsAdapter(
    private val onItemClick: (BrowserTab) -> Unit,
    private val onCloseClick: (BrowserTab) -> Unit
) : ListAdapter<BrowserTab, TabsAdapter.TabViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        val binding = ItemTabBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TabViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TabViewHolder(private val binding: ItemTabBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tab: BrowserTab) {
            binding.apply {
                tvTitle.text = tab.title.ifEmpty { "New Tab" }
                tvUrl.text = tab.url
                root.setOnClickListener { onItemClick(tab) }
               .btnClose.setOnClickListener { onCloseClick(tab) }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<BrowserTab>() {
        override fun areItemsTheSame(oldItem: BrowserTab, newItem: BrowserTab) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: BrowserTab, newItem: BrowserTab) = oldItem == newItem
    }
}
