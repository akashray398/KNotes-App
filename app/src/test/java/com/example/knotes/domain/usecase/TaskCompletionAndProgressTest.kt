package com.example.knotes.domain.usecase

import com.example.knotes.domain.model.Task
import com.example.knotes.domain.repository.TaskRepository
import com.example.knotes.ui.tasks.TasksViewModel
import com.example.knotes.util.StreakManager
import com.example.knotes.util.TaskReminderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class TaskCompletionAndProgressTest {

    @Mock
    private lateinit var taskRepository: TaskRepository

    @Mock
    private lateinit var taskReminderManager: TaskReminderManager

    @Mock
    private lateinit var streakManager: StreakManager

    private lateinit var toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase
    private lateinit var viewModel: TasksViewModel
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        toggleTaskCompletionUseCase = ToggleTaskCompletionUseCase(taskRepository)
        `when`(taskRepository.getCompletedTasksCount()).thenReturn(flowOf(2))
        `when`(taskRepository.getPendingTasksCount()).thenReturn(flowOf(3))
        `when`(taskRepository.getAllTasks()).thenReturn(flowOf(emptyList()))

        viewModel = TasksViewModel(
            repository = taskRepository,
            toggleTaskCompletionUseCase = toggleTaskCompletionUseCase,
            taskReminderManager = taskReminderManager,
            streakManager = streakManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `TEST 1 - Complete task calls repository with task ID and new state`() = runBlocking {
        val taskId = 101
        toggleTaskCompletionUseCase(taskId, true)

        verify(taskRepository).updateTaskCompletion(taskId, true)
    }

    @Test
    fun `TEST 2 - Uncomplete task calls repository with isCompleted false`() = runBlocking {
        val taskId = 101
        toggleTaskCompletionUseCase(taskId, false)

        verify(taskRepository).updateTaskCompletion(taskId, false)
    }

    @Test
    fun `TEST 3 - Progress calculation formula returns correct percentage`() = runBlocking {
        testDispatcher.scheduler.advanceUntilIdle()

        // 2 completed out of 5 total = 40%
        val stats = viewModel.productivityStats.value
        val completed = stats.first
        val pending = stats.second
        val percentage = stats.third

        assertEquals(2, completed)
        assertEquals(3, pending)
        assertEquals(40, percentage)
    }

    @Test
    fun `TEST 4 - Zero tasks returns 0 percentage without division by zero`() = runBlocking {
        `when`(taskRepository.getCompletedTasksCount()).thenReturn(flowOf(0))
        `when`(taskRepository.getPendingTasksCount()).thenReturn(flowOf(0))

        val zeroViewModel = TasksViewModel(
            repository = taskRepository,
            toggleTaskCompletionUseCase = toggleTaskCompletionUseCase,
            taskReminderManager = taskReminderManager,
            streakManager = streakManager
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val stats = zeroViewModel.productivityStats.value
        assertEquals(0, stats.first)
        assertEquals(0, stats.second)
        assertEquals(0, stats.third) // 0%
    }

    @Test
    fun `TEST 5 - Toggling completion on incomplete task cancels pending reminders`() = runBlocking {
        val task = Task(id = 55, title = "Test Task", isCompleted = false)

        viewModel.toggleTaskCompletion(task)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(taskRepository).updateTaskCompletion(55, true)
        verify(taskReminderManager).cancelTaskReminders(task)
    }

    @Test
    fun `TEST 6 - Rapid taps on same task ID are debounced`() = runBlocking {
        val task = Task(id = 77, title = "Rapid Tap Task", isCompleted = false)

        viewModel.toggleTaskCompletion(task)
        // Rapid second tap while first is in-flight
        viewModel.toggleTaskCompletion(task)

        testDispatcher.scheduler.advanceUntilIdle()

        // updateTaskCompletion should only be invoked once
        verify(taskRepository).updateTaskCompletion(77, true)
    }
}
