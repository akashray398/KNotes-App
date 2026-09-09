package com.example.knotes.data.repository

import android.content.Context
import android.content.Intent
import com.example.knotes.data.dao.NoteDao
import com.example.knotes.data.entity.Note
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    @ApplicationContext private val context: Context
) {
    fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()

    fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query)

    suspend fun insertNote(note: Note): Long {
        val id = noteDao.insertNote(note.copy(updatedTime = System.currentTimeMillis(), isSynced = false))
        updateWidget()
        return id
    }

    suspend fun updateNote(note: Note) {
        noteDao.updateNote(note.copy(updatedTime = System.currentTimeMillis(), isSynced = false))
        updateWidget()
    }

    private fun updateWidget() {
        val intent = Intent(context, com.example.knotes.widget.NoteWidgetProvider::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val ids = android.appwidget.AppWidgetManager.getInstance(context)
            .getAppWidgetIds(android.content.ComponentName(context, com.example.knotes.widget.NoteWidgetProvider::class.java))
        intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        context.sendBroadcast(intent)
    }

    suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note)
        updateWidget()
    }

    suspend fun getNoteById(id: Int): Note? = noteDao.getNoteById(id)
    
    fun getNotesSince(since: Long): Flow<List<Note>> = noteDao.getNotesSince(since)

    fun getArchivedNotes(): Flow<List<Note>> = noteDao.getArchivedNotes()

    fun getTrashedNotes(): Flow<List<Note>> = noteDao.getTrashedNotes()

    fun getArchivedCount(): Flow<Int> = noteDao.getArchivedCount()

    fun getTrashedCount(): Flow<Int> = noteDao.getTrashedCount()

    suspend fun moveToTrash(note: Note) {
        noteDao.updateNote(note.copy(
            isTrashed = true, 
            deletedTimestamp = System.currentTimeMillis(),
            updatedTime = System.currentTimeMillis(),
            isSynced = false
        ))
    }

    suspend fun restoreFromTrash(note: Note) {
        noteDao.updateNote(note.copy(
            isTrashed = false, 
            deletedTimestamp = null,
            updatedTime = System.currentTimeMillis(),
            isSynced = false
        ))
    }

    suspend fun archiveNote(note: Note) {
        noteDao.updateNote(note.copy(
            isArchived = true,
            updatedTime = System.currentTimeMillis(),
            isSynced = false
        ))
    }

    suspend fun unarchiveNote(note: Note) {
        noteDao.updateNote(note.copy(
            isArchived = false,
            updatedTime = System.currentTimeMillis(),
            isSynced = false
        ))
    }

    suspend fun deleteOldTrashedNotes(threshold: Long) {
        noteDao.deleteOldTrashedNotes(threshold)
    }
}
