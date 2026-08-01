package com.example.knotes.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.knotes.MainActivity
import com.example.knotes.R

object NotificationHelper {
    const val CHANNEL_ID_TASKS = "tasks_reminders"
    const val CHANNEL_ID_DAILY = "daily_reminders"
    const val CHANNEL_ID_OVERDUE = "overdue_reminders"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val taskChannel = NotificationChannel(CHANNEL_ID_TASKS, "Task Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Reminders for upcoming tasks"
            }
            
            val dailyChannel = NotificationChannel(CHANNEL_ID_DAILY, "Daily Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Daily summary of pending tasks"
            }
            
            val overdueChannel = NotificationChannel(CHANNEL_ID_OVERDUE, "Overdue Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alerts for overdue tasks"
            }

            notificationManager.createNotificationChannels(listOf(taskChannel, dailyChannel, overdueChannel))
        }
    }

    fun showNotification(
        context: Context,
        channelId: String,
        id: Int,
        title: String,
        content: String,
        targetScreen: String? = null
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", targetScreen)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, id, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_tasks)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(context.getColor(R.color.purple_6750A4))

        notificationManager.notify(id, builder.build())
    }
}
