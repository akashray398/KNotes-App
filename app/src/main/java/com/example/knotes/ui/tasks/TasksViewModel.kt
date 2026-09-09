package com.example.knotes.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.knotes.domain.model.Priority
import com.example.knotes.domain.model.Task
import com.example.knotes.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val taskReminderManager: com.example.knotes.util.TaskReminderManager,
    private val streakManager: com.example.knotes.util.StreakManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _priorityFilter = MutableStateFlow<Priority?>(null)
    val priorityFilter = _priorityFilter.asStateFlow()

    enum class SortOrder { DUE_DATE, PRIORITY, NEWEST, OLDEST, ALPHABETICAL }

    private val _sortOrder = MutableStateFlow(SortOrder.DUE_DATE)
    val sortOrder = _sortOrder.asStateFlow()

    enum class TaskFilter { ALL, TODAY, UPCOMING, COMPLETED, OVERDUE }

    private val _taskFilter = MutableStateFlow(TaskFilter.ALL)
    val taskFilter = _taskFilter.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks = combine(_searchQuery, _priorityFilter, _sortOrder, _taskFilter) { query, priority, sort, filter ->
        Quadruple(query, priority, sort, filter)
    }.flatMapLatest { (query, priority, sort, filter) ->
        val sourceFlow = if (priority != null) {
            repository.getTasksByPriority(priority)
        } else if (query.isNotBlank()) {
            repository.searchTasks(query)
        } else {
            repository.getAllTasks()
        }
        
        sourceFlow.map { list ->
            val now = System.currentTimeMillis()
            val startOfDay = getStartOfDay(now)
            val endOfDay = getEndOfDay(now)

            val filteredList = when (filter) {
                TaskFilter.ALL -> list
                TaskFilter.TODAY -> list.filter { it.deadline != null && it.deadline in startOfDay..endOfDay && !it.isCompleted }
                TaskFilter.UPCOMING -> list.filter { it.deadline != null && it.deadline > endOfDay && !it.isCompleted }
                TaskFilter.COMPLETED -> list.filter { it.isCompleted }
                TaskFilter.OVERDUE -> list.filter { it.deadline != null && it.deadline < startOfDay && !it.isCompleted }
            }

            when (sort) {
                SortOrder.DUE_DATE -> filteredList.sortedBy { it.deadline ?: Long.MAX_VALUE }
                SortOrder.PRIORITY -> filteredList.sortedByDescending { it.priority.ordinal }
                SortOrder.NEWEST -> filteredList.sortedByDescending { it.id } 
                SortOrder.OLDEST -> filteredList.sortedBy { it.id }
                SortOrder.ALPHABETICAL -> filteredList.sortedBy { it.title.lowercase() }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getEndOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    fun updateTaskFilter(filter: TaskFilter) {
        _taskFilter.value = filter
    }

    data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

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

    suspend fun insertTask(task: Task): Long {
        return repository.insertTask(task)
    }

    fun updateTask(task: Task) = viewModelScope.launch {
        repository.updateTask(task)
    }

    fun deleteTask(task: Task) = viewModelScope.launch {
        repository.deleteTask(task)
    }

    fun toggleTaskCompletion(task: Task) = viewModelScope.launch {
        val newCompletedState = !task.isCompleted
        
        if (newCompletedState && task.recurrence != com.example.knotes.domain.model.Recurrence.NONE) {
            // It's a recurring task being completed -> schedule next occurrence
            val nextDeadline = com.example.knotes.util.RecurrenceHelper.getNextOccurrence(task.deadline, com.example.knotes.data.entity.Recurrence.valueOf(task.recurrence.name))
            val updatedTask = task.copy(deadline = nextDeadline, isCompleted = false)
            repository.updateTask(updatedTask)
            
            // Reschedule reminders for the next occurrence
            taskReminderManager.cancelTaskReminders(task)
            taskReminderManager.scheduleTaskReminders(updatedTask)
            
            showCompletionCelebration(task.title)
            streakManager.checkAndUpdateStreak()
        } else {
            repository.updateTaskCompletion(task.id, newCompletedState)
            if (newCompletedState) {
                taskReminderManager.cancelTaskReminders(task)
                showCompletionCelebration(task.title)
                streakManager.checkAndUpdateStreak()
            } else {
                taskReminderManager.scheduleTaskReminders(task)
            }
        }
    }

    private fun showCompletionCelebration(title: String) {
        // We'll use a static method from NotificationHelper to avoid passing context
        // But NotificationHelper needs context. For now, we'll assume the fragment handles UI celebration
        // and ViewModel sends a "nudge" to Fragment or system notification.
    }

    suspend fun getTaskById(id: Int): Task? = repository.getTaskById(id)
}
