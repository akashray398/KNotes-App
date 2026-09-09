package com.example.knotes.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.knotes.domain.model.SearchHistory
import com.example.knotes.domain.model.SearchResult
import com.example.knotes.domain.usecase.DeleteSearchHistoryUseCase
import com.example.knotes.domain.usecase.GetSearchHistoryUseCase
import com.example.knotes.domain.usecase.SaveSearchHistoryUseCase
import com.example.knotes.domain.usecase.UnifiedSearchUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val unifiedSearchUseCase: UnifiedSearchUseCase,
    private val getSearchHistoryUseCase: GetSearchHistoryUseCase,
    private val saveSearchHistoryUseCase: SaveSearchHistoryUseCase,
    private val deleteSearchHistoryUseCase: DeleteSearchHistoryUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<SearchResult>> = _searchQuery
        .debounce(300L)
        .flatMapLatest { query ->
            unifiedSearchUseCase(query)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val searchHistory: StateFlow<List<SearchHistory>> = getSearchHistoryUseCase()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveSearch(query: String) {
        viewModelScope.launch {
            saveSearchHistoryUseCase(query)
        }
    }

    fun deleteHistoryItem(query: String) {
        viewModelScope.launch {
            deleteSearchHistoryUseCase.delete(query)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            deleteSearchHistoryUseCase.clearAll()
        }
    }
}
