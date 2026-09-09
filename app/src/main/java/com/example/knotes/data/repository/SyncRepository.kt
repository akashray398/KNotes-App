package com.example.knotes.data.repository

import android.util.Log
import com.example.knotes.data.dao.FolderDao
import com.example.knotes.data.dao.NoteDao
import com.example.knotes.data.dao.TaskDao
import com.example.knotes.data.entity.Folder
import com.example.knotes.data.entity.Note
import com.example.knotes.data.entity.Task
import com.example.knotes.util.SettingsManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val auth: FirebaseAuth?,
    private val firestore: FirebaseFirestore?,
    private val noteDao: NoteDao,
    private val taskDao: TaskDao,
    private val folderDao: FolderDao,
    private val settingsManager: SettingsManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val TAG = "SyncRepository"
    
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    sealed class SyncState {
        object Idle : SyncState()
        object Syncing : SyncState()
        data class Success(val lastSynced: Long) : SyncState()
        data class Error(val message: String) : SyncState()
    }

    private val userId: String?
        get() = auth?.currentUser?.uid

    fun startSync(manual: Boolean = false) {
        if (auth == null || firestore == null) {
            Log.w(TAG, "Sync skipped: Firebase not properly initialized")
            _syncState.value = SyncState.Error("Firebase not initialized")
            return
        }
        
        val uid = userId
        if (uid == null) {
            Log.w(TAG, "Sync skipped: User not signed in")
            return
        }
        
        if (_syncState.value is SyncState.Syncing) return
        _syncState.value = SyncState.Syncing
        
        Log.d(TAG, "Starting sync for user: $uid (manual=$manual)")
        
        scope.launch {
            try {
                // 1. Initial Sync: Pull remote data if it's a new sign-in or manual sync
                if (manual) {
                    pullRemoteData(uid)
                }
                
                // 2. Upload unsynced local data
                syncLocalToRemote(uid)
                
                // 3. Continuous Listeners (only if not already listening)
                // In a real app, you'd manage listener registrations. 
                // For this implementation, we'll assume they are started once.
                listenForRemoteChanges(uid)
                
                val now = System.currentTimeMillis()
                settingsManager.setLastSynced(now)
                _syncState.value = SyncState.Success(now)
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed", e)
                _syncState.value = SyncState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun pullRemoteData(uid: String) {
        if (firestore == null) return
        
        // Pull Folders
        val folders = firestore.collection("users").document(uid).collection("folders").get().await()
        folders.documents.forEach { doc ->
            val remoteFolder = doc.toObject(Folder::class.java)
            if (remoteFolder != null) {
                val localFolder = folderDao.getFolderByRemoteId(remoteFolder.remoteId!!)
                if (localFolder == null) {
                    folderDao.insertFolder(remoteFolder.copy(id = 0, isSynced = true))
                } else if (remoteFolder.updatedTime > localFolder.updatedTime) {
                    folderDao.updateFolder(remoteFolder.copy(id = localFolder.id, isSynced = true))
                }
            }
        }

        // Pull Notes
        val notes = firestore.collection("users").document(uid).collection("notes").get().await()
        notes.documents.forEach { doc ->
            val remoteNote = doc.toObject(Note::class.java)
            if (remoteNote != null) {
                val localNote = noteDao.getNoteByRemoteId(remoteNote.remoteId!!)
                if (localNote == null) {
                    noteDao.insertNote(remoteNote.copy(id = 0, isSynced = true))
                } else if (remoteNote.updatedTime > localNote.updatedTime) {
                    noteDao.updateNote(remoteNote.copy(id = localNote.id, isSynced = true))
                }
            }
        }

        // Pull Tasks
        val tasks = firestore.collection("users").document(uid).collection("tasks").get().await()
        tasks.documents.forEach { doc ->
            val remoteTask = doc.toObject(Task::class.java)
            if (remoteTask != null) {
                val localTask = taskDao.getTaskByRemoteId(remoteTask.remoteId!!)
                if (localTask == null) {
                    taskDao.insertTask(remoteTask.copy(id = 0, isSynced = true))
                } else if (remoteTask.updatedTime > localTask.updatedTime) {
                    taskDao.updateTask(remoteTask.copy(id = localTask.id, isSynced = true))
                }
            }
        }
    }

    private suspend fun syncLocalToRemote(uid: String) {
        try {
            // Folders
            val unsyncedFolders = folderDao.getAllFolders().first().filter { !it.isSynced || it.remoteId == null }
            unsyncedFolders.forEach { uploadFolder(uid, it) }

            // Notes
            val unsyncedNotes = noteDao.getAllNotes().first().filter { !it.isSynced || it.remoteId == null }
            unsyncedNotes.forEach { uploadNote(uid, it) }

            // Tasks
            val unsyncedTasks = taskDao.getAllTasks().first().filter { !it.isSynced || it.remoteId == null }
            unsyncedTasks.forEach { uploadTask(uid, it) }
        } catch (e: Exception) {
            Log.e(TAG, "Local to Remote sync failed", e)
        }
    }

    private suspend fun uploadFolder(uid: String, folder: Folder) {
        if (firestore == null) return
        try {
            val collection = firestore.collection("users").document(uid).collection("folders")
            val docRef = if (folder.remoteId != null) {
                collection.document(folder.remoteId!!)
            } else {
                collection.document()
            }

            val remoteId = docRef.id
            val folderToUpload = folder.copy(remoteId = remoteId, isSynced = true, updatedTime = System.currentTimeMillis())
            
            docRef.set(folderToUpload, SetOptions.merge()).await()
            folderDao.updateFolder(folderToUpload)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading folder: ${folder.name}", e)
        }
    }

    private suspend fun uploadNote(uid: String, note: Note) {
        if (firestore == null) return
        try {
            val collection = firestore.collection("users").document(uid).collection("notes")
            val docRef = if (note.remoteId != null) {
                collection.document(note.remoteId!!)
            } else {
                collection.document()
            }

            val remoteId = docRef.id
            val noteToUpload = note.copy(remoteId = remoteId, isSynced = true, updatedTime = System.currentTimeMillis())
            
            docRef.set(noteToUpload, SetOptions.merge()).await()
            noteDao.updateNote(noteToUpload)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading note: ${note.title}", e)
        }
    }

    private suspend fun uploadTask(uid: String, task: Task) {
        if (firestore == null) return
        try {
            val collection = firestore.collection("users").document(uid).collection("tasks")
            val docRef = if (task.remoteId != null) {
                collection.document(task.remoteId!!)
            } else {
                collection.document()
            }

            val remoteId = docRef.id
            val taskToUpload = task.copy(remoteId = remoteId, isSynced = true, updatedTime = System.currentTimeMillis())
            
            docRef.set(taskToUpload, SetOptions.merge()).await()
            taskDao.updateTask(taskToUpload)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading task: ${task.title}", e)
        }
    }

    private fun listenForRemoteChanges(uid: String) {
        if (firestore == null) return
        
        // Listen for Folders
        firestore.collection("users").document(uid).collection("folders")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                snapshots?.documentChanges?.forEach { dc ->
                    try {
                        val remote = dc.document.toObject(Folder::class.java) ?: return@forEach
                        scope.launch {
                            val local = folderDao.getFolderByRemoteId(remote.remoteId ?: return@launch)
                            if (local == null) {
                                folderDao.insertFolder(remote.copy(id = 0, isSynced = true))
                            } else if (remote.updatedTime > local.updatedTime) {
                                folderDao.updateFolder(remote.copy(id = local.id, isSynced = true))
                            }
                        }
                    } catch (ex: Exception) { Log.e(TAG, "Error parsing remote folder", ex) }
                }
            }

        // Listen for Notes
        firestore.collection("users").document(uid).collection("notes")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                snapshots?.documentChanges?.forEach { dc ->
                    try {
                        val remote = dc.document.toObject(Note::class.java) ?: return@forEach
                        scope.launch {
                            val local = noteDao.getNoteByRemoteId(remote.remoteId ?: return@launch)
                            if (local == null) {
                                noteDao.insertNote(remote.copy(id = 0, isSynced = true))
                            } else if (remote.updatedTime > local.updatedTime) {
                                noteDao.updateNote(remote.copy(id = local.id, isSynced = true))
                            }
                        }
                    } catch (ex: Exception) { Log.e(TAG, "Error parsing remote note", ex) }
                }
            }

        // Listen for Tasks
        firestore.collection("users").document(uid).collection("tasks")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                snapshots?.documentChanges?.forEach { dc ->
                    try {
                        val remote = dc.document.toObject(Task::class.java) ?: return@forEach
                        scope.launch {
                            val local = taskDao.getTaskByRemoteId(remote.remoteId ?: return@launch)
                            if (local == null) {
                                taskDao.insertTask(remote.copy(id = 0, isSynced = true))
                            } else if (remote.updatedTime > local.updatedTime) {
                                taskDao.updateTask(remote.copy(id = local.id, isSynced = true))
                            }
                        }
                    } catch (ex: Exception) { Log.e(TAG, "Error parsing remote task", ex) }
                }
            }
    }

    suspend fun clearTrash() {
        noteDao.clearTrash()
    }
}
