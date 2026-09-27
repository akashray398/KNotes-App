package com.example.knotes.domain.usecase

import com.example.knotes.domain.repository.AiRepository
import com.example.knotes.domain.repository.NoteRepository
import com.example.knotes.ui.notes.AiChatViewModel
import com.example.knotes.util.AiErrorMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.MockitoAnnotations
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

@OptIn(ExperimentalCoroutinesApi::class)
class AiSystemTest {

    @Mock
    private lateinit var aiRepository: AiRepository

    @Mock
    private lateinit var noteRepository: NoteRepository

    private lateinit var aiChatUseCase: AiChatUseCase
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        aiChatUseCase = AiChatUseCase(aiRepository, noteRepository)
        `when`(noteRepository.getAllNotes()).thenReturn(flowOf(emptyList()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Test 1 - Normal request returns success response`() = runBlocking {
        val prompt = "Explain Kotlin coroutines."
        val expectedAnswer = "Kotlin coroutines are lightweight threads used for asynchronous programming."
        `when`(aiRepository.chat(emptyList(), prompt, "")).thenReturn(Result.success(expectedAnswer))

        val result = aiChatUseCase(emptyList(), prompt, includeNoteContext = false)

        assertTrue(result.isSuccess)
        assertEquals(expectedAnswer, result.getOrNull())
    }

    @Test
    fun `Test 2 - Empty input does not send request in ViewModel`() = runBlocking {
        val viewModel = AiChatViewModel(aiChatUseCase)

        viewModel.sendMessage("   ")

        assertTrue(viewModel.chatHistory.value.isEmpty())
        assertFalse(viewModel.isLoading.value)
        verifyNoInteractions(aiRepository)
    }

    @Test
    fun `Test 3 - Network unavailable returns user-friendly network error`() {
        val networkError = IOException("No internet connection")

        val userMessage = AiErrorMapper.mapErrorToUserMessage(networkError)

        assertTrue(userMessage.contains("internet connection", ignoreCase = true))
        assertTrue(AiErrorMapper.isTransientError(networkError))
    }

    @Test
    fun `Test 4 - Timeout returns user-friendly timeout error`() {
        val timeoutError = SocketTimeoutException("Read timed out")

        val userMessage = AiErrorMapper.mapErrorToUserMessage(timeoutError)

        assertTrue(userMessage.contains("timed out", ignoreCase = true))
        assertTrue(AiErrorMapper.isTransientError(timeoutError))
    }

    @Test
    fun `Test 5 - Rate limit 429 returns busy error and is marked transient`() {
        val response = Response.error<String>(429, "Rate limit exceeded".toResponseBody(null))
        val httpException = HttpException(response)

        val userMessage = AiErrorMapper.mapErrorToUserMessage(httpException)

        assertTrue(userMessage.contains("temporarily busy", ignoreCase = true))
        assertTrue(AiErrorMapper.isTransientError(httpException))
    }

    @Test
    fun `Test 6 - Invalid API config 401 returns configuration error and is non-transient`() {
        val response = Response.error<String>(401, "Unauthorized".toResponseBody(null))
        val httpException = HttpException(response)

        val userMessage = AiErrorMapper.mapErrorToUserMessage(httpException)

        assertTrue(userMessage.contains("configuration is invalid", ignoreCase = true))
        assertFalse(AiErrorMapper.isTransientError(httpException))
    }

    @Test
    fun `Test 7 - Rapid send taps ignore duplicate calls while loading`() = runBlocking {
        val viewModel = AiChatViewModel(aiChatUseCase)
        val prompt = "Tell me a joke"
        `when`(aiRepository.chat(emptyList(), prompt, "")).thenAnswer {
            runBlocking { delay(100) }
            Result.success("Why did the Kotlin dev smile? Expressive code!")
        }

        viewModel.sendMessage(prompt)
        // Rapid second tap while first is in loading state
        viewModel.sendMessage(prompt)

        testDispatcher.scheduler.advanceUntilIdle()

        // Only one user message was recorded because the second tap was blocked by _isLoading = true
        assertEquals(2, viewModel.chatHistory.value.size)
        assertEquals(prompt, viewModel.chatHistory.value[0].text)
        assertTrue(viewModel.chatHistory.value[0].isUser)
        assertFalse(viewModel.chatHistory.value[1].isUser)
    }
}
