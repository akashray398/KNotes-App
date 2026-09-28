package com.example.knotes.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.knotes.domain.repository.NoteRepository
import com.example.knotes.domain.repository.TaskRepository
import com.example.knotes.util.RecurrenceHelper
import com.example.knotes.util.ReminderManager
import com.example.knotes.util.TaskReminderManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    private companion object {
        private const val TAG = "KNotes_Boot"
    }

    @Inject
    lateinit var taskRepository: TaskRepository

    @Inject
    lateinit var noteRepository: NoteRepository

    @Inject
    lateinit var taskReminderManager: TaskReminderManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d(TAG, "BOOT_COMPLETED received. Restoring active task and note reminders...")
                    val now = System.currentTimeMillis()

                    // Restore Task Reminders
                    val tasks = taskRepository.getAllTasks().first()
                    tasks.filter { !it.isCompleted }.forEach { task ->
                        val entityRecurrence = com.example.knotes.data.entity.Recurrence.valueOf(task.recurrence.name)

                        var activeTask = task
                        if (task.reminderTime != null && task.reminderTime < now) {
                            if (entityRecurrence != com.example.knotes.data.entity.Recurrence.NONE) {
                                val nextTime = RecurrenceHelper.getNextOccurrence(task.reminderTime, entityRecurrence)
                                if (nextTime != null) {
                                    activeTask = task.copy(reminderTime = nextTime)
                                    taskRepository.updateTask(activeTask)
                                }
                            } else {
                                // One-time past reminder, clear it to prevent catch-up loop
                                activeTask = task.copy(reminderTime = null)
                                taskRepository.updateTask(activeTask)
                            }
                        }

                        if (activeTask.reminderTime != null || activeTask.secondaryReminderTime != null) {
                            taskReminderManager.scheduleTaskReminders(activeTask)
                        }
                    }

                    // Restore Note Reminders
                    val notes = noteRepository.getAllNotes().first()
                    notes.filter { !it.isTrashed && !it.isArchived && it.reminderTime != null }.forEach { note ->
                        if (note.reminderTime!! > now) {
                            ReminderManager.scheduleReminder(
                                context = context,
                                noteId = note.id,
                                title = note.title,
                                content = note.content.take(50),
                                timeInMillis = note.reminderTime
                            )
                        } else {
                            // Past note reminder, clear it
                            noteRepository.updateNote(note.copy(reminderTime = null))
                        }
                    }

                    Log.d(TAG, "BOOT_COMPLETED reminder restoration completed successfully.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error restoring reminders on boot: ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
