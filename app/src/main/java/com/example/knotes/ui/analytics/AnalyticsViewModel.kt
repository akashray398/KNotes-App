package com.example.knotes.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.knotes.domain.repository.NoteRepository
import com.example.knotes.domain.repository.TaskRepository
import com.example.knotes.domain.usecase.GetProductivityAnalyticsUseCase
import com.example.knotes.domain.usecase.ProductivityAnalyticsReport
import com.example.knotes.util.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    noteRepository: NoteRepository,
    taskRepository: TaskRepository,
    settingsManager: SettingsManager,
    private val getProductivityAnalyticsUseCase: GetProductivityAnalyticsUseCase
) : ViewModel() {

    val analyticsReport: StateFlow<ProductivityAnalyticsReport> = combine(
        noteRepository.getAllNotes(),
        taskRepository.getAllTasks(),
        settingsManager.currentStreak,
        settingsManager.highestStreak
    ) { notes, tasks, streak, highestStreak ->
        getProductivityAnalyticsUseCase(notes, tasks, streak, highestStreak)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = getProductivityAnalyticsUseCase(emptyList(), emptyList(), 0, 0)
    )
}
