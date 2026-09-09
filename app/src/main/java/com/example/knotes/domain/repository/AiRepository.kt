package com.example.knotes.domain.repository

import kotlinx.coroutines.flow.Flow

interface AiRepository {
    suspend fun summarize(text: String): Result<String>
    suspend fun improveGrammar(text: String): Result<String>
    suspend fun rewrite(text: String, style: String): Result<String>
    suspend fun generateTags(text: String): Result<List<String>>
    suspend fun extractTasks(text: String): Result<List<String>>
    suspend fun generateTitle(text: String): Result<String>
    suspend fun askQuestion(noteContent: String, question: String): Result<String>
}
