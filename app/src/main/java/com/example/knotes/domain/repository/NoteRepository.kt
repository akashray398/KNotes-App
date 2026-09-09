package com.example.knotes.domain.repository

import com.example.knotes.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getAllNotes(): Flow<List<Note>>
    fun getArchivedNotes(): Flow<List<Note>>
    fun getTrashedNotes(): Flow<List<Note>>
    fun searchNotes(query: String): Flow<List<Note>>
    suspend fun getNoteById(id: Int): Note?
    suspend fun insertNote(note: Note): Long
    suspend fun updateNote(note: Note)
    suspend fun deleteNote(note: Note)
    suspend fun deleteOldTrashedNotes(threshold: Long)
    fun getArchivedCount(): Flow<Int>
    fun getTrashedCount(): Flow<Int>
    fun getNotesSince(since: Long): Flow<List<Note>>
    suspend fun moveToTrash(note: Note)
    suspend fun restoreFromTrash(note: Note)
    suspend fun archiveNote(note: Note)
    suspend fun unarchiveNote(note: Note)
}
