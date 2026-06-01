package com.example.knotes.data.repository

import com.example.knotes.data.dao.NoteDao
import com.example.knotes.data.entity.Note
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao
) {
    fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()

    fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query)

    suspend fun insertNote(note: Note): Long = noteDao.insertNote(note)

    suspend fun updateNote(note: Note) = noteDao.updateNote(note)

    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    suspend fun getNoteById(id: Int): Note? = noteDao.getNoteById(id)
    
    fun getNotesSince(since: Long): Flow<List<Note>> = noteDao.getNotesSince(since)

    fun getArchivedNotes(): Flow<List<Note>> = noteDao.getArchivedNotes()

    fun getTrashedNotes(): Flow<List<Note>> = noteDao.getTrashedNotes()

    suspend fun moveToTrash(note: Note) {
        noteDao.updateNote(note.copy(isTrashed = true, deletedTimestamp = System.currentTimeMillis()))
    }

    suspend fun restoreFromTrash(note: Note) {
        noteDao.updateNote(note.copy(isTrashed = false, deletedTimestamp = null))
    }

    suspend fun archiveNote(note: Note) {
        noteDao.updateNote(note.copy(isArchived = true))
    }

    suspend fun unarchiveNote(note: Note) {
        noteDao.updateNote(note.copy(isArchived = false))
    }

    suspend fun deleteOldTrashedNotes(threshold: Long) {
        noteDao.deleteOldTrashedNotes(threshold)
    }
}
