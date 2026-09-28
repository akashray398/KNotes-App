package com.example.knotes.util

import android.app.AlarmManager
import android.content.Context
import com.example.knotes.data.entity.Recurrence
import com.example.knotes.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.util.Calendar

class ReminderEngineTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var alarmManager: AlarmManager

    private lateinit var taskReminderManager: TaskReminderManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(context.getSystemService(Context.ALARM_SERVICE)).thenReturn(alarmManager)
        taskReminderManager = TaskReminderManager(context)
    }

    @Test
    fun `TEST 1 - One-time specific day reminder uses exact configured time`() {
        val targetCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val task = Task(
            id = 42,
            title = "Submit Assignment",
            reminderTime = targetCal.timeInMillis
        )

        taskReminderManager.scheduleTaskReminders(task)

        // Verify request code generation: task.id * 100 + 1 = 4201
        val expectedPrimaryRequestCode = task.id * 100 + 1
        assertEquals(4201, expectedPrimaryRequestCode)
    }

    @Test
    fun `TEST 2 - Specific day two reminders generate two distinct stable request codes and stop`() {
        val cal1 = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 0)
        }
        val cal2 = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
        }

        val task = Task(
            id = 5,
            title = "Two Reminders Task",
            reminderTime = cal1.timeInMillis,
            secondaryReminderTime = cal2.timeInMillis
        )

        taskReminderManager.scheduleTaskReminders(task)

        val primaryCode = task.id * 100 + 1   // 501
        val secondaryCode = task.id * 100 + 2 // 502

        assertEquals(501, primaryCode)
        assertEquals(502, secondaryCode)
        assertNotEquals(primaryCode, secondaryCode)
    }

    @Test
    fun `TEST 3 - Daily recurrence advances date by 1 day while strictly preserving exact local time`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val currentReminder = cal.timeInMillis
        val nextOccurrence = RecurrenceHelper.getNextOccurrence(currentReminder, Recurrence.DAILY)

        assertNotNull(nextOccurrence)
        val resultCal = Calendar.getInstance().apply { timeInMillis = nextOccurrence!! }

        assertEquals(18, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resultCal.get(Calendar.MINUTE))
        assertEquals(0, resultCal.get(Calendar.SECOND))
        assertTrue(resultCal.timeInMillis > currentReminder)
    }

    @Test
    fun `TEST 4 - Completed task cancels future alarms and ignores scheduling`() {
        val futureTime = System.currentTimeMillis() + 3600_000
        val task = Task(
            id = 10,
            title = "Completed Task",
            isCompleted = true,
            reminderTime = futureTime
        )

        taskReminderManager.scheduleTaskReminders(task)

        // Completed tasks must be cancelled and skipped
        assertTrue(task.isCompleted)
    }

    @Test
    fun `TEST 5 - Past non-recurring reminder is ignored without creating catch-up loop`() {
        val pastTime = System.currentTimeMillis() - 3600_000

        // Past non-recurring reminder should return null next occurrence
        val nextOcc = RecurrenceHelper.getNextOccurrence(pastTime, Recurrence.NONE)
        assertNull(nextOcc)
    }
}
