package com.example.knotes.ui.notes

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.knotes.R
import com.example.knotes.data.entity.Note
import com.example.knotes.data.entity.Priority
import com.example.knotes.databinding.ItemNoteBinding
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.*

class NotesAdapter(
    private val onNoteClick: (Note) -> Unit,
    private val onPinClick: (Note) -> Unit,
    private val onFavoriteClick: ((Note) -> Unit)? = null
) : ListAdapter<Note, NotesAdapter.NoteViewHolder>(NoteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note) {
            binding.apply {
                textViewTitle.text = note.title
                textViewDescription.text = note.description
                textViewTimestamp.text = "Edited ${formatDate(note.timestamp)}"
                
                imageViewPin.visibility = if (note.isPinned) View.VISIBLE else View.GONE
                ivFavorite.visibility = if (note.isFavorite) View.VISIBLE else View.GONE
                ivFavorite.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(root.context, R.color.priority_medium))
                
                // Priority Indicator Color
                val priorityColor = when (note.priority) {
                    Priority.HIGH -> ContextCompat.getColor(root.context, R.color.priority_high)
                    Priority.MEDIUM -> ContextCompat.getColor(root.context, R.color.priority_medium)
                    Priority.LOW -> ContextCompat.getColor(root.context, R.color.priority_low)
                }
                priorityIndicator.setBackgroundColor(priorityColor)

                // Render Tags
                chipGroupTags.removeAllViews()
                note.tags.take(3).forEach { tag ->
                    val chip = Chip(root.context).apply {
                        text = tag
                        isClickable = false
                        isCheckable = false
                        setEnsureMinTouchTargetSize(false)
                        setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelSmall)
                        val primaryColor = ContextCompat.getColor(context, R.color.purple_6750A4)
                        val backgroundColor = ColorUtils.setAlphaComponent(primaryColor, 20)
                        chipBackgroundColor = ColorStateList.valueOf(backgroundColor)
                        chipStrokeWidth = 0f
                        setTextColor(primaryColor)
                        shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                            .setAllCornerSizes(12f)
                            .build()
                    }
                    chipGroupTags.addView(chip)
                }

                root.setOnClickListener { onNoteClick(note) }
                imageViewPin.setOnClickListener { onPinClick(note) }
                ivFavorite.setOnClickListener { onFavoriteClick?.invoke(note) }
            }
        }

        private fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Note, newItem: Note) = oldItem == newItem
    }
}
