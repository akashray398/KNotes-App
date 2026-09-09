package com.example.knotes.domain.usecase

import com.example.knotes.domain.model.Note
import com.example.knotes.domain.repository.NoteRepository

class DeleteNoteUseCase(private val repository: NoteRepository) {
    suspend operator fun invoke(note: Note) {
        repository.deleteNote(note)
    }
}
