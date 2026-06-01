package com.example.knotes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val deadline: Long,
    val priority: Priority = Priority.MEDIUM,
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
