package com.example.knotes.data.repository.impl

import com.example.knotes.data.dao.NoteVersionDao
import com.example.knotes.data.mapper.toDomain
import com.example.knotes.data.mapper.toEntity
import com.example.knotes.domain.model.NoteVersion
import com.example.knotes.domain.repository.NoteVersionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteVersionRepositoryImpl @Inject constructor(
    private val dao: NoteVersionDao
) : NoteVersionRepository {
    override fun getVersionsForNote(noteId: Int): Flow<List<NoteVersion>> {
        return dao.getVersionsForNote(noteId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveVersion(version: NoteVersion): Long {
        return dao.insertVersion(version.toEntity())
    }

    override suspend fun deleteVersion(version: NoteVersion) {
        dao.deleteVersion(version.toEntity())
    }
}
