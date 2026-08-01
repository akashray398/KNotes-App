package com.example.knotes.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.knotes.data.repository.TaskRepository
import com.example.knotes.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val pendingCount = taskRepository.getPendingTasksCount().first()
        
        if (pendingCount > 0) {
            val calendar = java.util.Calendar.getInstance()
            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            
            val (title, content) = when {
                hour < 12 -> "🌅 Good Morning! Complete your today pending tasks." to "You have $pendingCount pending tasks. Let's make this day productive! 💜"
                hour < 17 -> "⚡ Mid-day Check-in!" to "Don't let those $pendingCount tasks pile up. You've got this!"
                else -> "🌙 Wrapping up the day?" to "Check off your $pendingCount pending tasks before you rest. 📋"
            }

            NotificationHelper.showNotification(
                context = applicationContext,
                channelId = NotificationHelper.CHANNEL_ID_DAILY,
                id = 9999,
                title = title,
                content = content,
                targetScreen = "tasks"
            )
        }
        
        return Result.success()
    }
}
