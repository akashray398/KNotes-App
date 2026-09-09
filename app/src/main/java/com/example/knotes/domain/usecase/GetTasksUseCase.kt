package com.example.knotes.domain.usecase

import com.example.knotes.domain.model.Task
import com.example.knotes.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow

class GetTasksUseCase(private val repository: TaskRepository) {
    operator fun invoke(query: String = ""): Flow<List<Task>> {
        return if (query.isBlank()) {
            repository.getAllTasks()
        } else {
            repository.searchTasks(query)
        }
    }
}
