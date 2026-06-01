package com.example.knotes.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KNotesRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val taskDao: TaskDao
) {

    // Notes
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

    fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query)

    suspend fun insertNote(note: Note) = noteDao.insertNote(note)

    suspend fun updateNote(note: Note) = noteDao.updateNote(note)

    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    // Tasks
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    fun searchTasks(query: String): Flow<List<Task>> = taskDao.searchTasks(query)

    fun getTasksByPriority(): Flow<List<Task>> = taskDao.getTasksByPriority()

    suspend fun insertTask(task: Task) = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)
}
