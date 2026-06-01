package com.example.knotes.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.knotes.data.entity.Priority
import com.example.knotes.data.entity.Task
import com.example.knotes.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _priorityFilter = MutableStateFlow<Priority?>(null)
    val priorityFilter = _priorityFilter.asStateFlow()

    enum class SortOrder { DUE_DATE, PRIORITY, NEWEST, OLDEST, ALPHABETICAL }

    private val _sortOrder = MutableStateFlow(SortOrder.DUE_DATE)
    val sortOrder = _sortOrder.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks = combine(_searchQuery, _priorityFilter, _sortOrder) { query, priority, sort ->
        Triple(query, priority, sort)
    }.flatMapLatest { (query, priority, sort) ->
        val sourceFlow = if (priority != null) {
            repository.getTasksByPriority(priority)
        } else if (query.isNotBlank()) {
            repository.searchTasks(query)
        } else {
            repository.getAllTasks()
        }
        
        sourceFlow.map { list ->
            when (sort) {
                SortOrder.DUE_DATE -> list.sortedBy { it.deadline ?: Long.MAX_VALUE }
                SortOrder.PRIORITY -> list.sortedByDescending { it.priority.ordinal }
                SortOrder.NEWEST -> list.sortedByDescending { it.id } 
                SortOrder.OLDEST -> list.sortedBy { it.id }
                SortOrder.ALPHABETICAL -> list.sortedBy { it.title.lowercase() }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val productivityStats = combine(
        repository.getCompletedTasksCount(),
        repository.getPendingTasksCount()
    ) { completed, pending ->
        val total = completed + pending
        val percent = if (total > 0) (completed.toFloat() / total.toFloat() * 100).toInt() else 0
        Triple(completed, pending, percent)
    }.stateIn(viewModelScope, SharingStarted.Lazily, Triple(0, 0, 0))

    fun updateSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updatePriorityFilter(priority: Priority?) {
        _priorityFilter.value = priority
    }

    fun insertTask(task: Task) = viewModelScope.launch {
        repository.insertTask(task)
    }

    fun updateTask(task: Task) = viewModelScope.launch {
        repository.updateTask(task)
    }

    fun deleteTask(task: Task) = viewModelScope.launch {
        repository.deleteTask(task)
    }

    fun toggleTaskCompletion(task: Task) = viewModelScope.launch {
        repository.updateTaskCompletion(task.id, !task.isCompleted)
    }

    suspend fun getTaskById(id: Int): Task? = repository.getTaskById(id)
}
