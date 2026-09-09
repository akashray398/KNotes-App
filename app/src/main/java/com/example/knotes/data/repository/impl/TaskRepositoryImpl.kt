package com.example.knotes.data.repository.impl

import com.example.knotes.data.dao.TaskDao
import com.example.knotes.data.mapper.toDomain
import com.example.knotes.data.mapper.toEntity
import com.example.knotes.domain.model.Task
import com.example.knotes.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {
    override fun getAllTasks(): Flow<List<Task>> = 
        taskDao.getAllTasks().map { list -> list.map { it.toDomain() } }

    override fun searchTasks(query: String): Flow<List<Task>> = 
        taskDao.searchTasks(query).map { list -> list.map { it.toDomain() } }

    override suspend fun getTaskById(id: Int): Task? = 
        taskDao.getTaskById(id)?.toDomain()

    override suspend fun insertTask(task: Task): Long = 
        taskDao.insertTask(task.toEntity().copy(isSynced = false))

    override suspend fun updateTask(task: Task) = 
        taskDao.updateTask(task.toEntity().copy(isSynced = false))

    override suspend fun deleteTask(task: Task) = 
        taskDao.deleteTask(task.toEntity())

    override suspend fun updateTaskCompletion(id: Int, isCompleted: Boolean) {
        val task = taskDao.getTaskById(id)
        if (task != null) {
            taskDao.updateTask(task.copy(isCompleted = isCompleted, updatedTime = System.currentTimeMillis(), isSynced = false))
        }
    }

    override fun getCompletedTasksCount(): Flow<Int> = taskDao.getCompletedTasksCount()
    override fun getPendingTasksCount(): Flow<Int> = taskDao.getPendingTasksCount()
    override fun getCompletedTodayCount(startOfDay: Long): Flow<Int> = taskDao.getCompletedTodayCount(startOfDay)
    override fun getDueTodayCount(startOfDay: Long, endOfDay: Long): Flow<Int> = taskDao.getDueTodayCount(startOfDay, endOfDay)
    override fun getOverdueCount(now: Long): Flow<Int> = taskDao.getOverdueCount(now)
    override fun getCompletedTasksSince(since: Long): Flow<Int> = taskDao.getCompletedTasksSince(since)
    override fun getPendingTasksSince(since: Long): Flow<Int> = taskDao.getPendingTasksSince(since)
    override fun getTasksByPriority(priority: com.example.knotes.domain.model.Priority): Flow<List<Task>> = 
        taskDao.getTasksByPriority(com.example.knotes.data.entity.Priority.valueOf(priority.name)).map { list -> list.map { it.toDomain() } }
}
