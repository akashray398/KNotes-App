package com.example.knotes.domain.usecase.ai

import com.example.knotes.domain.repository.AiRepository
import javax.inject.Inject

class SummarizeNoteUseCase @Inject constructor(private val repository: AiRepository) {
    suspend operator fun invoke(text: String) = repository.summarize(text)
}

class ImproveGrammarUseCase @Inject constructor(private val repository: AiRepository) {
    suspend operator fun invoke(text: String) = repository.improveGrammar(text)
}

class RewriteNoteUseCase @Inject constructor(private val repository: AiRepository) {
    suspend operator fun invoke(text: String, style: String) = repository.rewrite(text, style)
}

class GenerateTagsUseCase @Inject constructor(private val repository: AiRepository) {
    suspend operator fun invoke(text: String) = repository.generateTags(text)
}

class ExtractTasksUseCase @Inject constructor(private val repository: AiRepository) {
    suspend operator fun invoke(text: String) = repository.extractTasks(text)
}

class GenerateTitleUseCase @Inject constructor(private val repository: AiRepository) {
    suspend operator fun invoke(text: String) = repository.generateTitle(text)
}

class AskAiQuestionUseCase @Inject constructor(private val repository: AiRepository) {
    suspend operator fun invoke(noteContent: String, question: String) = repository.askQuestion(noteContent, question)
}
