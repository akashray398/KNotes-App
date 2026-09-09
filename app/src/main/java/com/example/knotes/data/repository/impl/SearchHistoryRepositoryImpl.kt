package com.example.knotes.data.repository.impl

import com.example.knotes.data.dao.SearchHistoryDao
import com.example.knotes.data.entity.SearchHistory
import com.example.knotes.data.mapper.toDomain
import com.example.knotes.domain.model.SearchHistory as Model
import com.example.knotes.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchHistoryRepositoryImpl @Inject constructor(
    private val searchHistoryDao: SearchHistoryDao
) : SearchHistoryRepository {
    override fun getRecentSearches(): Flow<List<Model>> =
        searchHistoryDao.getRecentSearches().map { list -> list.map { it.toDomain() } }

    override suspend fun insertSearch(query: String) {
        searchHistoryDao.insertSearch(SearchHistory(query))
    }

    override suspend fun deleteSearch(query: String) {
        searchHistoryDao.deleteSearch(query)
    }

    override suspend fun clearHistory() {
        searchHistoryDao.clearHistory()
    }
}
