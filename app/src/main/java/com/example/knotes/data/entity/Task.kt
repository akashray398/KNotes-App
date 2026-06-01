package com.example.knotes.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String = "",
    val deadline: Long? = null,
    val reminderTime: Long? = null,
    val isCompleted: Boolean = false,
    val priority: Priority = Priority.MEDIUM,
    val tags: List<String> = emptyList(),
    val color: Int = 0
)
