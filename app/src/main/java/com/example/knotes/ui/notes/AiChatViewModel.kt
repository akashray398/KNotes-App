package com.example.knotes.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.knotes.domain.usecase.AiChatUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val aiChatUseCase: AiChatUseCase
) : ViewModel() {

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory = _chatHistory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun sendMessage(message: String) {
        val trimmed = message.trim()
        if (trimmed.isEmpty() || _isLoading.value) return

        // Set loading state synchronously to prevent duplicate rapid requests
        _isLoading.value = true
        val userMessage = ChatMessage(trimmed, isUser = true)
        val currentList = _chatHistory.value + userMessage
        _chatHistory.value = currentList

        viewModelScope.launch {
            try {
                // Build valid history excluding error messages and the newly appended user message
                val validHistoryMessages = currentList.dropLast(1).filter { !it.isError }
                
                val historyPairs = validHistoryMessages
                    .chunked(2)
                    .mapNotNull { pair ->
                        if (pair.size == 2 && pair[0].isUser && !pair[1].isUser) {
                            Pair(pair[0].text, pair[1].text)
                        } else null
                    }

                val result = aiChatUseCase(historyPairs, trimmed)
                
                result.onSuccess { response ->
                    _chatHistory.value = _chatHistory.value + ChatMessage(response, isUser = false)
                }.onFailure { e ->
                    val errorText = e.message ?: "Unable to process AI request."
                    _chatHistory.value = _chatHistory.value + ChatMessage(errorText, isUser = false, isError = true)
                }
            } catch (e: Exception) {
                _chatHistory.value = _chatHistory.value + ChatMessage(e.message ?: "An error occurred.", isUser = false, isError = true)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearChat() {
        if (!_isLoading.value) {
            _chatHistory.value = emptyList()
        }
    }

    data class ChatMessage(
        val text: String,
        val isUser: Boolean,
        val isError: Boolean = false,
        val timestamp: Long = System.currentTimeMillis()
    )
}
