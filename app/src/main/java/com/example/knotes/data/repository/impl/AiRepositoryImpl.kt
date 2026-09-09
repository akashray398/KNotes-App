package com.example.knotes.data.repository.impl

import com.example.knotes.BuildConfig
import com.example.knotes.domain.repository.AiRepository
import com.example.knotes.util.AiPromptTemplates
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepositoryImpl @Inject constructor() : AiRepository {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.AI_API_KEY,
        generationConfig = GenerationConfig.Builder().apply {
            temperature = 0.7f
            topK = 40
            topP = 0.95f
        }.build()
    )

    override suspend fun summarize(text: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = generativeModel.generateContent(AiPromptTemplates.SUMMARIZE + text)
            Result.success(response.text ?: "No summary generated")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun improveGrammar(text: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = generativeModel.generateContent(AiPromptTemplates.IMPROVE_GRAMMAR + text)
            Result.success(response.text ?: text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rewrite(text: String, style: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = "${AiPromptTemplates.REWRITE_PREFIX}$style${AiPromptTemplates.REWRITE_SUFFIX}$text"
            val response = generativeModel.generateContent(prompt)
            Result.success(response.text ?: text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateTags(text: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val response = generativeModel.generateContent(AiPromptTemplates.GENERATE_TAGS + text)
            val tags = response.text?.split(",")?.map { it.trim().removePrefix("#") } ?: emptyList()
            Result.success(tags)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun extractTasks(text: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val response = generativeModel.generateContent(AiPromptTemplates.EXTRACT_TASKS + text)
            val tasks = response.text?.lines()
                ?.filter { it.trim().startsWith("-") || it.trim().startsWith("*") || it.trim().firstOrNull()?.isDigit() == true }
                ?.map { it.trim().removePrefix("-").removePrefix("*").trim() } ?: emptyList()
            Result.success(tasks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateTitle(text: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = generativeModel.generateContent(AiPromptTemplates.GENERATE_TITLE + text)
            Result.success(response.text?.trim()?.removeSurrounding("\"") ?: "Untitled Note")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun askQuestion(noteContent: String, question: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = "${AiPromptTemplates.ASK_QUESTION_PREFIX}$noteContent${AiPromptTemplates.ASK_QUESTION_MID}$question${AiPromptTemplates.ASK_QUESTION_SUFFIX}"
            val response = generativeModel.generateContent(prompt)
            Result.success(response.text ?: "I couldn't find an answer in the note.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
