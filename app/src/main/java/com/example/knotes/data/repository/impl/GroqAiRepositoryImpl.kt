package com.example.knotes.data.repository.impl

import android.graphics.Bitmap
import android.util.Log
import com.example.knotes.BuildConfig
import com.example.knotes.data.remote.GroqApiService
import com.example.knotes.data.remote.GroqMessage
import com.example.knotes.data.remote.GroqRequest
import com.example.knotes.domain.repository.AiRepository
import com.example.knotes.util.AiPromptTemplates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class GroqAiRepositoryImpl @Inject constructor(
    private val apiService: GroqApiService
) : AiRepository {

    private companion object {
        private const val TAG = "KNotes_GroqAI"
    }

    private val isKeyConfigured: Boolean
        get() = BuildConfig.GROQ_API_KEY.isNotBlank()

    private val authHeader: String
        get() = "Bearer ${BuildConfig.GROQ_API_KEY.trim()}"

    override suspend fun summarize(text: String): Result<String> = performGroqAction(AiPromptTemplates.SUMMARIZE + text)

    override suspend fun improveGrammar(text: String): Result<String> = performGroqAction(AiPromptTemplates.IMPROVE_GRAMMAR + text)

    override suspend fun rewrite(text: String, style: String): Result<String> {
        val prompt = "${AiPromptTemplates.REWRITE_PREFIX}$style${AiPromptTemplates.REWRITE_SUFFIX}$text"
        return performGroqAction(prompt)
    }

    override suspend fun generateTags(text: String): Result<List<String>> = withContext(Dispatchers.IO) {
        val prompt = AiPromptTemplates.GENERATE_TAGS + text
        performGroqAction(prompt).map { response ->
            response.split(",")
                .map { it.trim().removePrefix("#") }
                .filter { it.isNotBlank() }
        }
    }

    override suspend fun extractTasks(text: String): Result<List<String>> = withContext(Dispatchers.IO) {
        val prompt = AiPromptTemplates.EXTRACT_TASKS + text
        performGroqAction(prompt).map { response ->
            response.lines()
                .filter { it.trim().startsWith("-") || it.trim().startsWith("*") || it.trim().firstOrNull()?.isDigit() == true }
                .map { it.trim().removePrefix("-").removePrefix("*").trim() }
                .filter { it.isNotBlank() }
        }
    }

    override suspend fun generateTitle(text: String): Result<String> = performGroqAction(AiPromptTemplates.GENERATE_TITLE + text)

    override suspend fun askQuestion(noteContent: String, question: String): Result<String> {
        val prompt = "${AiPromptTemplates.ASK_QUESTION_PREFIX}$noteContent${AiPromptTemplates.ASK_QUESTION_MID}$question${AiPromptTemplates.ASK_QUESTION_SUFFIX}"
        return performGroqAction(prompt)
    }

    override suspend fun chat(
        history: List<Pair<String, String>>,
        message: String,
        context: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!isKeyConfigured) {
            Log.e(TAG, "AI_REQUEST_FAILED provider=Groq reason=API key missing")
            return@withContext Result.failure(IllegalStateException("Groq API key is not configured."))
        }

        try {
            Log.d(TAG, "AI_REQUEST_STARTED provider=Groq action=chat")
            val messages = mutableListOf<GroqMessage>()
            if (context.isNotBlank()) {
                messages.add(GroqMessage("system", "Context of user's notes: $context"))
            }
            
            history.forEach { (user, ai) ->
                if (user.isNotBlank()) messages.add(GroqMessage("user", user))
                if (ai.isNotBlank()) messages.add(GroqMessage("assistant", ai))
            }
            
            messages.add(GroqMessage("user", message))

            val request = GroqRequest(messages = messages)
            val response = apiService.getChatCompletion(authHeader, request)
            val content = response.choices.firstOrNull()?.message?.content

            if (!content.isNullOrBlank()) {
                Log.d(TAG, "AI_REQUEST_SUCCESS provider=Groq action=chat")
                Result.success(content.trim())
            } else {
                Log.w(TAG, "AI_REQUEST_FAILED provider=Groq action=chat reason=Empty response")
                Result.failure(IllegalStateException("Received empty response from Groq AI service."))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "AI_REQUEST_FAILED provider=Groq action=chat error=${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun analyzeImage(bitmap: Bitmap, prompt: String): Result<String> {
        return Result.failure(UnsupportedOperationException("Groq does not support multimodal image analysis. Please use Gemini for this feature."))
    }

    private suspend fun performGroqAction(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isKeyConfigured) {
            Log.e(TAG, "AI_REQUEST_FAILED provider=Groq reason=API key missing")
            return@withContext Result.failure(IllegalStateException("Groq API key is not configured."))
        }

        try {
            Log.d(TAG, "AI_REQUEST_STARTED provider=Groq action=performAction")
            val request = GroqRequest(
                messages = listOf(GroqMessage("user", prompt))
            )
            val response = apiService.getChatCompletion(authHeader, request)
            val content = response.choices.firstOrNull()?.message?.content

            if (!content.isNullOrBlank()) {
                Log.d(TAG, "AI_REQUEST_SUCCESS provider=Groq action=performAction")
                Result.success(content.trim())
            } else {
                Log.w(TAG, "AI_REQUEST_FAILED provider=Groq action=performAction reason=Empty response")
                Result.failure(IllegalStateException("Received empty response from Groq AI service."))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "AI_REQUEST_FAILED provider=Groq action=performAction error=${e.message}")
            Result.failure(e)
        }
    }
}
