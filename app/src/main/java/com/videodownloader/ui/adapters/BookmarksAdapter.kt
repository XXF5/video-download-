package com.videodownloader.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.videodownloader.data.models.BookmarkItem
import com.videodownloader.databinding.ItemBookmarkBinding

class BookmarksAdapter(
    private val onItemClick: (BookmarkItem) -> Unit,
    private val onDeleteClick: (BookmarkItem) -> Unit
) : ListAdapter<BookmarkItem, BookmarksAdapter.BookmarkViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
        val binding = ItemBookmarkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookmarkViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BookmarkViewHolder(private val binding: ItemBookmarkBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: BookmarkItem) {
            binding.apply {
                tvTitle.text = item.title.ifEmpty { item.url }
                tvUrl.text = item.url
                root.setOnClickListener { onItemClick(item) }
                btnDelete.setOnClickListener { onDeleteClick(item) }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<BookmarkItem>() {
        override fun areItemsTheSame(oldItem: BookmarkItem, newItem: BookmarkItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: BookmarkItem, newItem: BookmarkItem) = oldItem == newItem
    }
}
