package com.example.knotes.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attachments")
data class Attachment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val noteId: Int,
    val uri: String,
    val type: String,
    val name: String? = null
)
