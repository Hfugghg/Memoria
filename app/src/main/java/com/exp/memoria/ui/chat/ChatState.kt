package com.exp.memoria.ui.chat

// 表示聊天中的单条消息
data class ChatMessage(val text: String, val isFromUser: Boolean)

// 表示聊天屏幕的整个状态
data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false
)
