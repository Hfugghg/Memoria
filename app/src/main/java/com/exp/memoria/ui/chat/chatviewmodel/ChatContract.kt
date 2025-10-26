package com.exp.memoria.ui.chat.chatviewmodel

import android.net.Uri
import java.util.UUID

/**
 * [聊天消息数据类]
 *
 * 代表聊天界面中的一条单独消息。
 *
 * @property id 一个唯一的标识符，用于在列表中高效地更新和识别消息。
 * @property text 消息的文本内容。
 * @property isFromUser 一个布尔值，如果消息来自用户，则为 true；如果来自 AI 模型，则为 false。
 * @property memoryId 对应于数据库中 `RawMemory` 表的 `id`。这对于更新或删除操作至-关重要。
 *                    对于尚未存入数据库的新消息（例如，用户刚输入但未发送的消息），此值为 null。
 * @property attachments 附加到消息的文件URI列表。
 * @property summary 消息的摘要。
 */
data class ChatMessage(
    val id: UUID = UUID.randomUUID(),
    val text: String,
    val isFromUser: Boolean,
    val memoryId: Long? = null,
    val attachments: List<Uri> = emptyList(),
    val summary: String? = null
)

/**
 * [聊天界面状态数据类]
 *
 * 代表了整个聊天界面的 UI 状态。ChatViewModel 会持有这个状态，
 * 并在状态变更时通知 UI (ChatScreen) 进行重绘。
 *
 * @property messages 当前聊天对话中的消息列表。
 * @property isLoading 一个布尔值，指示应用当前是否正在等待 AI 的响应。用于在界面上显示加载指示器。
 * @property selectedFiles 用户选择的要附加到下一条消息的文件URI列表。
 */
data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val selectedFiles: List<Uri> = emptyList()
)
