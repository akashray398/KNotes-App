package com.example.knotes.ui.notes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.knotes.databinding.ItemNoteVersionBinding
import com.example.knotes.domain.model.NoteVersion
import java.text.SimpleDateFormat
import java.util.*

class VersionHistoryAdapter(
    private val onRestoreClick: (NoteVersion) -> Unit
) : ListAdapter<NoteVersion, VersionHistoryAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemNoteVersionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemNoteVersionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(version: NoteVersion) {
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            binding.tvTimestamp.text = sdf.format(Date(version.timestamp))
            binding.tvTitle.text = version.title
            binding.tvVersionName.text = version.versionName
            
            binding.btnRestore.setOnClickListener { onRestoreClick(version) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<NoteVersion>() {
        override fun areItemsTheSame(oldItem: NoteVersion, newItem: NoteVersion) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: NoteVersion, newItem: NoteVersion) = oldItem == newItem
    }
}
