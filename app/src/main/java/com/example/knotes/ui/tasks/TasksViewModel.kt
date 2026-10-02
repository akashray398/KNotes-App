package com.example.knotes.ui.tasks

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.knotes.domain.model.Priority
import com.example.knotes.domain.model.Task
import com.example.knotes.domain.repository.TaskRepository
import com.example.knotes.domain.usecase.ToggleTaskCompletionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase,
    private val taskReminderManager: com.example.knotes.util.TaskReminderManager,
    private val streakManager: com.example.knotes.util.StreakManager
) : ViewModel() {

    private companion object {
        private const val TAG = "KNotes_Tasks"
    }

    private val inFlightTaskIds = ConcurrentHashMap.newKeySet<Int>()

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

    val allTasks = repository.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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

    // Authoritative Progress Calculation synchronized with Home Screen
    val productivityStats = repository.getAllTasks().map { tasks ->
        val now = System.currentTimeMillis()
        val startOfDay = getStartOfDay(now)
        val endOfDay = getEndOfDay(now)

        // Today's relevant tasks (scheduled for today, created today for today, or completed today)
        val todayRelevantTasks = tasks.filter { task ->
            val targetTime = task.deadline ?: task.reminderTime
            val isScheduledToday = targetTime != null && targetTime in startOfDay..endOfDay
            val isCreatedTodayNoFuture = task.createdTime in startOfDay..endOfDay && (targetTime == null || targetTime <= endOfDay)
            val isCompletedToday = task.lastCompletedTime != null && task.lastCompletedTime in startOfDay..endOfDay
            val isCurrentlyCompleted = task.isCompleted && (task.updatedTime in startOfDay..endOfDay || isScheduledToday || isCreatedTodayNoFuture)

            isScheduledToday || isCreatedTodayNoFuture || isCompletedToday || isCurrentlyCompleted
        }

        val completed = todayRelevantTasks.count { task ->
            (task.lastCompletedTime != null && task.lastCompletedTime in startOfDay..endOfDay) ||
            (task.isCompleted && (task.updatedTime in startOfDay..endOfDay || (task.deadline != null && task.deadline in startOfDay..endOfDay)))
        }

        val total = todayRelevantTasks.size
        val pending = maxOf(0, total - completed)
        val percent = if (total > 0) minOf(100, ((completed.toFloat() / total.toFloat()) * 100).toInt()) else 0

        Log.d(TAG, "PROGRESS_RECALCULATED total=$total completed=$completed pending=$pending percentage=$percent")
        Triple(completed, pending, percent)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Triple(0, 0, 0))

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
        val id = repository.insertTask(task)
        val insertedTask = task.copy(id = id.toInt())
        taskReminderManager.scheduleTaskReminders(insertedTask)
        Log.d(TAG, "TASK_CREATED taskId=$id title=${task.title}")
        return id
    }

    fun updateTask(task: Task) = viewModelScope.launch {
        repository.updateTask(task)
        taskReminderManager.cancelTaskReminders(task)
        taskReminderManager.scheduleTaskReminders(task)
        Log.d(TAG, "TASK_UPDATED taskId=${task.id}")
    }

    fun deleteTask(task: Task) = viewModelScope.launch {
        taskReminderManager.cancelTaskReminders(task)
        repository.deleteTask(task)
        Log.d(TAG, "TASK_DELETED taskId=${task.id}")
    }

    fun toggleTaskCompletion(task: Task) {
        // Rapid tap debouncing check by taskId
        if (!inFlightTaskIds.add(task.id)) {
            Log.d(TAG, "TASK_TOGGLE_DEBOUNCED taskId=${task.id}")
            return
        }

        viewModelScope.launch {
            try {
                val newCompletedState = !task.isCompleted
                val now = System.currentTimeMillis()
                Log.d(TAG, "TASK_COMPLETION_REQUEST taskId=${task.id} targetState=$newCompletedState")

                if (newCompletedState && task.recurrence != com.example.knotes.domain.model.Recurrence.NONE) {
                    // Recurring task completed -> advance to next occurrence
                    val entityRecurrence = com.example.knotes.data.entity.Recurrence.valueOf(task.recurrence.name)
                    val nextReminder = com.example.knotes.util.RecurrenceHelper.getNextOccurrence(
                        task.reminderTime ?: task.deadline,
                        entityRecurrence,
                        task.recurrenceEndDate
                    )
                    val nextDeadline = com.example.knotes.util.RecurrenceHelper.getNextOccurrence(
                        task.deadline,
                        entityRecurrence,
                        task.recurrenceEndDate
                    )

                    if (nextDeadline != null || nextReminder != null) {
                        val updatedTask = task.copy(
                            reminderTime = nextReminder,
                            deadline = nextDeadline ?: task.deadline,
                            isCompleted = false,
                            lastCompletedTime = now,
                            updatedTime = now
                        )
                        repository.updateTask(updatedTask)

                        taskReminderManager.cancelTaskReminders(task)
                        taskReminderManager.scheduleTaskReminders(updatedTask)

                        streakManager.checkAndUpdateStreak()
                        Log.d(TAG, "TASK_RECURRING_ADVANCED taskId=${task.id} nextDeadline=$nextDeadline")
                    } else {
                        // Recurrence end date reached! Complete task permanently
                        val updatedTask = task.copy(
                            isCompleted = true,
                            recurrence = com.example.knotes.domain.model.Recurrence.NONE,
                            lastCompletedTime = now,
                            updatedTime = now
                        )
                        repository.updateTask(updatedTask)
                        taskReminderManager.cancelTaskReminders(task)
                        streakManager.checkAndUpdateStreak()
                        Log.d(TAG, "TASK_RECURRING_COMPLETED_END_DATE taskId=${task.id}")
                    }
                } else {
                    val updatedTask = task.copy(
                        isCompleted = newCompletedState,
                        lastCompletedTime = if (newCompletedState) now else null,
                        updatedTime = now
                    )
                    repository.updateTask(updatedTask)
                    if (newCompletedState) {
                        taskReminderManager.cancelTaskReminders(task)
                        streakManager.checkAndUpdateStreak()
                        Log.d(TAG, "TASK_COMPLETED taskId=${task.id} completed=true")
                    } else {
                        taskReminderManager.scheduleTaskReminders(updatedTask)
                        Log.d(TAG, "TASK_UNCOMPLETED taskId=${task.id} completed=false")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling task completion for taskId=${task.id}: ${e.message}", e)
            } finally {
                inFlightTaskIds.remove(task.id)
            }
        }
    }

    suspend fun getTaskById(id: Int): Task? = repository.getTaskById(id)
}
