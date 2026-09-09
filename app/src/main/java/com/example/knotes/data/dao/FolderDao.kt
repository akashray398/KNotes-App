package com.example.knotes.data.dao

import androidx.room.*
import com.example.knotes.data.entity.Folder
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getFolderById(id: Long): Folder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder): Long

    @Update
    suspend fun updateFolder(folder: Folder)

    @Delete
    suspend fun deleteFolder(folder: Folder)

    @Query("SELECT * FROM folders WHERE remoteId = :remoteId")
    suspend fun getFolderByRemoteId(remoteId: String): Folder?

    @Query("SELECT * FROM folders")
    suspend fun getAllFoldersSync(): List<Folder>

    @Query("DELETE FROM folders")
    suspend fun deleteAll()
}
