package com.exp.memoria.ui.chat

import java.util.UUID

/**
 * [聊天消息数据类]
 *
 * 代表聊天界面中的一条单独消息。
 *
 * @property id 一个唯一的标识符，用于在列表中高效地更新和识别消息。
 * @property text 消息的文本内容。
 * @property isFromUser 一个布尔值，如果消息来自用户，则为 true；如果来自 AI 模型，则为 false。
 */
data class ChatMessage(
    val id: UUID = UUID.randomUUID(),
    val text: String,
    val isFromUser: Boolean
)

/**
 * [聊天界面状态数据类]
 *
 * 代表了整个聊天界面的 UI 状态。ChatViewModel 会持有这个状态，
 * 并在状态变更时通知 UI (ChatScreen) 进行重绘。
 *
 * @property messages 当前聊天对话中的消息列表。
 * @property isLoading 一个布尔值，指示应用当前是否正在等待 AI 的响应。用于在界面上显示加载指示器。
 */
data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false
)
