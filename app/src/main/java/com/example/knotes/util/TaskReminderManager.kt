package com.example.knotes.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.knotes.domain.model.Task
import com.example.knotes.receiver.ReminderReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskReminderManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private companion object {
        private const val TAG = "KNotes_Reminder"
        private const val PRIMARY_REMINDER_OFFSET = 1
        private const val SECONDARY_REMINDER_OFFSET = 2
    }

    fun scheduleTaskReminders(task: Task) {
        if (task.isCompleted) {
            Log.d(TAG, "REMINDER_SKIPPED taskId=${task.id} reason=task_completed")
            cancelTaskReminders(task)
            return
        }

        schedulePrimaryReminder(task)
        scheduleSecondaryReminder(task)
    }

    private fun schedulePrimaryReminder(task: Task) {
        val timeInMillis = task.reminderTime ?: return
        val now = System.currentTimeMillis()

        if (timeInMillis < now) {
            Log.d(TAG, "REMINDER_SKIPPED taskId=${task.id} type=primary reason=past_time")
            return
        }

        val requestCode = task.id * 100 + PRIMARY_REMINDER_OFFSET
        scheduleAlarm(
            requestCode = requestCode,
            taskId = task.id,
            title = task.title,
            reminderType = PRIMARY_REMINDER_OFFSET,
            timeInMillis = timeInMillis
        )
    }

    private fun scheduleSecondaryReminder(task: Task) {
        val timeInMillis = task.secondaryReminderTime ?: return
        val now = System.currentTimeMillis()

        if (timeInMillis < now) {
            Log.d(TAG, "REMINDER_SKIPPED taskId=${task.id} type=secondary reason=past_time")
            return
        }

        val requestCode = task.id * 100 + SECONDARY_REMINDER_OFFSET
        scheduleAlarm(
            requestCode = requestCode,
            taskId = task.id,
            title = task.title,
            reminderType = SECONDARY_REMINDER_OFFSET,
            timeInMillis = timeInMillis
        )
    }

    private fun scheduleAlarm(
        requestCode: Int,
        taskId: Int,
        title: String,
        reminderType: Int,
        timeInMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "ACTION_TASK_REMINDER"
            putExtra("taskId", taskId)
            putExtra("taskTitle", title)
            putExtra("reminderType", reminderType)
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
            Log.d(TAG, "REMINDER_SCHEDULED taskId=$taskId requestCode=$requestCode time=$timeInMillis exact=$canExact")
        } catch (e: SecurityException) {
            Log.w(TAG, "REMINDER_FALLBACK taskId=$taskId requestCode=$requestCode fallback=setAndAllowWhileIdle")
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelTaskReminders(task: Task) {
        cancelAlarm(task.id * 100 + PRIMARY_REMINDER_OFFSET)
        cancelAlarm(task.id * 100 + SECONDARY_REMINDER_OFFSET)
        Log.d(TAG, "REMINDER_CANCELLED taskId=${task.id}")
    }

    private fun cancelAlarm(requestCode: Int) {
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
        }
    }
}
