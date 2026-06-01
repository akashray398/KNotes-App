package com.example.knotes.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.knotes.data.entity.Note
import com.example.knotes.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NoteRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag = _selectedTag.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.NEWEST)
    val sortOrder = _sortOrder.asStateFlow()

    enum class SortOrder { NEWEST, OLDEST, ALPHABETICAL, PRIORITY, LAST_MODIFIED }

    private val _filterFavorite = MutableStateFlow(false)
    val filterFavorite = _filterFavorite.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes = combine(_searchQuery, _selectedTag, _sortOrder, _filterFavorite) { query, tag, sort, fav ->
        Quadruple(query, tag, sort, fav)
    }.flatMapLatest { (query, tag, sort, fav) ->
        repository.searchNotes(query).map { list ->
            var filteredList = list
            if (tag != null) {
                filteredList = filteredList.filter { it.tags.contains(tag) }
            }
            if (fav) {
                filteredList = filteredList.filter { it.isFavorite }
            }

            when (sort) {
                SortOrder.NEWEST -> filteredList.sortedByDescending { it.timestamp }
                SortOrder.OLDEST -> filteredList.sortedBy { it.timestamp }
                SortOrder.ALPHABETICAL -> filteredList.sortedBy { it.title.lowercase() }
                SortOrder.PRIORITY -> filteredList.sortedByDescending { it.priority.ordinal }
                SortOrder.LAST_MODIFIED -> filteredList.sortedByDescending { it.timestamp }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val archivedNotes = repository.getArchivedNotes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val trashedNotes = repository.getTrashedNotes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalNotesCount = repository.getAllNotes().map { it.size }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    // Dashboard Statistics
    val weeklyStats = repository.getNotesSince(getStartOfWeek()).map { weeklyNotes ->
        val count = weeklyNotes.size
        val topTag = weeklyNotes.flatMap { it.tags }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }?.key ?: "None"
        
        // Mock productivity score: (notes created / 10) * 100, max 100%
        val score = minOf((count.toFloat() / 10f) * 100f, 100f).toInt()
        
        Triple(count, topTag, score)
    }.stateIn(viewModelScope, SharingStarted.Lazily, Triple(0, "None", 0))

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectTag(tag: String?) {
        _selectedTag.value = tag
    }

    fun updateSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun togglePin(note: Note) = viewModelScope.launch {
        repository.updateNote(note.copy(isPinned = !note.isPinned))
    }

    fun toggleFavorite(note: Note) = viewModelScope.launch {
        repository.updateNote(note.copy(isFavorite = !note.isFavorite))
    }

    fun archiveNote(note: Note) = viewModelScope.launch {
        repository.archiveNote(note)
    }

    fun unarchiveNote(note: Note) = viewModelScope.launch {
        repository.unarchiveNote(note)
    }

    fun moveToTrash(note: Note) = viewModelScope.launch {
        repository.moveToTrash(note)
    }

    fun restoreFromTrash(note: Note) = viewModelScope.launch {
        repository.restoreFromTrash(note)
    }

    fun deletePermanently(note: Note) = viewModelScope.launch {
        repository.deleteNote(note)
    }

    fun setFilterFavorite(favorite: Boolean) {
        _filterFavorite.value = favorite
    }

    data class Quadruple<out A, out B, out C, out D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )

    suspend fun insertNote(note: Note): Long {
        return repository.insertNote(note)
    }

    suspend fun updateNote(note: Note) {
        repository.updateNote(note)
    }

    fun deleteNote(note: Note) = viewModelScope.launch {
        repository.deleteNote(note)
    }

    suspend fun getNoteById(id: Int): Note? {
        return repository.getNoteById(id)
    }

    private fun getStartOfWeek(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
