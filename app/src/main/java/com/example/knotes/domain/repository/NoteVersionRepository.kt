package com.example.knotes.domain.repository

import com.example.knotes.domain.model.NoteVersion
import kotlinx.coroutines.flow.Flow

interface NoteVersionRepository {
    fun getVersionsForNote(noteId: Int): Flow<List<NoteVersion>>
    suspend fun saveVersion(version: NoteVersion): Long
    suspend fun deleteVersion(version: NoteVersion)
}
