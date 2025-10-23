package com.exp.memoria.ui.chat.chatviewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exp.memoria.data.repository.MemoryRepository
import com.exp.memoria.data.repository.SettingsRepository
import com.exp.memoria.domain.usecase.GetChatResponseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    getChatResponseUseCase: GetChatResponseUseCase,
    private val memoryRepository: MemoryRepository,
    settingsRepository: SettingsRepository,
    application: Application,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val conversationId: String = savedStateHandle.get<String>("conversationId") ?: UUID.randomUUID().toString()

    private val _uiState = MutableStateFlow(ChatState())
    val uiState = _uiState.asStateFlow()

    private val _totalTokenCount = MutableStateFlow<Int?>(null)
    val totalTokenCount = _totalTokenCount.asStateFlow()

    private var currentPage = 0
    private val pageSize = 50

    private val responseHandler: ChatResponseHandler

    init {
        responseHandler = ChatResponseHandler(
            getChatResponseUseCase = getChatResponseUseCase,
            memoryRepository = memoryRepository,
            settingsRepository = settingsRepository,
            application = application,
            conversationId = conversationId,
            _uiState = _uiState,
            _totalTokenCount = _totalTokenCount,
            coroutineScope = viewModelScope
        )

        Log.d("ChatViewModel", "使用 conversationId 初始化: $conversationId")
        loadMoreMessages()
        viewModelScope.launch {
            val header = memoryRepository.getConversationHeaderById(conversationId)
            header?.let {
                _totalTokenCount.value = it.totalTokenCount
            }
        }
    }

    fun loadMoreMessages() {
        viewModelScope.launch {
            Log.d("ChatViewModel", "[诊断] 为 conversationId: $conversationId, 页面: $currentPage 加载更多消息")
            val memories = memoryRepository.getRawMemories(conversationId, pageSize, currentPage * pageSize)
            Log.d(
                "ChatViewModel",
                "[诊断] 从数据库为 conversationId: $conversationId 获取了 ${memories.size} 条原始记忆。"
            )

            val chatMessages = memories.reversed().map { memory ->
                ChatMessage(
                    id = UUID.nameUUIDFromBytes(memory.id.toString().toByteArray()),
                    text = memory.text,
                    isFromUser = memory.sender == "user",
                    memoryId = memory.id
                )
            }
            Log.d("ChatViewModel", "[诊断] 转换后，准备将 ${chatMessages.size} 条新消息添加到UI。")

            _uiState.update { currentState ->
                val newMessages = chatMessages + currentState.messages
                Log.d(
                    "ChatViewModel",
                    "[诊断] 更新UI状态。之前消息数: ${currentState.messages.size}，之后总消息数: ${newMessages.size}。"
                )
                currentState.copy(messages = newMessages)
            }
            currentPage++
        }
    }

    fun updateMessage(messageId: UUID, newText: String) {
        viewModelScope.launch {
            var memoryIdToUpdate: Long? = null
            var originalText: String? = null

            _uiState.update { currentState ->
                val updatedMessages = currentState.messages.map { message ->
                    if (message.id == messageId) {
                        memoryIdToUpdate = message.memoryId
                        originalText = message.text
                        message.copy(text = newText)
                    } else {
                        message
                    }
                }
                currentState.copy(messages = updatedMessages)
            }

            memoryIdToUpdate?.let { memoryId ->
                Log.d("ChatViewModel", "准备更新数据库... Memory ID: $memoryId, 新文本: $newText")
                try {
                    memoryRepository.updateMemoryText(memoryId, newText)
                    Log.d("ChatViewModel", "数据库更新成功。")
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "数据库更新失败。", e)
                    _uiState.update { currentState ->
                        val restoredMessages = currentState.messages.map { message ->
                            if (message.id == messageId) {
                                message.copy(text = originalText ?: newText)
                            } else {
                                message
                            }
                        }
                        currentState.copy(messages = restoredMessages)
                    }
                }
            }
        }
    }

    fun regenerateResponse(aiMessageId: UUID) {
        viewModelScope.launch {
            val messages = _uiState.value.messages
            val aiMessageIndex = messages.indexOfFirst { it.id == aiMessageId }

            if (aiMessageIndex > 0) {
                val aiMessage = messages[aiMessageIndex]
                val userMessage = messages[aiMessageIndex - 1]

                if (aiMessage.memoryId != null && userMessage.isFromUser) {
                    Log.d(
                        "ChatViewModel",
                        "regenerateResponse: 准备从数据库删除 memoryId >= ${aiMessage.memoryId} 的消息"
                    )
                    memoryRepository.deleteFrom(conversationId, aiMessage.memoryId)
                    Log.d("ChatViewModel", "regenerateResponse: 数据库删除完成")

                    _uiState.update { currentState ->
                        val messagesAfterDeletion = currentState.messages.take(aiMessageIndex)
                        Log.d(
                            "ChatViewModel",
                            "regenerateResponse: UI状态更新，保留 ${messagesAfterDeletion.size} 条消息"
                        )
                        currentState.copy(
                            messages = messagesAfterDeletion,
                            isLoading = true
                        )
                    }

                    Log.d("ChatViewModel", "regenerateResponse: 重新发送用户消息: ${userMessage.text}")
                    responseHandler.generateAiResponse(queryForLlm = null, userQueryForSaving = userMessage.text)
                } else {
                    Log.w(
                        "ChatViewModel",
                        "regenerateResponse: 无法重说。AI消息没有memoryId或前一条不是用户消息。 memoryId is ${aiMessage.memoryId}, isFromUser is ${userMessage.isFromUser}"
                    )
                }
            } else {
                Log.w("ChatViewModel", "regenerateResponse: 无法重说。未找到AI消息或它是第一条消息。")
            }
        }
    }

    fun sendMessage(query: String) {
        if (query.isBlank()) return

        val userMessage = ChatMessage(
            id = UUID.randomUUID(),
            text = query,
            isFromUser = true
        )

        _uiState.update { currentState ->
            currentState.copy(
                messages = currentState.messages + userMessage,
                isLoading = true
            )
        }

        _totalTokenCount.value = null

        viewModelScope.launch {
            responseHandler.generateAiResponse(queryForLlm = query, userQueryForSaving = query)
        }
    }
}
