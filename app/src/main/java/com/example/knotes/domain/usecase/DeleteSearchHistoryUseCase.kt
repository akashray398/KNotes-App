package com.example.knotes.domain.usecase

import com.example.knotes.domain.repository.SearchHistoryRepository
import javax.inject.Inject

class DeleteSearchHistoryUseCase @Inject constructor(
    private val repository: SearchHistoryRepository
) {
    suspend fun delete(query: String) = repository.deleteSearch(query)
    suspend fun clearAll() = repository.clearHistory()
}
