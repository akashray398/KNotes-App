package com.example.knotes.domain.model

data class NoteVersion(
    val id: Long = 0,
    val noteId: Int,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val versionName: String = ""
)
