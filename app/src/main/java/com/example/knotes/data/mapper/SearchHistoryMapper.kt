package com.example.knotes.data.mapper

import com.example.knotes.data.entity.SearchHistory as Entity
import com.example.knotes.domain.model.SearchHistory as Model

fun Entity.toDomain() = Model(
    query = query,
    timestamp = timestamp
)

fun Model.toEntity() = Entity(
    query = query,
    timestamp = timestamp
)
