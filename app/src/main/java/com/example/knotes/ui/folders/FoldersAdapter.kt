package com.example.knotes.ui.folders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.knotes.databinding.ItemFolderBinding
import com.example.knotes.domain.model.Folder

class FoldersAdapter(
    private val onEditClick: (Folder) -> Unit,
    private val onDeleteClick: (Folder) -> Unit
) : ListAdapter<Folder, FoldersAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemFolderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(folder: Folder) {
            binding.textViewFolderName.text = folder.name
            // binding.folderColorIndicator.setBackgroundTintList(ColorStateList.valueOf(folder.color))
            binding.buttonEdit.setOnClickListener { onEditClick(folder) }
            binding.buttonDelete.setOnClickListener { onDeleteClick(folder) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Folder>() {
        override fun areItemsTheSame(oldItem: Folder, newItem: Folder): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Folder, newItem: Folder): Boolean {
            return oldItem == newItem
        }
    }
}
