package com.example.knotes

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.knotes.util.NotificationHelper
import com.example.knotes.worker.TrashCleanupWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class KNotesApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        scheduleTrashCleanup()
        // Cancel legacy periodic workers to ensure notifications strictly respect the user's exact set alarm
        cancelOverdueCheckSpam()
    }

    private fun scheduleTrashCleanup() {
        val cleanupRequest = PeriodicWorkRequestBuilder<TrashCleanupWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "trash_cleanup",
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )
    }

    private fun cancelOverdueCheckSpam() {
        // Cancel hourly overdue worker and periodic daily reminder workers so only user's exact 6:00 PM alarms fire
        WorkManager.getInstance(this).cancelUniqueWork("overdue_check")
        WorkManager.getInstance(this).cancelUniqueWork("daily_reminder_morning")
        WorkManager.getInstance(this).cancelUniqueWork("daily_reminder_afternoon")
        WorkManager.getInstance(this).cancelUniqueWork("daily_reminder_evening")
    }
}
