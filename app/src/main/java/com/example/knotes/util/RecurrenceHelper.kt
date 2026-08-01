package com.example.knotes.util

import com.example.knotes.data.entity.Recurrence
import java.util.Calendar

object RecurrenceHelper {
    fun getNextOccurrence(currentDeadline: Long?, recurrence: Recurrence): Long? {
        if (currentDeadline == null || currentDeadline == 0L || recurrence == Recurrence.NONE) return null
        
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentDeadline
        
        // Ensure we move to a future date relative to current time if the deadline was in the past
        val now = Calendar.getInstance()
        if (calendar.before(now)) {
            // If the deadline passed, start from today or tomorrow
            calendar.timeInMillis = now.timeInMillis
        }

        when (recurrence) {
            Recurrence.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            Recurrence.WEEKDAYS -> {
                do {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                } while (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || 
                         calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
            }
            Recurrence.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            Recurrence.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            else -> return null
        }
        
        return calendar.timeInMillis
    }
}
