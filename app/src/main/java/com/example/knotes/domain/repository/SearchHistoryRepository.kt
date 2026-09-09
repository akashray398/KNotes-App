package com.example.knotes.domain.repository

import com.example.knotes.domain.model.SearchHistory
import kotlinx.coroutines.flow.Flow

interface SearchHistoryRepository {
    fun getRecentSearches(): Flow<List<SearchHistory>>
    suspend fun insertSearch(query: String)
    suspend fun deleteSearch(query: String)
    suspend fun clearHistory()
}
