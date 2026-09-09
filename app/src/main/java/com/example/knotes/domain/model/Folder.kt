package com.example.knotes.domain.model

data class Folder(
    val id: Long = 0,
    val name: String,
    val color: Int = 0,
    val createdTime: Long = System.currentTimeMillis()
)
