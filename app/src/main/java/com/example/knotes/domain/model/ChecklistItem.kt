package com.example.knotes.domain.model

data class ChecklistItem(
    val id: Long = 0,
    val noteId: Int,
    val text: String,
    val isChecked: Boolean = false,
    val order: Int = 0
)
