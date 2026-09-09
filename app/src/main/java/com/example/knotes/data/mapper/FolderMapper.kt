package com.example.knotes.data.mapper

import com.example.knotes.data.entity.Folder as FolderEntity
import com.example.knotes.domain.model.Folder as FolderDomain

fun FolderEntity.toDomain(): FolderDomain {
    return FolderDomain(
        id = id,
        name = name,
        color = color,
        createdTime = createdTime
    )
}

fun FolderDomain.toEntity(): FolderEntity {
    return FolderEntity(
        id = id,
        name = name,
        color = color,
        createdTime = createdTime
    )
}
