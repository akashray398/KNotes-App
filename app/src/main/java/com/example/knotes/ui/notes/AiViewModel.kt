package com.example.knotes.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.knotes.domain.usecase.ai.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiViewModel @Inject constructor(
    private val summarizeNoteUseCase: SummarizeNoteUseCase,
    private val improveGrammarUseCase: ImproveGrammarUseCase,
    private val rewriteNoteUseCase: RewriteNoteUseCase,
    private val generateTagsUseCase: GenerateTagsUseCase,
    private val extractTasksUseCase: ExtractTasksUseCase,
    private val generateTitleUseCase: GenerateTitleUseCase,
    private val askAiQuestionUseCase: AskAiQuestionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun summarize(text: String) = performAiAction { summarizeNoteUseCase(text) }
    
    fun improveGrammar(text: String) = performAiAction { improveGrammarUseCase(text) }
    
    fun rewrite(text: String, style: String) = performAiAction { rewriteNoteUseCase(text, style) }
    
    fun generateTags(text: String) = viewModelScope.launch {
        _uiState.value = AiUiState.Loading
        generateTagsUseCase(text).onSuccess { tags ->
            _uiState.value = AiUiState.TagsGenerated(tags)
        }.onFailure { e ->
            _uiState.value = AiUiState.Error(e.message ?: "Unknown error")
        }
    }
    
    fun extractTasks(text: String) = viewModelScope.launch {
        _uiState.value = AiUiState.Loading
        extractTasksUseCase(text).onSuccess { tasks ->
            _uiState.value = AiUiState.TasksExtracted(tasks)
        }.onFailure { e ->
            _uiState.value = AiUiState.Error(e.message ?: "Unknown error")
        }
    }
    
    fun generateTitle(text: String) = performAiAction { generateTitleUseCase(text) }
    
    fun askQuestion(noteContent: String, question: String) = performAiAction { 
        askAiQuestionUseCase(noteContent, question) 
    }

    private fun performAiAction(action: suspend () -> Result<String>) {
        viewModelScope.launch {
            _uiState.value = AiUiState.Loading
            action().onSuccess { result ->
                _uiState.value = AiUiState.Success(result)
            }.onFailure { e ->
                _uiState.value = AiUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetState() {
        _uiState.value = AiUiState.Idle
    }

    sealed class AiUiState {
        object Idle : AiUiState()
        object Loading : AiUiState()
        data class Success(val output: String) : AiUiState()
        data class TagsGenerated(val tags: List<String>) : AiUiState()
        data class TasksExtracted(val tasks: List<String>) : AiUiState()
        data class Error(val message: String) : AiUiState()
    }
}
