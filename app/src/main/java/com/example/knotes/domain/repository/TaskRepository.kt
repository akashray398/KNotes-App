package com.example.knotes.domain.repository

import com.example.knotes.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getAllTasks(): Flow<List<Task>>
    fun searchTasks(query: String): Flow<List<Task>>
    suspend fun getTaskById(id: Int): Task?
    suspend fun insertTask(task: Task): Long
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(task: Task)
    suspend fun updateTaskCompletion(id: Int, isCompleted: Boolean)
    fun getCompletedTasksCount(): Flow<Int>
    fun getPendingTasksCount(): Flow<Int>
    fun getCompletedTodayCount(startOfDay: Long): Flow<Int>
    fun getDueTodayCount(startOfDay: Long, endOfDay: Long): Flow<Int>
    fun getOverdueCount(now: Long): Flow<Int>
    fun getCompletedTasksSince(since: Long): Flow<Int>
    fun getPendingTasksSince(since: Long): Flow<Int>
    fun getTasksByPriority(priority: com.example.knotes.domain.model.Priority): Flow<List<Task>>
}
