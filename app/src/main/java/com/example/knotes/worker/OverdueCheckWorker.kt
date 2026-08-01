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
class OverdueCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val tasks = taskRepository.getAllTasks().first()
        val now = System.currentTimeMillis()
        
        tasks.filter { !it.isCompleted && it.deadline != null && it.deadline < now }
            .forEach { task ->
                val diff = now - task.deadline!!
                val timeText = when {
                    diff < 3600000 -> "just now"
                    diff < 86400000 -> "today"
                    else -> "yesterday"
                }

                NotificationHelper.showNotification(
                    context = applicationContext,
                    channelId = NotificationHelper.CHANNEL_ID_OVERDUE,
                    id = task.id + 20000,
                    title = "⚠️ Task Overdue!",
                    content = "\"${task.title}\" was due $timeText. Let's finish it now! 💪",
                    targetScreen = "tasks"
                )
            }
        
        return Result.success()
    }
}
