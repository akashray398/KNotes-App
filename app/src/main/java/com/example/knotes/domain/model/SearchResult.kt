package com.example.knotes.domain.model

sealed class SearchResult {
    data class NoteResult(val note: Note) : SearchResult()
    data class TaskResult(val task: Task) : SearchResult()
}
