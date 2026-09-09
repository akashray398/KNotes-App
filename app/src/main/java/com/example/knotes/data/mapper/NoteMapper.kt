package com.example.knotes.data.mapper

import com.example.knotes.data.entity.Note as NoteEntity
import com.example.knotes.domain.model.Note as NoteDomain
import com.example.knotes.domain.model.Priority as PriorityDomain
import com.example.knotes.data.entity.Priority as PriorityEntity

fun NoteEntity.toDomain(): NoteDomain {
    return NoteDomain(
        id = id,
        title = title,
        content = description,
        createdTime = createdTime,
        updatedTime = updatedTime,
        isPinned = isPinned,
        isFavorite = isFavorite,
        isArchived = isArchived,
        isTrashed = isTrashed,
        color = color,
        folderId = folderId,
        reminderTime = reminderTime,
        isSynced = isSynced,
        tags = tags,
        priority = PriorityDomain.valueOf(priority.name)
    )
}

fun NoteDomain.toEntity(): NoteEntity {
    return NoteEntity(
        id = id,
        title = title,
        description = content,
        createdTime = createdTime,
        updatedTime = updatedTime,
        isPinned = isPinned,
        isFavorite = isFavorite,
        isArchived = isArchived,
        isTrashed = isTrashed,
        folderId = folderId,
        color = color,
        reminderTime = reminderTime,
        isSynced = isSynced,
        tags = tags,
        priority = PriorityEntity.valueOf(priority.name)
    )
}
