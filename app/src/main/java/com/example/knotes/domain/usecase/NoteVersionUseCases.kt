package com.example.knotes.domain.usecase

import com.example.knotes.domain.model.NoteVersion
import com.example.knotes.domain.repository.NoteVersionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNoteVersionsUseCase @Inject constructor(
    private val repository: NoteVersionRepository
) {
    operator fun invoke(noteId: Int): Flow<List<NoteVersion>> = repository.getVersionsForNote(noteId)
}

class SaveNoteVersionUseCase @Inject constructor(
    private val repository: NoteVersionRepository
) {
    suspend operator fun invoke(version: NoteVersion): Long = repository.saveVersion(version)
}
