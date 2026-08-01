package com.example.knotes.util

import com.example.knotes.data.repository.TaskRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreakManager @Inject constructor(
    private val settingsManager: SettingsManager,
    private val taskRepository: TaskRepository
) {
    suspend fun checkAndUpdateStreak() {
        val today = getStartOfDay()
        val lastProductiveDate = settingsManager.lastStreakDate.first()
        val currentStreak = settingsManager.currentStreak.first()

        // Check if all tasks due today are completed
        val pendingDueToday = taskRepository.getDueTodayCount(today, getEndOfDay(today)).first()
        val completedToday = taskRepository.getCompletedTodayCount(today).first()
        
        if (pendingDueToday == 0 && completedToday > 0) {
            // Check if we already updated today
            if (lastProductiveDate == today) return

            if (lastProductiveDate == getYesterday(today)) {
                // Continued from yesterday
                settingsManager.updateStreak(currentStreak + 1, today)
            } else {
                // Streak broken or just starting
                settingsManager.updateStreak(1, today)
            }
        }
    }

    suspend fun validateStreak() {
        val today = getStartOfDay()
        val yesterday = getYesterday(today)
        val lastProductiveDate = settingsManager.lastStreakDate.first()

        if (lastProductiveDate != today && lastProductiveDate != yesterday) {
            // Streak broken
            settingsManager.updateStreak(0, lastProductiveDate)
        }
    }

    private fun getStartOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfDay(startOfDay: Long): Long {
        return startOfDay + 24 * 60 * 60 * 1000 - 1
    }

    private fun getYesterday(startOfDay: Long): Long {
        return startOfDay - 24 * 60 * 60 * 1000
    }
}
