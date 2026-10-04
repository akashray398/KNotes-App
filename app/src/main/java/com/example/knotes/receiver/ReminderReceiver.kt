package com.example.knotes.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.knotes.domain.repository.NoteRepository
import com.example.knotes.domain.repository.TaskRepository
import com.example.knotes.util.NotificationHelper
import com.example.knotes.util.RecurrenceHelper
import com.example.knotes.util.TaskReminderManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var taskRepository: TaskRepository

    @Inject
    lateinit var noteRepository: NoteRepository

    @Inject
    lateinit var taskReminderManager: TaskReminderManager

    private companion object {
        private const val TAG = "KNotes_Reminder"
        private val lastDeliveredCache = ConcurrentHashMap<String, Long>()
        private const val DEDUPLICATION_WINDOW_MS = 60_000L
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    "ACTION_TASK_REMINDER" -> handleTaskReminder(context, intent)
                    "ACTION_NOTE_REMINDER" -> handleNoteReminder(context, intent)
                    else -> {
                        val title = intent.getStringExtra("title") ?: "Reminder"
                        val content = intent.getStringExtra("content") ?: "You have a reminder."
                        NotificationHelper.showNotification(
                            context,
                            NotificationHelper.CHANNEL_ID_TASKS,
                            (System.currentTimeMillis() % 100000).toInt(),
                            title,
                            content
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling reminder broadcast: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleTaskReminder(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra("taskId", 0)
        val reminderType = intent.getIntExtra("reminderType", 1)
        val scheduledTime = intent.getLongExtra("scheduledTime", 0L)

        if (taskId == 0) return

        val dedupeKey = "task_${taskId}_$reminderType"
        val now = System.currentTimeMillis()
        val lastDelivered = lastDeliveredCache[dedupeKey] ?: 0L

        if (now - lastDelivered < DEDUPLICATION_WINDOW_MS) {
            Log.d(TAG, "REMINDER_ALREADY_DELIVERED taskId=$taskId type=$reminderType")
            return
        }

        val task = taskRepository.getTaskById(taskId)
        if (task == null || task.isCompleted) {
            Log.d(TAG, "REMINDER_SKIPPED taskId=$taskId reason=completed_or_deleted")
            return
        }

        lastDeliveredCache[dedupeKey] = now
        Log.d(TAG, "REMINDER_TRIGGERED taskId=$taskId type=$reminderType title=${task.title}")

        val content = if (reminderType == 2) {
            "Reminder #2: ${task.title}"
        } else {
            "Reminder: ${task.title}"
        }

        val notificationId = 100000 + taskId * 10 + reminderType
        NotificationHelper.showNotification(
            context = context,
            channelId = NotificationHelper.CHANNEL_ID_TASKS,
            id = notificationId,
            title = "📌 Task Reminder: ${task.title}",
            content = content,
            targetScreen = "tasks"
        )
        Log.d(TAG, "REMINDER_DELIVERED taskId=$taskId notificationId=$notificationId")

        // Handle Recurrence
        val entityRecurrence = com.example.knotes.data.entity.Recurrence.valueOf(task.recurrence.name)
        if (entityRecurrence != com.example.knotes.data.entity.Recurrence.NONE) {
            val nextTime = RecurrenceHelper.getNextOccurrence(
                task.reminderTime ?: scheduledTime,
                entityRecurrence,
                task.recurrenceEndDate
            )
            val nextDeadline = RecurrenceHelper.getNextOccurrence(
                task.deadline,
                entityRecurrence,
                task.recurrenceEndDate
            )
            if (nextTime != null || nextDeadline != null) {
                val updatedTask = task.copy(
                    reminderTime = nextTime,
                    deadline = nextDeadline ?: task.deadline
                )
                taskRepository.updateTask(updatedTask)
                taskReminderManager.scheduleTaskReminders(updatedTask)
                Log.d(TAG, "REMINDER_RESCHEDULED taskId=$taskId nextOccurrence=$nextTime")
            }
        }
    }

    private suspend fun handleNoteReminder(context: Context, intent: Intent) {
        val noteId = intent.getIntExtra("noteId", 0)
        val title = intent.getStringExtra("title") ?: "Note Reminder"
        val content = intent.getStringExtra("content") ?: "You have a reminder for your note."

        if (noteId == 0) return

        val dedupeKey = "note_$noteId"
        val now = System.currentTimeMillis()
        val lastDelivered = lastDeliveredCache[dedupeKey] ?: 0L

        if (now - lastDelivered < DEDUPLICATION_WINDOW_MS) {
            Log.d(TAG, "REMINDER_ALREADY_DELIVERED noteId=$noteId")
            return
        }

        val note = noteRepository.getNoteById(noteId)
        if (note == null || note.isTrashed || note.isArchived) {
            Log.d(TAG, "REMINDER_SKIPPED noteId=$noteId reason=trashed_archived_or_deleted")
            return
        }

        lastDeliveredCache[dedupeKey] = now
        Log.d(TAG, "REMINDER_TRIGGERED noteId=$noteId title=${note.title}")

        val notificationId = 300000 + noteId
        NotificationHelper.showNotification(
            context = context,
            channelId = NotificationHelper.CHANNEL_ID_TASKS,
            id = notificationId,
            title = title,
            content = content,
            targetScreen = "notes"
        )
        Log.d(TAG, "REMINDER_DELIVERED noteId=$noteId notificationId=$notificationId")

        // Clear reminderTime on note after delivery so it doesn't re-trigger
        val updatedNote = note.copy(reminderTime = null)
        noteRepository.updateNote(updatedNote)
    }
}
