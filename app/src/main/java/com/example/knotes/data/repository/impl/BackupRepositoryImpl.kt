package com.example.knotes.data.repository.impl

import com.example.knotes.data.dao.FolderDao
import com.example.knotes.data.dao.NoteDao
import com.example.knotes.data.dao.TaskDao
import com.example.knotes.data.mapper.toDomain
import com.example.knotes.data.mapper.toEntity
import com.example.knotes.domain.model.Note
import com.example.knotes.domain.model.Task
import com.example.knotes.domain.model.Folder
import com.example.knotes.domain.repository.BackupRepository
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val taskDao: TaskDao,
    private val folderDao: FolderDao,
    private val gson: Gson
) : BackupRepository {

    override suspend fun exportData(outputStream: OutputStream): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val notes = noteDao.getAllNotesSync().map { it.toDomain() }
            val tasks = taskDao.getAllTasksSync().map { it.toDomain() }
            val folders = folderDao.getAllFoldersSync().map { it.toDomain() }

            val backupData = BackupData(notes, tasks, folders)
            val json = gson.toJson(backupData)

            OutputStreamWriter(outputStream).use { writer ->
                writer.write(json)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importData(inputStream: InputStream): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val backupData = InputStreamReader(inputStream).use { reader ->
                gson.fromJson(reader, BackupData::class.java)
            }

            backupData.folders.forEach { folder ->
                folderDao.insertFolder(folder.toEntity())
            }
            backupData.notes.forEach { note ->
                noteDao.insertNote(note.toEntity())
            }
            backupData.tasks.forEach { task ->
                taskDao.insertTask(task.toEntity())
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearAllData(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            noteDao.deleteAll()
            taskDao.deleteAll()
            folderDao.deleteAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class BackupData(
        @SerializedName("notes") val notes: List<Note>,
        @SerializedName("tasks") val tasks: List<Task>,
        @SerializedName("folders") val folders: List<Folder>
    )
}
