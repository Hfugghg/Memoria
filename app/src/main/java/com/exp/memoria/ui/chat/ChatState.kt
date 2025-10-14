package com.exp.memoria.ui.chat

// Represents a single message in the chat
data class ChatMessage(val text: String, val isFromUser: Boolean)

// Represents the entire state of the chat screen
data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false
)