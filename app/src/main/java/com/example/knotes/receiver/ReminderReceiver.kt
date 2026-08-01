package com.example.knotes.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.knotes.util.NotificationHelper

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "ACTION_TASK_REMINDER" -> handleTaskReminder(context, intent)
            "ACTION_NOTE_REMINDER" -> handleNoteReminder(context, intent)
            Intent.ACTION_BOOT_COMPLETED -> {
                // Boot logic is now handled in BootReceiver.kt
            }
            else -> {
                // Generic fallback
                val title = intent.getStringExtra("title") ?: "Reminder"
                val content = intent.getStringExtra("content") ?: "You have a reminder."
                NotificationHelper.showNotification(
                    context, NotificationHelper.CHANNEL_ID_TASKS, System.currentTimeMillis().toInt(), 
                    title, content
                )
            }
        }
    }

    private fun handleNoteReminder(context: Context, intent: Intent) {
        val noteId = intent.getIntExtra("noteId", 0)
        val title = intent.getStringExtra("title") ?: "Note Reminder"
        val content = intent.getStringExtra("content") ?: "You have a reminder for your note."
        
        NotificationHelper.showNotification(
            context = context,
            channelId = NotificationHelper.CHANNEL_ID_TASKS,
            id = noteId + 30000, // Offset for note reminders
            title = title,
            content = content,
            targetScreen = "notes"
        )
    }

    private fun handleTaskReminder(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra("taskId", 0)
        val taskTitle = intent.getStringExtra("taskTitle") ?: "Task"
        val type = intent.getIntExtra("reminderType", 0)

        val content = when (type) {
            1 -> "This task is due in 2 hours."
            2 -> "This task is due tomorrow."
            3 -> "Deadline approaching! 30 minutes left."
            else -> "Time's up! The deadline is here."
        }

        NotificationHelper.showNotification(
            context = context,
            channelId = NotificationHelper.CHANNEL_ID_TASKS,
            id = taskId + 10000, // Offset for task reminders
            title = "📌 Task Reminder: $taskTitle",
            content = content,
            targetScreen = "tasks"
        )
    }
}
