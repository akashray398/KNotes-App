package com.example.knotes.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.knotes.data.entity.Priority
import com.example.knotes.databinding.ItemNoteBinding
import com.example.knotes.databinding.ItemTaskBinding
import com.example.knotes.domain.model.SearchResult
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.*

class SearchAdapter(
    private val onNoteClick: (Int) -> Unit,
    private val onTaskClick: (Int) -> Unit
) : ListAdapter<SearchResult, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val TYPE_NOTE = 0
        private const val TYPE_TASK = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SearchResult.NoteResult -> TYPE_NOTE
            is SearchResult.TaskResult -> TYPE_TASK
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_NOTE -> {
                val binding = ItemNoteBinding.inflate(layoutInflater, parent, false)
                NoteViewHolder(binding)
            }
            else -> {
                val binding = ItemTaskBinding.inflate(layoutInflater, parent, false)
                TaskViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is NoteViewHolder -> holder.bind((item as SearchResult.NoteResult).note)
            is TaskViewHolder -> holder.bind((item as SearchResult.TaskResult).task)
        }
    }

    inner class NoteViewHolder(private val binding: ItemNoteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(note: com.example.knotes.domain.model.Note) {
            binding.textViewTitle.text = note.title
            binding.textViewDescription.text = note.content
            binding.textViewTimestamp.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(note.updatedTime))
            
            binding.priorityIndicator.setBackgroundColor(
                binding.root.context.getColor(
                    when (note.priority) {
                        com.example.knotes.domain.model.Priority.HIGH -> com.example.knotes.R.color.priority_high
                        com.example.knotes.domain.model.Priority.MEDIUM -> com.example.knotes.R.color.priority_medium
                        com.example.knotes.domain.model.Priority.LOW -> com.example.knotes.R.color.priority_low
                    }
                )
            )

            binding.chipGroupTags.removeAllViews()
            note.tags.forEach { tag ->
                val chip = Chip(binding.root.context)
                chip.text = tag
                binding.chipGroupTags.addView(chip)
            }

            binding.root.setOnClickListener { onNoteClick(note.id) }
        }
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: com.example.knotes.domain.model.Task) {
            binding.textViewTitle.text = task.title
            binding.checkBoxCompleted.isChecked = task.isCompleted
            
            val deadlineText = task.deadline?.let {
                SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(it))
            } ?: ""
            binding.textViewDeadline.text = deadlineText
            binding.layoutMeta.visibility = if (deadlineText.isEmpty()) View.GONE else View.VISIBLE

            binding.priorityIndicator.setBackgroundColor(
                binding.root.context.getColor(
                    when (task.priority) {
                        com.example.knotes.domain.model.Priority.HIGH -> com.example.knotes.R.color.priority_high
                        com.example.knotes.domain.model.Priority.MEDIUM -> com.example.knotes.R.color.priority_medium
                        com.example.knotes.domain.model.Priority.LOW -> com.example.knotes.R.color.priority_low
                    }
                )
            )

            binding.root.setOnClickListener { onTaskClick(task.id) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SearchResult>() {
        override fun areItemsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
            return when {
                oldItem is SearchResult.NoteResult && newItem is SearchResult.NoteResult -> 
                    oldItem.note.id == newItem.note.id
                oldItem is SearchResult.TaskResult && newItem is SearchResult.TaskResult -> 
                    oldItem.task.id == newItem.task.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
            return oldItem == newItem
        }
    }
}
