package com.example.knotes.domain.usecase

import com.example.knotes.domain.repository.SearchHistoryRepository
import javax.inject.Inject

class SaveSearchHistoryUseCase @Inject constructor(
    private val repository: SearchHistoryRepository
) {
    suspend operator fun invoke(query: String) {
        if (query.isNotBlank()) {
            repository.insertSearch(query)
        }
    }
}
