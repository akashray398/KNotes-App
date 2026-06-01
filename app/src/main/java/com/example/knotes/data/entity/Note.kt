package com.example.knotes.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList(),
    val color: Int = 0,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val deletedTimestamp: Long? = null,
    val priority: Priority = Priority.LOW,
    val reminderTime: Long? = null
)
