package com.example.knotes.domain.model

data class Attachment(
    val id: Long = 0,
    val noteId: Int,
    val uri: String,
    val type: String, // e.g., "image", "audio"
    val name: String? = null
)
