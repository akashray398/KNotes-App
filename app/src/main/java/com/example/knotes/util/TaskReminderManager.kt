package com.example.knotes.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.knotes.domain.model.Task
import com.example.knotes.receiver.ReminderReceiver
import java.util.concurrent.TimeUnit

import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskReminderManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val TYPE_EXACT = 0
    private val TYPE_2HRS = 1
    private val TYPE_1DAY = 2
    private val TYPE_30MINS = 3

    fun scheduleTaskReminders(task: Task) {
        val deadline = task.deadline ?: return
        if (task.isCompleted) return

        // 1. At deadline
        scheduleAlarm(task, deadline, TYPE_EXACT)

        // 2. 30 minutes before
        val thirtyMinsBefore = deadline - TimeUnit.MINUTES.toMillis(30)
        if (thirtyMinsBefore > System.currentTimeMillis()) {
            scheduleAlarm(task, thirtyMinsBefore, TYPE_30MINS)
        }

        // 3. 2 hours before
        val twoHoursBefore = deadline - TimeUnit.HOURS.toMillis(2)
        if (twoHoursBefore > System.currentTimeMillis()) {
            scheduleAlarm(task, twoHoursBefore, TYPE_2HRS)
        }

        // 4. 1 day before
        val oneDayBefore = deadline - TimeUnit.DAYS.toMillis(1)
        if (oneDayBefore > System.currentTimeMillis()) {
            scheduleAlarm(task, oneDayBefore, TYPE_1DAY)
        }
    }

    private fun scheduleAlarm(task: Task, timeInMillis: Long, type: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val requestCode = task.id * 10 + type

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "ACTION_TASK_REMINDER"
            putExtra("taskId", task.id)
            putExtra("taskTitle", task.title)
            putExtra("reminderType", type)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
                return
            }
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelTaskReminders(task: Task) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf(TYPE_EXACT, TYPE_2HRS, TYPE_1DAY, TYPE_30MINS).forEach { type ->
            val requestCode = task.id * 10 + type
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
            }
        }
    }
}
