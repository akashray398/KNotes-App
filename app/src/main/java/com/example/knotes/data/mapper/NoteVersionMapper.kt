package com.example.knotes.data.mapper

import com.example.knotes.data.entity.NoteVersion as NoteVersionEntity
import com.example.knotes.domain.model.NoteVersion as NoteVersionDomain

fun NoteVersionEntity.toDomain(): NoteVersionDomain {
    return NoteVersionDomain(
        id = id,
        noteId = noteId,
        title = title,
        content = content,
        timestamp = timestamp,
        versionName = versionName
    )
}

fun NoteVersionDomain.toEntity(): NoteVersionEntity {
    return NoteVersionEntity(
        id = id,
        noteId = noteId,
        title = title,
        content = content,
        timestamp = timestamp,
        versionName = versionName
    )
}
