package com.example.knotes.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String = "",
    val description: String = "",
    val deadline: Long? = null,
    val reminderTime: Long? = null,
    val secondaryReminderTime: Long? = null,
    val isCompleted: Boolean = false,
    val priority: Priority = Priority.MEDIUM,
    val recurrence: Recurrence = Recurrence.NONE,
    val tags: List<String> = emptyList(),
    val color: Int = 0,
    @ColumnInfo(name = "lastModified")
    val updatedTime: Long = System.currentTimeMillis(),
    val createdTime: Long = System.currentTimeMillis(),
    val relatedNoteId: Int? = null,
    val remoteId: String? = null,
    val isSynced: Boolean = false
)
