package com.exp.memoria.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exp.memoria.domain.usecase.GetChatResponseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val getChatResponseUseCase: GetChatResponseUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatState())
    val uiState = _uiState.asStateFlow()

    fun sendMessage(query: String) {
        if (query.isBlank()) return

        // Add user's message to UI immediately
        _uiState.update { currentState ->
            currentState.copy(
                messages = currentState.messages + ChatMessage(text = query, isFromUser = true),
                isLoading = true
            )
        }

        // Launch a coroutine to get the AI response
        viewModelScope.launch {
            val response = getChatResponseUseCase(query)
            _uiState.update { currentState ->
                currentState.copy(
                    messages = currentState.messages + ChatMessage(text = response, isFromUser = false),
                    isLoading = false
                )
            }
        }
    }
}