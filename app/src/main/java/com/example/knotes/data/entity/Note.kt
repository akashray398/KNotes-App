package com.example.knotes.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String = "",
    val description: String = "",
    @ColumnInfo(name = "timestamp")
    val createdTime: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "lastModified")
    val updatedTime: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val deletedTimestamp: Long? = null,
    val priority: Priority = Priority.LOW,
    val reminderTime: Long? = null,
    val remoteId: String? = null,
    val isSynced: Boolean = false,
    val folderId: Long? = null,
    val color: Int = 0,
    val tags: List<String> = emptyList() // Keep for compatibility or simple tag storage
)
