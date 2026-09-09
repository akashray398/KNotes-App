package com.example.knotes.domain.usecase

import com.example.knotes.domain.model.SearchHistory
import com.example.knotes.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSearchHistoryUseCase @Inject constructor(
    private val repository: SearchHistoryRepository
) {
    operator fun invoke(): Flow<List<SearchHistory>> = repository.getRecentSearches()
}
