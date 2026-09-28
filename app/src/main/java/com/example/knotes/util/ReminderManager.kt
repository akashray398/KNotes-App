package com.example.knotes.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.example.knotes.receiver.ReminderReceiver

object ReminderManager {

    private const val TAG = "KNotes_Reminder"
    private const val NOTE_REMINDER_OFFSET = 5

    fun scheduleReminder(context: Context, noteId: Int, title: String, content: String, timeInMillis: Long) {
        if (timeInMillis < System.currentTimeMillis()) {
            Log.d(TAG, "REMINDER_SKIPPED noteId=$noteId reason=past_time")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    Toast.makeText(context, "Please grant exact alarm permission for reminders to work.", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Log.w(TAG, "Unable to launch exact alarm settings: ${e.message}")
                }
            }
        }

        val requestCode = noteId * 100 + NOTE_REMINDER_OFFSET

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "ACTION_NOTE_REMINDER"
            putExtra("title", "Reminder: $title")
            putExtra("content", content)
            putExtra("noteId", noteId)
            putExtra("scheduledTime", timeInMillis)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "REMINDER_SCHEDULED noteId=$noteId requestCode=$requestCode time=$timeInMillis exact=$canExact")
            Toast.makeText(context, "Reminder set!", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Log.w(TAG, "REMINDER_FALLBACK noteId=$noteId requestCode=$requestCode fallback=setAndAllowWhileIdle")
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
            Toast.makeText(context, "Reminder set!", Toast.LENGTH_SHORT).show()
        }
    }

    fun cancelReminder(context: Context, noteId: Int) {
        val requestCode = noteId * 100 + NOTE_REMINDER_OFFSET
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "REMINDER_CANCELLED noteId=$noteId requestCode=$requestCode")
        }
    }
}
