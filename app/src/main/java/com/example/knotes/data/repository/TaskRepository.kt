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

    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task.copy(updatedTime = System.currentTimeMillis(), isSynced = false))

    suspend fun updateTask(task: Task) = taskDao.updateTask(task.copy(updatedTime = System.currentTimeMillis(), isSynced = false))

    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    suspend fun updateTaskCompletion(id: Int, isCompleted: Boolean) {
        val task = taskDao.getTaskById(id)
        if (task != null) {
            taskDao.updateTask(task.copy(isCompleted = isCompleted, updatedTime = System.currentTimeMillis(), isSynced = false))
        }
    }

    fun getCompletedTasksCount(): Flow<Int> = taskDao.getCompletedTasksCount()

    fun getPendingTasksCount(): Flow<Int> = taskDao.getPendingTasksCount()

    fun getCompletedTodayCount(startOfDay: Long): Flow<Int> = taskDao.getCompletedTodayCount(startOfDay)

    fun getDueTodayCount(startOfDay: Long, endOfDay: Long): Flow<Int> = taskDao.getDueTodayCount(startOfDay, endOfDay)

    fun getOverdueCount(now: Long): Flow<Int> = taskDao.getOverdueCount(now)

    fun getCompletedTasksSince(since: Long): Flow<Int> = taskDao.getCompletedTasksSince(since)

    fun getPendingTasksSince(since: Long): Flow<Int> = taskDao.getPendingTasksSince(since)
}
