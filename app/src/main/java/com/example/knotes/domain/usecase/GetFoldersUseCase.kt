package com.example.knotes.domain.usecase

import com.example.knotes.domain.model.Folder
import com.example.knotes.domain.repository.FolderRepository
import kotlinx.coroutines.flow.Flow

class GetFoldersUseCase(private val repository: FolderRepository) {
    operator fun invoke(): Flow<List<Folder>> {
        return repository.getAllFolders()
    }
}
