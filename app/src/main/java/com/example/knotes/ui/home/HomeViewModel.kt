package com.example.knotes.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.knotes.domain.model.Task
import com.example.knotes.domain.repository.NoteRepository
import com.example.knotes.domain.repository.TaskRepository
import com.example.knotes.domain.usecase.ToggleTaskCompletionUseCase
import com.example.knotes.util.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val taskRepository: TaskRepository,
    private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase,
    private val taskReminderManager: com.example.knotes.util.TaskReminderManager,
    private val streakManager: com.example.knotes.util.StreakManager,
    val settingsManager: SettingsManager
) : ViewModel() {

    val allNotes = noteRepository.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allTasks = taskRepository.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val totalNotesCount = noteRepository.getAllNotes().map { it.size }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val pendingTasksCount = taskRepository.getPendingTasksCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val homeState: StateFlow<HomeState> = combine(
        allNotes,
        allTasks,
        settingsManager.currentStreak
    ) { notes, tasks, streak ->
        val now = System.currentTimeMillis()
        val startOfDay = getStartOfDay(now)
        val endOfDay = getEndOfDay(now)

        // Today's tasks (tasks scheduled or due today)
        val todayTasks = tasks.filter { task ->
            val targetTime = task.deadline ?: task.reminderTime
            targetTime != null && targetTime in startOfDay..endOfDay
        }.sortedBy { it.deadline ?: it.reminderTime ?: Long.MAX_VALUE }

        val todayTotal = todayTasks.size
        val todayCompleted = todayTasks.count { it.isCompleted }
        val todayRemaining = todayTotal - todayCompleted
        val todayPercent = if (todayTotal > 0) ((todayCompleted.toFloat() / todayTotal.toFloat()) * 100).toInt() else 0

        HomeState(
            totalNotes = notes.size,
            pendingTasks = tasks.count { !it.isCompleted },
            todayTotal = todayTotal,
            todayCompleted = todayCompleted,
            todayRemaining = todayRemaining,
            todayProgressPercent = todayPercent,
            todayTasks = todayTasks.take(4),
            allTasks = tasks,
            streak = streak
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeState())

    fun toggleTaskCompletion(task: Task) = viewModelScope.launch {
        val newCompletedState = !task.isCompleted
        toggleTaskCompletionUseCase(task.id, newCompletedState)
        if (newCompletedState) {
            taskReminderManager.cancelTaskReminders(task)
            streakManager.checkAndUpdateStreak()
        } else {
            taskReminderManager.scheduleTaskReminders(task.copy(isCompleted = false))
        }
    }

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

    data class HomeState(
        val totalNotes: Int = 0,
        val pendingTasks: Int = 0,
        val todayTotal: Int = 0,
        val todayCompleted: Int = 0,
        val todayRemaining: Int = 0,
        val todayProgressPercent: Int = 0,
        val todayTasks: List<Task> = emptyList(),
        val allTasks: List<Task> = emptyList(),
        val streak: Int = 0
    )
}
