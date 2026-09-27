package com.example.knotes.domain.usecase

import com.example.knotes.domain.model.Note
import com.example.knotes.domain.repository.NoteRepository

class SaveNoteUseCase(private val repository: NoteRepository) {
    suspend operator fun invoke(note: Note): Long {
        return if (note.id == 0) {
            repository.insertNote(note)
        } else {
            repository.updateNote(note)
            note.id.toLong()
        }
    }
}
