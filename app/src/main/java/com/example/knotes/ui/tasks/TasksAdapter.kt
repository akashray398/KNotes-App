package com.example.knotes.ui.tasks

import android.content.res.ColorStateList
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.knotes.R
import com.example.knotes.data.entity.Priority
import com.example.knotes.data.entity.Task
import com.example.knotes.databinding.ItemTaskBinding
import java.text.SimpleDateFormat
import java.util.*

class TasksAdapter(
    private val onTaskClick: (Task) -> Unit,
    private val onTaskCheckedChange: (Task) -> Unit
) : ListAdapter<Task, TasksAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            binding.apply {
                textViewTitle.text = task.title
                val deadlineText = formatDate(task.deadline)
                textViewDeadline.text = deadlineText
                textViewDeadline.visibility = if (deadlineText.isEmpty()) View.GONE else View.VISIBLE
                checkBoxCompleted.isChecked = task.isCompleted
                
                // Overdue highlighting
                val isOverdue = !task.isCompleted && task.deadline != null && task.deadline > 0 && task.deadline < System.currentTimeMillis()
                if (isOverdue) {
                    textViewDeadline.setTextColor(ContextCompat.getColor(root.context, R.color.priority_high))
                    textViewDeadline.alpha = 1.0f
                } else {
                    textViewDeadline.setTextColor(ContextCompat.getColor(root.context, R.color.outlineLight))
                    textViewDeadline.alpha = 0.7f
                }
                
                ivCalendar.visibility = if (deadlineText.isEmpty()) View.GONE else View.VISIBLE
                ivReminder.visibility = if (task.reminderTime != null) View.VISIBLE else View.GONE
                
                chipCategory.visibility = if (task.tags.isNotEmpty()) {
                    chipCategory.text = task.tags[0]
                    View.VISIBLE
                } else View.GONE

                // Strikethrough if completed
                if (task.isCompleted) {
                    textViewTitle.paintFlags = textViewTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    textViewTitle.alpha = 0.5f
                    cardViewTask.alpha = 0.6f
                    cardViewTask.strokeColor = ContextCompat.getColor(root.context, android.R.color.transparent)
                } else {
                    textViewTitle.paintFlags = textViewTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    textViewTitle.alpha = 1.0f
                    cardViewTask.alpha = 1.0f
                    cardViewTask.strokeColor = ContextCompat.getColor(root.context, R.color.card_stroke_color)
                }

                // Priority indicator color
                val priorityColor = when (task.priority) {
                    Priority.LOW -> R.color.priority_low
                    Priority.MEDIUM -> R.color.priority_medium
                    Priority.HIGH -> R.color.priority_high
                }
                priorityIndicator.setBackgroundColor(
                    ContextCompat.getColor(root.context, priorityColor)
                )

                root.setOnClickListener { onTaskClick(task) }
                checkBoxCompleted.setOnClickListener { onTaskCheckedChange(task) }
            }
        }

        private fun formatDate(timestamp: Long?): String {
            if (timestamp == null || timestamp == 0L) return ""
            val sdf = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Task, newItem: Task) = oldItem == newItem
    }
}
