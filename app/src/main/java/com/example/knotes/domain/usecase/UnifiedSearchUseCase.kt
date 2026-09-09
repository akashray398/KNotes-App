package com.example.knotes.domain.usecase

import com.example.knotes.domain.model.SearchResult
import com.example.knotes.domain.repository.NoteRepository
import com.example.knotes.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class UnifiedSearchUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
    private val taskRepository: TaskRepository
) {
    operator fun invoke(query: String): Flow<List<SearchResult>> {
        if (query.isBlank()) return kotlinx.coroutines.flow.flowOf(emptyList())
        
        return combine(
            noteRepository.searchNotes(query),
            taskRepository.searchTasks(query)
        ) { notes, tasks ->
            val results = mutableListOf<SearchResult>()
            results.addAll(notes.map { SearchResult.NoteResult(it) })
            results.addAll(tasks.map { SearchResult.TaskResult(it) })
            
            results.sortedByDescending { 
                when (it) {
                    is SearchResult.NoteResult -> it.note.updatedTime
                    is SearchResult.TaskResult -> it.task.updatedTime
                }
            }
        }
    }
}
