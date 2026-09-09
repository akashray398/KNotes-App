package com.example.knotes

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.knotes.util.NotificationHelper
import com.example.knotes.worker.DailyReminderWorker
import com.example.knotes.worker.OverdueCheckWorker
import com.example.knotes.worker.TrashCleanupWorker
import com.google.firebase.FirebaseApp
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
        scheduleDailyReminders()
        scheduleOverdueChecks()
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

    private fun scheduleDailyReminders() {
        // Schedule morning reminder (9 AM)
        val morningRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(calculateDelay(9, 0), TimeUnit.MILLISECONDS)
            .build()
        
        // Mid-day nudge (2 PM)
        val afternoonRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(calculateDelay(14, 0), TimeUnit.MILLISECONDS)
            .build()
        
        // Schedule evening reminder (7 PM)
        val eveningRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(calculateDelay(19, 0), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_reminder_morning",
            ExistingPeriodicWorkPolicy.UPDATE,
            morningRequest
        )
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_reminder_afternoon",
            ExistingPeriodicWorkPolicy.UPDATE,
            afternoonRequest
        )
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_reminder_evening",
            ExistingPeriodicWorkPolicy.UPDATE,
            eveningRequest
        )
    }

    private fun scheduleOverdueChecks() {
        // Run every hour for "Real-time" feel
        val overdueRequest = PeriodicWorkRequestBuilder<OverdueCheckWorker>(1, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "overdue_check",
            ExistingPeriodicWorkPolicy.UPDATE,
            overdueRequest
        )
    }

    private fun calculateDelay(hour: Int, minute: Int): Long {
        val calendar = java.util.Calendar.getInstance()
        val now = calendar.timeInMillis
        calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
        calendar.set(java.util.Calendar.MINUTE, minute)
        calendar.set(java.util.Calendar.SECOND, 0)
        
        if (calendar.timeInMillis <= now) {
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis - now
    }
}
