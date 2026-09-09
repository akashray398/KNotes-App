package com.example.knotes.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checklist_items")
data class ChecklistItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val noteId: Int,
    val text: String,
    val isChecked: Boolean = false,
    val order: Int = 0
)
