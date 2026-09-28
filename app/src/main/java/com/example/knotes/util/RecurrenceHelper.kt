package com.example.knotes.util

import com.example.knotes.data.entity.Recurrence
import java.util.Calendar

object RecurrenceHelper {

    fun getNextOccurrence(currentScheduledTime: Long?, recurrence: Recurrence): Long? {
        if (currentScheduledTime == null || currentScheduledTime == 0L || recurrence == Recurrence.NONE) return null

        val targetCal = Calendar.getInstance()
        targetCal.timeInMillis = currentScheduledTime

        // Preserve exact hour, minute, second, millisecond
        val targetHour = targetCal.get(Calendar.HOUR_OF_DAY)
        val targetMinute = targetCal.get(Calendar.MINUTE)
        val targetSecond = targetCal.get(Calendar.SECOND)
        val targetMillis = targetCal.get(Calendar.MILLISECOND)

        val nowCal = Calendar.getInstance()

        // Advance date while preserving exact time of day until targetCal is strictly in the future
        do {
            when (recurrence) {
                Recurrence.DAILY -> targetCal.add(Calendar.DAY_OF_YEAR, 1)
                Recurrence.WEEKDAYS -> {
                    do {
                        targetCal.add(Calendar.DAY_OF_YEAR, 1)
                    } while (targetCal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                             targetCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                }
                Recurrence.WEEKLY -> targetCal.add(Calendar.WEEK_OF_YEAR, 1)
                Recurrence.MONTHLY -> targetCal.add(Calendar.MONTH, 1)
                else -> return null
            }
            // Ensure exact time of day is preserved
            targetCal.set(Calendar.HOUR_OF_DAY, targetHour)
            targetCal.set(Calendar.MINUTE, targetMinute)
            targetCal.set(Calendar.SECOND, targetSecond)
            targetCal.set(Calendar.MILLISECOND, targetMillis)
        } while (targetCal.before(nowCal))

        return targetCal.timeInMillis
    }
}
