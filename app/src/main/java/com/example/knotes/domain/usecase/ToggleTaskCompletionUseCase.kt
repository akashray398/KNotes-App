package com.example.knotes.domain.usecase

import com.example.knotes.domain.repository.TaskRepository

class ToggleTaskCompletionUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(taskId: Int, isCompleted: Boolean) {
        repository.updateTaskCompletion(taskId, isCompleted)
    }
}
