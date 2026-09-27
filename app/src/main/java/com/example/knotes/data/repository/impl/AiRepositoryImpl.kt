package com.example.knotes.data.repository.impl

import android.graphics.Bitmap
import android.util.Log
import com.example.knotes.BuildConfig
import com.example.knotes.domain.repository.AiRepository
import com.example.knotes.util.AiPromptTemplates
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerationConfig
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class AiRepositoryImpl @Inject constructor() : AiRepository {

    private companion object {
        private const val TAG = "KNotes_GeminiAI"
    }

    private val isKeyConfigured: Boolean
        get() = BuildConfig.AI_API_KEY.isNotBlank()

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.AI_API_KEY.trim(),
            generationConfig = GenerationConfig.Builder().apply {
                temperature = 0.7f
                topK = 40
                topP = 0.95f
            }.build()
        )
    }

    override suspend fun summarize(text: String): Result<String> = withContext(Dispatchers.IO) {
        performGeminiGenerate(AiPromptTemplates.SUMMARIZE + text, "summarize")
    }

    override suspend fun improveGrammar(text: String): Result<String> = withContext(Dispatchers.IO) {
        performGeminiGenerate(AiPromptTemplates.IMPROVE_GRAMMAR + text, "improveGrammar")
    }

    override suspend fun rewrite(text: String, style: String): Result<String> = withContext(Dispatchers.IO) {
        val prompt = "${AiPromptTemplates.REWRITE_PREFIX}$style${AiPromptTemplates.REWRITE_SUFFIX}$text"
        performGeminiGenerate(prompt, "rewrite")
    }

    override suspend fun generateTags(text: String): Result<List<String>> = withContext(Dispatchers.IO) {
        performGeminiGenerate(AiPromptTemplates.GENERATE_TAGS + text, "generateTags").map { response ->
            response.split(",")
                .map { it.trim().removePrefix("#") }
                .filter { it.isNotBlank() }
        }
    }

    override suspend fun extractTasks(text: String): Result<List<String>> = withContext(Dispatchers.IO) {
        performGeminiGenerate(AiPromptTemplates.EXTRACT_TASKS + text, "extractTasks").map { response ->
            response.lines()
                .filter { it.trim().startsWith("-") || it.trim().startsWith("*") || it.trim().firstOrNull()?.isDigit() == true }
                .map { it.trim().removePrefix("-").removePrefix("*").trim() }
                .filter { it.isNotBlank() }
        }
    }

    override suspend fun generateTitle(text: String): Result<String> = withContext(Dispatchers.IO) {
        performGeminiGenerate(AiPromptTemplates.GENERATE_TITLE + text, "generateTitle").map {
            it.trim().removeSurrounding("\"").ifBlank { "Untitled Note" }
        }
    }

    override suspend fun askQuestion(noteContent: String, question: String): Result<String> = withContext(Dispatchers.IO) {
        val prompt = "${AiPromptTemplates.ASK_QUESTION_PREFIX}$noteContent${AiPromptTemplates.ASK_QUESTION_MID}$question${AiPromptTemplates.ASK_QUESTION_SUFFIX}"
        performGeminiGenerate(prompt, "askQuestion")
    }

    override suspend fun chat(
        history: List<Pair<String, String>>,
        message: String,
        context: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!isKeyConfigured) {
            Log.e(TAG, "AI_REQUEST_FAILED provider=Gemini reason=API key missing")
            return@withContext Result.failure(IllegalStateException("Gemini API key is not configured."))
        }

        try {
            Log.d(TAG, "AI_REQUEST_STARTED provider=Gemini action=chat")
            val chat = generativeModel.startChat(
                history = history.filter { it.first.isNotBlank() && it.second.isNotBlank() }.flatMap { (user, ai) ->
                    listOf(
                        content("user") { text(user) },
                        content("model") { text(ai) }
                    )
                }
            )
            val fullPrompt = if (context.isNotBlank()) {
                "Context (User's Notes):\n$context\n\nUser Message: $message"
            } else {
                message
            }
            val response = chat.sendMessage(fullPrompt)
            val text = response.text

            if (!text.isNullOrBlank()) {
                Log.d(TAG, "AI_REQUEST_SUCCESS provider=Gemini action=chat")
                Result.success(text.trim())
            } else {
                Log.w(TAG, "AI_REQUEST_FAILED provider=Gemini action=chat reason=Empty response")
                Result.failure(IllegalStateException("Received empty response from Gemini AI service."))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "AI_REQUEST_FAILED provider=Gemini action=chat error=${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun analyzeImage(bitmap: Bitmap, prompt: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isKeyConfigured) {
            Log.e(TAG, "AI_REQUEST_FAILED provider=Gemini action=analyzeImage reason=API key missing")
            return@withContext Result.failure(IllegalStateException("Gemini API key is not configured."))
        }

        try {
            Log.d(TAG, "AI_REQUEST_STARTED provider=Gemini action=analyzeImage")
            val inputContent = content {
                image(bitmap)
                text(prompt)
            }
            val response = generativeModel.generateContent(inputContent)
            val text = response.text

            if (!text.isNullOrBlank()) {
                Log.d(TAG, "AI_REQUEST_SUCCESS provider=Gemini action=analyzeImage")
                Result.success(text.trim())
            } else {
                Log.w(TAG, "AI_REQUEST_FAILED provider=Gemini action=analyzeImage reason=Empty response")
                Result.failure(IllegalStateException("Received empty response from Gemini image analysis."))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "AI_REQUEST_FAILED provider=Gemini action=analyzeImage error=${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun performGeminiGenerate(prompt: String, actionName: String): Result<String> {
        if (!isKeyConfigured) {
            Log.e(TAG, "AI_REQUEST_FAILED provider=Gemini action=$actionName reason=API key missing")
            return Result.failure(IllegalStateException("Gemini API key is not configured."))
        }

        return try {
            Log.d(TAG, "AI_REQUEST_STARTED provider=Gemini action=$actionName")
            val response = generativeModel.generateContent(prompt)
            val text = response.text

            if (!text.isNullOrBlank()) {
                Log.d(TAG, "AI_REQUEST_SUCCESS provider=Gemini action=$actionName")
                Result.success(text.trim())
            } else {
                Log.w(TAG, "AI_REQUEST_FAILED provider=Gemini action=$actionName reason=Empty response")
                Result.failure(IllegalStateException("Received empty response from Gemini AI service."))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "AI_REQUEST_FAILED provider=Gemini action=$actionName error=${e.message}")
            Result.failure(e)
        }
    }
}
