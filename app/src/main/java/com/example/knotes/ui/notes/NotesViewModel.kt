package com.example.knotes.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.knotes.domain.model.Note
import com.example.knotes.domain.repository.NoteRepository
import com.example.knotes.domain.repository.TaskRepository as DomainTaskRepository
import com.example.knotes.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NoteRepository,
    private val taskRepository: DomainTaskRepository,
    private val getNotesUseCase: GetNotesUseCase,
    private val saveNoteUseCase: SaveNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val getFoldersUseCase: GetFoldersUseCase,
    private val settingsManager: com.example.knotes.util.SettingsManager,
    private val streakManager: com.example.knotes.util.StreakManager
) : ViewModel() {

    private val _streakEvent = MutableSharedFlow<Int>()
    val streakEvent = _streakEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            streakManager.validateStreak()
            
            // Watch for streak increases to trigger celebration
            var lastStreak = settingsManager.currentStreak.first()
            settingsManager.currentStreak.collect { current ->
                if (current > lastStreak) {
                    _streakEvent.emit(current)
                }
                lastStreak = current
            }
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag = _selectedTag.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.NEWEST)
    val sortOrder = _sortOrder.asStateFlow()

    enum class SortOrder { NEWEST, OLDEST, ALPHABETICAL, PRIORITY, LAST_MODIFIED }

    private val _filterFavorite = MutableStateFlow(false)
    val filterFavorite = _filterFavorite.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<Long?>(null)
    val selectedFolderId = _selectedFolderId.asStateFlow()

    val folders = getFoldersUseCase().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val isGridView = settingsManager.isGridView
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes = combine(_searchQuery, _selectedTag, _sortOrder, _filterFavorite, _selectedFolderId) { query, tag, sort, fav, folderId ->
        Filters(query, tag, sort, fav, folderId)
    }.flatMapLatest { filters ->
        repository.searchNotes(filters.query).map { list ->
            var filteredList = list
            if (filters.tag != null) {
                filteredList = filteredList.filter { it.tags.contains(filters.tag) }
            }
            if (filters.fav) {
                filteredList = filteredList.filter { it.isFavorite }
            }
            if (filters.folderId != null) {
                filteredList = filteredList.filter { it.folderId == filters.folderId }
            }

            when (filters.sort) {
                SortOrder.NEWEST -> filteredList.sortedByDescending { it.createdTime }
                SortOrder.OLDEST -> filteredList.sortedBy { it.createdTime }
                SortOrder.ALPHABETICAL -> filteredList.sortedBy { it.title.lowercase() }
                SortOrder.PRIORITY -> filteredList.sortedByDescending { it.priority.ordinal }
                SortOrder.LAST_MODIFIED -> filteredList.sortedByDescending { it.updatedTime }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    data class Filters(
        val query: String,
        val tag: String?,
        val sort: SortOrder,
        val fav: Boolean,
        val folderId: Long?
    )

    val archivedNotes = repository.getArchivedNotes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val trashedNotes = repository.getTrashedNotes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalNotesCount = repository.getAllNotes().map { it.size }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val tagUsageCounts = repository.getAllNotes().map { notes ->
        notes.flatMap { it.tags }
            .groupingBy { it }
            .eachCount()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    private fun calculateStreak(notes: List<Note>): Int {
        if (notes.isEmpty()) return 0
        val dates = notes.map {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.createdTime
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.distinct().sortedDescending()

        var streakCount = 0
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        var checkDate = cal.timeInMillis

        // If no note today, check if there was one yesterday to continue streak
        if (!dates.contains(checkDate)) {
            checkDate -= 24 * 60 * 60 * 1000
        }

        for (date in dates) {
            if (date == checkDate) {
                streakCount++
                checkDate -= 24 * 60 * 60 * 1000
            } else if (date < checkDate) {
                break
            }
        }
        return streakCount
    }

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

    val dashboardState = combine(
        totalNotesCount,
        taskRepository.getPendingTasksCount(),
        taskRepository.getCompletedTodayCount(getStartOfDay()),
        taskRepository.getDueTodayCount(getStartOfDay(), getEndOfDay()),
        taskRepository.getOverdueCount(System.currentTimeMillis()),
        settingsManager.currentStreak,
        settingsManager.highestStreak,
        repository.getArchivedCount(),
        repository.getTrashedCount(),
        taskRepository.getCompletedTasksSince(getStartOfWeek()),
        taskRepository.getPendingTasksSince(getStartOfWeek())
    ) { args ->
        val totalNotes = args[0] as Int
        val pending = args[1] as Int
        val completedToday = args[2] as Int
        val dueToday = args[3] as Int
        val overdue = args[4] as Int
        val streakCount = args[5] as Int
        val highestStreak = args[6] as Int
        val archivedCount = args[7] as Int
        val trashedCount = args[8] as Int
        val completedWeek = args[9] as Int
        val pendingWeek = args[10] as Int
        
        val totalWeek = completedWeek + pendingWeek
        val weeklyCompletion = if (totalWeek > 0) (completedWeek.toFloat() / totalWeek.toFloat() * 100).toInt() else 0

        DashboardState(
            totalNotes = totalNotes,
            pendingTasks = pending,
            completedTasks = completedToday,
            dueToday = dueToday,
            overdue = overdue,
            streak = streakCount,
            highestStreak = highestStreak,
            archivedCount = archivedCount,
            trashedCount = trashedCount,
            weeklyCompletionRate = weeklyCompletion
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())

    private fun getStartOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    data class DashboardState(
        val totalNotes: Int = 0,
        val pendingTasks: Int = 0,
        val completedTasks: Int = 0,
        val dueToday: Int = 0,
        val overdue: Int = 0,
        val streak: Int = 0,
        val highestStreak: Int = 0,
        val archivedCount: Int = 0,
        val trashedCount: Int = 0,
        val weeklyCompletionRate: Int = 0
    )

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

    fun selectFolder(folderId: Long?) {
        _selectedFolderId.value = folderId
    }

    fun setGridView(isGrid: Boolean) = viewModelScope.launch {
        settingsManager.setGridView(isGrid)
    }

    fun duplicateNote(note: Note) = viewModelScope.launch {
        val duplicatedNote = note.copy(
            id = 0,
            title = "${note.title} (Copy)",
            createdTime = System.currentTimeMillis(),
            updatedTime = System.currentTimeMillis(),
            isSynced = false
        )
        repository.insertNote(duplicatedNote)
    }

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
