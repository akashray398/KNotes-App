package com.example.knotes.data.mapper

import com.example.knotes.data.entity.Task as TaskEntity
import com.example.knotes.domain.model.Task as TaskDomain
import com.example.knotes.data.entity.Priority as PriorityEntity
import com.example.knotes.domain.model.Priority as PriorityDomain
import com.example.knotes.data.entity.Recurrence as RecurrenceEntity
import com.example.knotes.domain.model.Recurrence as RecurrenceDomain

fun TaskEntity.toDomain(): TaskDomain {
    return TaskDomain(
        id = id,
        title = title,
        description = description,
        isCompleted = isCompleted,
        priority = PriorityDomain.valueOf(priority.name),
        deadline = deadline,
        reminderTime = reminderTime,
        secondaryReminderTime = secondaryReminderTime,
        recurrence = RecurrenceDomain.valueOf(recurrence.name),
        recurrenceEndDate = recurrenceEndDate,
        createdTime = createdTime,
        updatedTime = updatedTime,
        relatedNoteId = relatedNoteId,
        tags = tags
    )
}

fun TaskDomain.toEntity(): TaskEntity {
    return TaskEntity(
        id = id,
        title = title,
        description = description,
        isCompleted = isCompleted,
        priority = PriorityEntity.valueOf(priority.name),
        deadline = deadline,
        reminderTime = reminderTime,
        secondaryReminderTime = secondaryReminderTime,
        recurrence = RecurrenceEntity.valueOf(recurrence.name),
        recurrenceEndDate = recurrenceEndDate,
        createdTime = createdTime,
        updatedTime = updatedTime,
        relatedNoteId = relatedNoteId,
        tags = tags
    )
}
