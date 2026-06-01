package com.example.knotes.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY deadline ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :searchQuery || '%' OR description LIKE '%' || :searchQuery || '%' ORDER BY deadline ASC")
    fun searchTasks(searchQuery: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks ORDER BY CASE priority WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 3 END ASC")
    fun getTasksByPriority(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)
}
