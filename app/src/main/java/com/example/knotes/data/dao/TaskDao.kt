package com.example.knotes.data.dao

import androidx.room.*
import com.example.knotes.data.entity.Priority
import com.example.knotes.data.entity.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, deadline ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("""
        SELECT * FROM tasks 
        WHERE title LIKE '%' || :searchQuery || '%' 
        OR tags LIKE '%' || :searchQuery || '%'
        ORDER BY isCompleted ASC, deadline ASC
    """)
    fun searchTasks(searchQuery: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE priority = :priority ORDER BY isCompleted ASC, deadline ASC")
    fun getTasksByPriority(priority: Priority): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateTaskCompletion(id: Int, isCompleted: Boolean)

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1")
    fun getCompletedTasksCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0")
    fun getPendingTasksCount(): Flow<Int>
}
