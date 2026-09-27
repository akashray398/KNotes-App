package com.example.knotes.domain.usecase

import com.example.knotes.domain.repository.AiRepository
import com.example.knotes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AiChatUseCase @Inject constructor(
    private val aiRepository: AiRepository,
    private val noteRepository: NoteRepository
) {
    suspend operator fun invoke(
        history: List<Pair<String, String>>,
        message: String,
        includeNoteContext: Boolean = true
    ): Result<String> {
        val context = if (includeNoteContext) {
            val notes = noteRepository.getAllNotes().first()
            notes.joinToString("\n---\n") { "Title: ${it.title}\nContent: ${it.content}" }
        } else ""
        
        return aiRepository.chat(history, message, context)
    }
}
