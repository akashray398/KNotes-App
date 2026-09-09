package com.example.knotes.domain.usecase

import com.example.knotes.domain.model.Note
import com.example.knotes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow

class GetNotesUseCase(private val repository: NoteRepository) {
    operator fun invoke(query: String = ""): Flow<List<Note>> {
        return if (query.isBlank()) {
            repository.getAllNotes()
        } else {
            repository.searchNotes(query)
        }
    }
}
