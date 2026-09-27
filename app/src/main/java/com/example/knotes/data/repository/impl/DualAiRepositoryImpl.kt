package com.example.knotes.data.repository.impl

import android.graphics.Bitmap
import android.util.Log
import com.example.knotes.BuildConfig
import com.example.knotes.domain.repository.AiRepository
import com.example.knotes.util.AiErrorMapper
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DualAiRepositoryImpl @Inject constructor(
    private val groqAiRepository: GroqAiRepositoryImpl,
    private val geminiAiRepository: AiRepositoryImpl
) : AiRepository {

    private companion object {
        private const val TAG = "KNotes_AI_Router"
        private const val MAX_RETRIES = 2
        private const val INITIAL_BACKOFF_MS = 1000L
    }

    private val isGroqAvailable: Boolean
        get() = BuildConfig.GROQ_API_KEY.isNotBlank()

    private val isGeminiAvailable: Boolean
        get() = BuildConfig.AI_API_KEY.isNotBlank()

    override suspend fun summarize(text: String): Result<String> {
        return executeWithFallback("summarize") { provider ->
            provider.summarize(text)
        }
    }

    override suspend fun improveGrammar(text: String): Result<String> {
        return executeWithFallback("improveGrammar") { provider ->
            provider.improveGrammar(text)
        }
    }

    override suspend fun rewrite(text: String, style: String): Result<String> {
        return executeWithFallback("rewrite") { provider ->
            provider.rewrite(text, style)
        }
    }

    override suspend fun generateTags(text: String): Result<List<String>> {
        return executeWithFallbackList("generateTags") { provider ->
            provider.generateTags(text)
        }
    }

    override suspend fun extractTasks(text: String): Result<List<String>> {
        return executeWithFallbackList("extractTasks") { provider ->
            provider.extractTasks(text)
        }
    }

    override suspend fun generateTitle(text: String): Result<String> {
        return executeWithFallback("generateTitle") { provider ->
            provider.generateTitle(text)
        }
    }

    override suspend fun askQuestion(noteContent: String, question: String): Result<String> {
        return executeWithFallback("askQuestion") { provider ->
            provider.askQuestion(noteContent, question)
        }
    }

    override suspend fun chat(
        history: List<Pair<String, String>>,
        message: String,
        context: String
    ): Result<String> {
        return executeWithFallback("chat") { provider ->
            provider.chat(history, message, context)
        }
    }

    override suspend fun analyzeImage(bitmap: Bitmap, prompt: String): Result<String> {
        Log.d(TAG, "AI_PROVIDER_SELECTED action=analyzeImage primary=Gemini")
        if (!isGeminiAvailable) {
            val err = IllegalStateException("Gemini API key is not configured for image analysis.")
            return Result.failure(Exception(AiErrorMapper.mapErrorToUserMessage(err), err))
        }

        return try {
            val result = geminiAiRepository.analyzeImage(bitmap, prompt)
            if (result.isSuccess) {
                result
            } else {
                val e = result.exceptionOrNull() ?: Exception("Unknown image analysis error")
                Result.failure(Exception(AiErrorMapper.mapErrorToUserMessage(e), e))
            }
        } catch (e: Exception) {
            Result.failure(Exception(AiErrorMapper.mapErrorToUserMessage(e), e))
        }
    }

    private suspend fun executeWithFallback(
        actionName: String,
        call: suspend (AiRepository) -> Result<String>
    ): Result<String> {
        val (primary, secondary, primaryName, secondaryName) = getProviders()

        if (primary == null) {
            val err = IllegalStateException("No AI provider API key is configured. Please add GROQ_API_KEY or AI_API_KEY to local.properties.")
            Log.e(TAG, "AI_REQUEST_FAILED action=$actionName reason=No keys configured")
            return Result.failure(Exception(AiErrorMapper.mapErrorToUserMessage(err), err))
        }

        Log.d(TAG, "AI_PROVIDER_SELECTED action=$actionName primary=$primaryName secondary=$secondaryName")

        // Try primary provider with retry
        var primaryResult = callWithRetry(primaryName, actionName) { call(primary) }
        if (primaryResult.isSuccess) {
            return primaryResult
        }

        val primaryError = primaryResult.exceptionOrNull()
        Log.w(TAG, "AI_PRIMARY_FAILED provider=$primaryName action=$actionName error=${primaryError?.message}")

        // Check if secondary fallback provider is available
        if (secondary != null) {
            Log.i(TAG, "AI_FALLBACK from=$primaryName to=$secondaryName action=$actionName")
            val secondaryResult = callWithRetry(secondaryName, actionName) { call(secondary) }
            if (secondaryResult.isSuccess) {
                return secondaryResult
            }
            val secondaryError = secondaryResult.exceptionOrNull()
            Log.e(TAG, "AI_FALLBACK_FAILED provider=$secondaryName action=$actionName error=${secondaryError?.message}")
            
            val finalMsg = AiErrorMapper.mapErrorToUserMessage(secondaryError ?: primaryError ?: Exception("AI request failed."))
            return Result.failure(Exception(finalMsg, secondaryError ?: primaryError))
        }

        val finalMsg = AiErrorMapper.mapErrorToUserMessage(primaryError ?: Exception("AI request failed."))
        return Result.failure(Exception(finalMsg, primaryError))
    }

    private suspend fun executeWithFallbackList(
        actionName: String,
        call: suspend (AiRepository) -> Result<List<String>>
    ): Result<List<String>> {
        val (primary, secondary, primaryName, secondaryName) = getProviders()

        if (primary == null) {
            val err = IllegalStateException("No AI provider API key is configured.")
            return Result.failure(Exception(AiErrorMapper.mapErrorToUserMessage(err), err))
        }

        Log.d(TAG, "AI_PROVIDER_SELECTED action=$actionName primary=$primaryName secondary=$secondaryName")

        val primaryResult = callWithRetryList(primaryName, actionName) { call(primary) }
        if (primaryResult.isSuccess) {
            return primaryResult
        }

        val primaryError = primaryResult.exceptionOrNull()

        if (secondary != null) {
            Log.i(TAG, "AI_FALLBACK from=$primaryName to=$secondaryName action=$actionName")
            val secondaryResult = callWithRetryList(secondaryName, actionName) { call(secondary) }
            if (secondaryResult.isSuccess) {
                return secondaryResult
            }
            val secondaryError = secondaryResult.exceptionOrNull()
            val finalMsg = AiErrorMapper.mapErrorToUserMessage(secondaryError ?: primaryError ?: Exception("AI request failed."))
            return Result.failure(Exception(finalMsg, secondaryError ?: primaryError))
        }

        val finalMsg = AiErrorMapper.mapErrorToUserMessage(primaryError ?: Exception("AI request failed."))
        return Result.failure(Exception(finalMsg, primaryError))
    }

    private suspend fun callWithRetry(
        providerName: String,
        actionName: String,
        block: suspend () -> Result<String>
    ): Result<String> {
        var currentDelay = INITIAL_BACKOFF_MS
        var lastResult: Result<String> = Result.failure(Exception("Not started"))

        for (attempt in 0..MAX_RETRIES) {
            if (attempt > 0) {
                Log.d(TAG, "AI_RETRY provider=$providerName action=$actionName attempt=$attempt delayMs=$currentDelay")
                delay(currentDelay)
                currentDelay *= 2
            }

            lastResult = try {
                block()
            } catch (e: Exception) {
                Result.failure(e)
            }

            if (lastResult.isSuccess) {
                return lastResult
            }

            val e = lastResult.exceptionOrNull()
            if (e != null && !AiErrorMapper.isTransientError(e)) {
                // Non-transient error, do not retry further on this provider
                break
            }
        }
        return lastResult
    }

    private suspend fun callWithRetryList(
        providerName: String,
        actionName: String,
        block: suspend () -> Result<List<String>>
    ): Result<List<String>> {
        var currentDelay = INITIAL_BACKOFF_MS
        var lastResult: Result<List<String>> = Result.failure(Exception("Not started"))

        for (attempt in 0..MAX_RETRIES) {
            if (attempt > 0) {
                Log.d(TAG, "AI_RETRY provider=$providerName action=$actionName attempt=$attempt delayMs=$currentDelay")
                delay(currentDelay)
                currentDelay *= 2
            }

            lastResult = try {
                block()
            } catch (e: Exception) {
                Result.failure(e)
            }

            if (lastResult.isSuccess) {
                return lastResult
            }

            val e = lastResult.exceptionOrNull()
            if (e != null && !AiErrorMapper.isTransientError(e)) {
                break
            }
        }
        return lastResult
    }

    private data class ProviderTuple(
        val primary: AiRepository?,
        val secondary: AiRepository?,
        val primaryName: String,
        val secondaryName: String
    )

    private fun getProviders(): ProviderTuple {
        return when {
            isGroqAvailable && isGeminiAvailable -> ProviderTuple(groqAiRepository, geminiAiRepository, "Groq", "Gemini")
            isGroqAvailable -> ProviderTuple(groqAiRepository, null, "Groq", "None")
            isGeminiAvailable -> ProviderTuple(geminiAiRepository, null, "Gemini", "None")
            else -> ProviderTuple(null, null, "None", "None")
        }
    }
}
