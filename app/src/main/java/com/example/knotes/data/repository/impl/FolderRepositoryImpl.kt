package com.example.knotes.data.repository.impl

import com.example.knotes.data.dao.FolderDao
import com.example.knotes.data.mapper.toDomain
import com.example.knotes.data.mapper.toEntity
import com.example.knotes.domain.model.Folder
import com.example.knotes.domain.repository.FolderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderRepositoryImpl @Inject constructor(
    private val folderDao: com.example.knotes.data.dao.FolderDao,
    private val noteDao: com.example.knotes.data.dao.NoteDao
) : FolderRepository {
    override fun getAllFolders(): Flow<List<Folder>> = 
        folderDao.getAllFolders().map { list -> list.map { it.toDomain() } }

    override suspend fun getFolderById(id: Long): Folder? = 
        folderDao.getFolderById(id)?.toDomain()

    override suspend fun insertFolder(folder: Folder): Long = 
        folderDao.insertFolder(folder.toEntity())

    override suspend fun updateFolder(folder: Folder) = 
        folderDao.updateFolder(folder.toEntity())

    override suspend fun deleteFolder(folder: Folder) = 
        folderDao.deleteFolder(folder.toEntity())

    override suspend fun deleteFolderAndClearNotes(folder: Folder) {
        noteDao.clearNotesFolder(folder.id)
        folderDao.deleteFolder(folder.toEntity())
    }
}
