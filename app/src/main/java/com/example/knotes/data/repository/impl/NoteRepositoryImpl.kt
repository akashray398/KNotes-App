package com.example.knotes.data.repository.impl

import android.content.Context
import android.content.Intent
import com.example.knotes.data.dao.NoteDao
import com.example.knotes.data.mapper.toDomain
import com.example.knotes.data.mapper.toEntity
import com.example.knotes.domain.model.Note
import com.example.knotes.domain.repository.NoteRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    @ApplicationContext private val context: Context
) : NoteRepository {
    override fun getAllNotes(): Flow<List<Note>> = 
        noteDao.getAllNotes().map { list -> list.map { it.toDomain() } }

    override fun searchNotes(query: String): Flow<List<Note>> = 
        noteDao.searchNotes(query).map { list -> list.map { it.toDomain() } }

    override fun getArchivedNotes(): Flow<List<Note>> = 
        noteDao.getArchivedNotes().map { list -> list.map { it.toDomain() } }

    override fun getTrashedNotes(): Flow<List<Note>> = 
        noteDao.getTrashedNotes().map { list -> list.map { it.toDomain() } }

    override suspend fun getNoteById(id: Int): Note? = 
        noteDao.getNoteById(id)?.toDomain()

    override suspend fun insertNote(note: Note): Long {
        val id = noteDao.insertNote(note.toEntity().copy(isSynced = false))
        updateWidget()
        return id
    }

    override suspend fun updateNote(note: Note) {
        noteDao.updateNote(note.toEntity().copy(isSynced = false))
        updateWidget()
    }

    override suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note.toEntity())
        updateWidget()
    }

    override suspend fun deleteOldTrashedNotes(threshold: Long) {
        noteDao.deleteOldTrashedNotes(threshold)
    }

    override fun getArchivedCount(): Flow<Int> = noteDao.getArchivedCount()
    override fun getTrashedCount(): Flow<Int> = noteDao.getTrashedCount()
    override fun getNotesSince(since: Long): Flow<List<Note>> = 
        noteDao.getNotesSince(since).map { list -> list.map { it.toDomain() } }

    override suspend fun moveToTrash(note: Note) {
        noteDao.updateNote(note.toEntity().copy(
            isTrashed = true,
            deletedTimestamp = System.currentTimeMillis(),
            isSynced = false
        ))
        updateWidget()
    }

    override suspend fun restoreFromTrash(note: Note) {
        noteDao.updateNote(note.toEntity().copy(
            isTrashed = false,
            deletedTimestamp = null,
            isSynced = false
        ))
        updateWidget()
    }

    override suspend fun archiveNote(note: Note) {
        noteDao.updateNote(note.toEntity().copy(
            isArchived = true,
            isSynced = false
        ))
        updateWidget()
    }

    override suspend fun unarchiveNote(note: Note) {
        noteDao.updateNote(note.toEntity().copy(
            isArchived = false,
            isSynced = false
        ))
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
}
