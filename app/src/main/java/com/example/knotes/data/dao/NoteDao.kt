package com.example.knotes.data.dao

import androidx.room.*
import com.example.knotes.data.entity.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isArchived = 0 AND isTrashed = 0 ORDER BY isPinned DESC, timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("""
        SELECT * FROM notes 
        WHERE (title LIKE '%' || :searchQuery || '%' 
        OR description LIKE '%' || :searchQuery || '%' 
        OR tags LIKE '%' || :searchQuery || '%')
        AND isArchived = 0 AND isTrashed = 0
        ORDER BY isPinned DESC, timestamp DESC
    """)
    fun searchNotes(searchQuery: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 AND isTrashed = 0 ORDER BY timestamp DESC")
    fun getArchivedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isTrashed = 1 ORDER BY deletedTimestamp DESC")
    fun getTrashedNotes(): Flow<List<Note>>

    @Query("DELETE FROM notes WHERE isTrashed = 1 AND deletedTimestamp <= :threshold")
    suspend fun deleteOldTrashedNotes(threshold: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Int): Note?
    
    @Query("SELECT * FROM notes WHERE timestamp >= :since")
    fun getNotesSince(since: Long): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isPinned = 1 ORDER BY timestamp DESC")
    suspend fun getPinnedNotesSync(): List<Note>
}
