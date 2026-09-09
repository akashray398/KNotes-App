package com.example.knotes.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.knotes.domain.repository.TaskRepository
import com.example.knotes.util.TaskReminderManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var taskRepository: TaskRepository

    @Inject
    lateinit var taskReminderManager: TaskReminderManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                val tasks = taskRepository.getAllTasks().first()
                tasks.filter { !it.isCompleted }.forEach { task ->
                    taskReminderManager.scheduleTaskReminders(task)
                }
            }
        }
    }
}
