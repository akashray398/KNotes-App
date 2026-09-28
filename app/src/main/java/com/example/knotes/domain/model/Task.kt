package com.example.knotes.domain.model

data class Task(
    val id: Int = 0,
    val title: String = "",
    val description: String = "",
    val isCompleted: Boolean = false,
    val priority: Priority = Priority.MEDIUM,
    val deadline: Long? = null,
    val reminderTime: Long? = null,
    val secondaryReminderTime: Long? = null,
    val recurrence: Recurrence = Recurrence.NONE,
    val createdTime: Long = System.currentTimeMillis(),
    val updatedTime: Long = System.currentTimeMillis(),
    val relatedNoteId: Int? = null,
    val tags: List<String> = emptyList()
)
