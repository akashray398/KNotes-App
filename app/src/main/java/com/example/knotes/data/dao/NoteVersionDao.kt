package com.example.knotes.data.dao

import androidx.room.*
import com.example.knotes.data.entity.NoteVersion
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteVersionDao {
    @Query("SELECT * FROM note_versions WHERE noteId = :noteId ORDER BY timestamp DESC")
    fun getVersionsForNote(noteId: Int): Flow<List<NoteVersion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: NoteVersion): Long

    @Delete
    suspend fun deleteVersion(version: NoteVersion)

    @Query("DELETE FROM note_versions WHERE noteId = :noteId")
    suspend fun deleteAllVersionsForNote(noteId: Int)
}
