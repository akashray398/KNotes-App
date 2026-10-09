package com.example.knotes.domain.usecase

import com.example.knotes.domain.model.Note
import com.example.knotes.domain.model.Task
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class DailyActivity(
    val dateMillis: Long,
    val dayLabel: String,
    val completedTasks: Int,
    val totalTasks: Int,
    val level: Int // 0 to 4
)

data class AchievementBadge(
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean
)

data class ProductivityAnalyticsReport(
    val weeklyScorePercent: Int,
    val weeklyCompletedCount: Int,
    val weeklyTotalCount: Int,
    val dailyActivities: List<DailyActivity>,
    val heatmapGrid: List<DailyActivity>,
    val peakWindowLabel: String,
    val peakWindowCount: Int,
    val badges: List<AchievementBadge>,
    val currentStreak: Int,
    val highestStreak: Int,
    val totalNotes: Int
)

class GetProductivityAnalyticsUseCase @Inject constructor() {

    operator fun invoke(
        notes: List<Note>,
        tasks: List<Task>,
        currentStreak: Int,
        highestStreak: Int
    ): ProductivityAnalyticsReport {
        val now = System.currentTimeMillis()

        // 1. Calculate Past 7 Days Activity for Bar Chart
        val dailyActivities = mutableListOf<DailyActivity>()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

        for (i in 6 downTo 0) {
            val calDay = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = calDay.timeInMillis
            val endOfDay = startOfDay + 86400000L - 1L

            val completedOnDay = tasks.count { task ->
                val compTime = task.lastCompletedTime ?: if (task.isCompleted) task.updatedTime else 0L
                compTime in startOfDay..endOfDay
            }

            val totalOnDay = tasks.count { task ->
                val targetTime = task.deadline ?: task.reminderTime ?: task.createdTime
                targetTime in startOfDay..endOfDay
            }

            val count = maxOf(completedOnDay, totalOnDay)
            val level = when {
                count >= 5 -> 4
                count >= 3 -> 3
                count >= 2 -> 2
                count >= 1 -> 1
                else -> 0
            }

            dailyActivities.add(
                DailyActivity(
                    dateMillis = startOfDay,
                    dayLabel = dayFormat.format(Date(startOfDay)),
                    completedTasks = completedOnDay,
                    totalTasks = maxOf(totalOnDay, completedOnDay),
                    level = level
                )
            )
        }

        // 2. Calculate Past 30 Days Heatmap Grid
        val heatmapGrid = mutableListOf<DailyActivity>()
        val shortDayFormat = SimpleDateFormat("d", Locale.getDefault())

        for (i in 29 downTo 0) {
            val calDay = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = calDay.timeInMillis
            val endOfDay = startOfDay + 86400000L - 1L

            val completedOnDay = tasks.count { task ->
                val compTime = task.lastCompletedTime ?: if (task.isCompleted) task.updatedTime else 0L
                compTime in startOfDay..endOfDay
            }

            val level = when {
                completedOnDay >= 5 -> 4
                completedOnDay >= 3 -> 3
                completedOnDay >= 2 -> 2
                completedOnDay >= 1 -> 1
                else -> 0
            }

            heatmapGrid.add(
                DailyActivity(
                    dateMillis = startOfDay,
                    dayLabel = shortDayFormat.format(Date(startOfDay)),
                    completedTasks = completedOnDay,
                    totalTasks = completedOnDay,
                    level = level
                )
            )
        }

        // 3. Weekly Summary Totals
        val weeklyCompleted = dailyActivities.sumOf { it.completedTasks }
        val weeklyTotal = dailyActivities.sumOf { it.totalTasks }
        val weeklyPercent = if (weeklyTotal > 0) minOf(100, ((weeklyCompleted.toFloat() / weeklyTotal.toFloat()) * 100).toInt()) else 0

        // 4. Calculate Peak Productivity Window of Day
        var morningCount = 0
        var afternoonCount = 0
        var eveningCount = 0
        var nightCount = 0

        tasks.forEach { task ->
            val time = task.lastCompletedTime ?: task.updatedTime
            if (task.isCompleted && time > 0L) {
                val cal = Calendar.getInstance().apply { timeInMillis = time }
                when (cal.get(Calendar.HOUR_OF_DAY)) {
                    in 5..11 -> morningCount++
                    in 12..16 -> afternoonCount++
                    in 17..21 -> eveningCount++
                    else -> nightCount++
                }
            }
        }

        val windowMap = mapOf(
            "🌅 Morning (5 AM - 12 PM)" to morningCount,
            "☀️ Afternoon (12 PM - 5 PM)" to afternoonCount,
            "🌆 Evening (5 PM - 10 PM)" to eveningCount,
            "🌙 Night (10 PM - 5 AM)" to nightCount
        )
        val peakEntry = windowMap.maxByOrNull { it.value } ?: windowMap.entries.first()

        // 5. Achievement Badges
        val totalCompletedAllTime = tasks.count { it.isCompleted }
        val badges = listOf(
            AchievementBadge("First Step 🎯", "Completed your first task", "🎯", totalCompletedAllTime >= 1),
            AchievementBadge("3-Day Spark 🔥", "Maintained a 3-day streak", "🔥", currentStreak >= 3 || highestStreak >= 3),
            AchievementBadge("Week Warrior 👑", "Reached a 7-day streak", "👑", currentStreak >= 7 || highestStreak >= 7),
            AchievementBadge("Task Crusher ⚡", "Completed 10+ tasks", "⚡", totalCompletedAllTime >= 10),
            AchievementBadge("Knowledge Master 🧠", "Created 5+ notes", "🧠", notes.size >= 5)
        )

        return ProductivityAnalyticsReport(
            weeklyScorePercent = weeklyPercent,
            weeklyCompletedCount = weeklyCompleted,
            weeklyTotalCount = maxOf(weeklyTotal, weeklyCompleted),
            dailyActivities = dailyActivities,
            heatmapGrid = heatmapGrid,
            peakWindowLabel = peakEntry.key,
            peakWindowCount = peakEntry.value,
            badges = badges,
            currentStreak = currentStreak,
            highestStreak = highestStreak,
            totalNotes = notes.size
        )
    }
}
