package com.example.knotes.data.repository

import com.example.knotes.data.dao.TaskDao
import com.example.knotes.data.entity.Priority
import com.example.knotes.data.entity.Task
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
) {
    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()

    fun searchTasks(query: String): Flow<List<Task>> = taskDao.searchTasks(query)

    fun getTasksByPriority(priority: Priority): Flow<List<Task>> = taskDao.getTasksByPriority(priority)

    suspend fun getTaskById(id: Int): Task? = taskDao.getTaskById(id)

    suspend fun insertTask(task: Task) = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    suspend fun updateTaskCompletion(id: Int, isCompleted: Boolean) = taskDao.updateTaskCompletion(id, isCompleted)

    fun getCompletedTasksCount(): Flow<Int> = taskDao.getCompletedTasksCount()

    fun getPendingTasksCount(): Flow<Int> = taskDao.getPendingTasksCount()
}
