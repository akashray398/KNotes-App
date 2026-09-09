package com.example.knotes.domain.model

data class Note(
    val id: Int = 0,
    val title: String = "",
    val content: String = "",
    val createdTime: Long = System.currentTimeMillis(),
    val updatedTime: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val color: Int = 0,
    val folderId: Long? = null,
    val tags: List<String> = emptyList(),
    val priority: Priority = Priority.LOW,
    val reminderTime: Long? = null,
    val isSynced: Boolean = false,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val attachments: List<Attachment> = emptyList()
)
