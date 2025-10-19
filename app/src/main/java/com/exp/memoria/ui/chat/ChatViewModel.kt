package com.exp.memoria.ui.chat

import android.app.Application
import android.util.Log // 导入 Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.exp.memoria.core.workers.MemoryProcessingWorker
import com.exp.memoria.data.repository.MemoryRepository
import com.exp.memoria.domain.usecase.GetChatResponseUseCase
import com.exp.memoria.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.delay

/**
 * ChatViewModel
 *
 * 职责:
 * 1. 作为UI(ChatScreen)和数据/业务逻辑层(Use Cases, Repositories)之间的桥梁，遵循MVVM架构。
 * 2. 持有并管理聊天界面的UI状态(ChatState)，例如消息列表、加载状态等，通过StateFlow暴露给UI。
 * 3. 处理用户交互，如发送消息，并调用相应的业务逻辑。
 * 4. 从仓库加载并显示历史消息。
 * 5. 在收到新消息时，触发后台任务处理记忆。
 * 6. 处理流式响应：通过逐字更新UI状态并引入微小延迟，实现平滑的打字机效果，避免UI卡顿。
 *
 * @property getChatResponseUseCase 用于获取聊天回复的用例。
 * @property memoryRepository 用于操作记忆数据的仓库。
 * @property settingsRepository 用于获取应用设置的仓库。
 * @property application 应用上下文，用于访问WorkManager等。
 * @param savedStateHandle 用于处理进程重建后的状态保存和恢复。
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val getChatResponseUseCase: GetChatResponseUseCase,
    private val memoryRepository: MemoryRepository,
    private val settingsRepository: SettingsRepository, // 注入 SettingsRepository
    private val application: Application,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val conversationId: String = savedStateHandle.get<String>("conversationId") ?: UUID.randomUUID().toString()

    private val _uiState = MutableStateFlow(ChatState())
    val uiState = _uiState.asStateFlow()

    private var currentPage = 0
    private val pageSize = 50

    init {
        Log.d("ChatViewModel", "使用 conversationId 初始化: $conversationId")
        loadMoreMessages()
    }

    fun loadMoreMessages() {
        viewModelScope.launch {
            Log.d("ChatViewModel", "[诊断] 为 conversationId: $conversationId, 页面: $currentPage 加载更多消息")
            val memories = memoryRepository.getRawMemories(conversationId, pageSize, currentPage * pageSize)
            Log.d("ChatViewModel", "[诊断] 从数据库为 conversationId: $conversationId 获取了 ${memories.size} 条原始记忆。")

            val chatMessages = memories.reversed().map { memory ->
                ChatMessage(
                    // 从数据库的 Long ID 生成一个稳定的 UUID
                    id = UUID.nameUUIDFromBytes(memory.id.toString().toByteArray()),
                    text = memory.text,
                    isFromUser = memory.sender == "user"
                )
            }
            Log.d("ChatViewModel", "[诊断] 转换后，准备将 ${chatMessages.size} 条新消息添加到UI。")

            _uiState.update { currentState ->
                val newMessages = chatMessages + currentState.messages
                Log.d("ChatViewModel", "[诊断] 更新UI状态。之前消息数: ${currentState.messages.size}，之后总消息数: ${newMessages.size}。")
                currentState.copy(messages = newMessages)
            }
            currentPage++
        }
    }

    fun sendMessage(query: String) {
        if (query.isBlank()) return

        // 为用户消息创建一个唯一的ID，这对于Compose的性能至关重要
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

        viewModelScope.launch {
            val isStreaming = settingsRepository.settingsFlow.first().isStreamingEnabled

            try {
                if (isStreaming) {
                    streamResponse(query)
                } else {
                    nonStreamResponse(query)
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        messages = currentState.messages + ChatMessage(id = UUID.randomUUID(), text = "出错了，请重试", isFromUser = false),
                        isLoading = false
                    )
                }
                e.printStackTrace()
            }
        }
    }

    private suspend fun streamResponse(query: String) {
        Log.d("ChatViewModel", "streamResponse: 开始处理查询: $query")

        // 1. 为AI响应添加一个带唯一ID的占位符。
        val aiMessageId = UUID.randomUUID()
        _uiState.update { currentState ->
            currentState.copy(
                messages = currentState.messages + ChatMessage(id = aiMessageId, text = "", isFromUser = false)
            )
        }
        Log.d("ChatViewModel", "streamResponse: 已为AI响应添加占位符。")

        try {
            // 2. 收集流并逐字更新UI，以创建打字机效果。
            getChatResponseUseCase.invoke(query, conversationId, true).collect { chunk ->
                // 模拟打字效果，逐字更新
                for (char in chunk) {
                    _uiState.update { currentState ->
                        val updatedMessages = currentState.messages.toMutableList()
                        val messageIndex = updatedMessages.indexOfFirst { it.id == aiMessageId }

                        // 仅当找到我们的AI响应占位符时才更新
                        if (messageIndex != -1) {
                            val currentMessage = updatedMessages[messageIndex]
                            val updatedMessage = currentMessage.copy(text = currentMessage.text + char)
                            updatedMessages[messageIndex] = updatedMessage
                            currentState.copy(messages = updatedMessages)
                        } else {
                            currentState // 如果发生意外情况，则不更新
                        }
                    }
                    delay(15) // 调整此延迟以控制打字速度，15ms是一个比较平滑的值
                }
            }
            Log.d("ChatViewModel", "streamResponse: 流收集完成。")

        } catch (e: Exception) {
            Log.e("ChatViewModel", "streamResponse: 流收集过程中发生错误", e)
            _uiState.update { currentState ->
                val updatedMessages = currentState.messages.toMutableList()
                // 同样，只更新我们之前创建的占位符消息
                val messageIndex = updatedMessages.indexOfFirst { it.id == aiMessageId }
                if (messageIndex != -1) {
                    updatedMessages[messageIndex] = updatedMessages[messageIndex].copy(text = "流式响应出错: ${e.message}")
                    currentState.copy(messages = updatedMessages)
                } else {
                    // 如果占位符消息由于某种原因不存在，则添加一条新的错误消息
                    currentState.copy(messages = currentState.messages + ChatMessage(id = UUID.randomUUID(), text = "流式响应出错: ${e.message}", isFromUser = false))
                }
            }
        } finally {
            // 3. 流完成后（或失败），更新加载状态。
            _uiState.update { it.copy(isLoading = false) }
            Log.d("ChatViewModel", "streamResponse: 设置 isLoading 为 false。")

            // 4. 将最终的完整响应保存到数据库。
            val finalResponse = _uiState.value.messages.firstOrNull { it.id == aiMessageId }
            if (finalResponse != null && finalResponse.text.isNotEmpty() && !finalResponse.text.contains("出错")) {
                Log.d("ChatViewModel", "streamResponse: 正在保存最终响应到记忆中。")
                val memoryId = memoryRepository.saveNewMemory(query, finalResponse.text, conversationId)
                Log.d("ChatViewModel", "streamResponse: 新记忆已保存。 ID: $memoryId")

                // 安排后台处理。
                val workRequest = OneTimeWorkRequestBuilder<MemoryProcessingWorker>()
                    .setInputData(workDataOf(MemoryProcessingWorker.KEY_MEMORY_ID to memoryId))
                    .build()
                WorkManager.getInstance(application).enqueue(workRequest)
                Log.d("ChatViewModel", "streamResponse: WorkManager 任务已入队。")
            } else {
                Log.w("ChatViewModel", "streamResponse: 最终响应为空或为错误，不保存到记忆中。")
            }
        }
    }

    private suspend fun nonStreamResponse(query: String) {
        Log.d("ChatViewModel", "nonStreamResponse: 开始处理查询: $query")
        try {
            // 使用 .first() 来获取非流式响应的单个结果
            Log.d("ChatViewModel", "nonStreamResponse: 调用 getChatResponseUseCase (非流式模式)...")
            val response = getChatResponseUseCase.invoke(query, conversationId, false).first()
            Log.d("ChatViewModel", "nonStreamResponse: 获取到的响应: '$response'")

            if (response.isNotEmpty()) {
                Log.d("ChatViewModel", "nonStreamResponse: 为 conversationId: $conversationId 保存新内存")
                val memoryId = memoryRepository.saveNewMemory(query, response, conversationId)
                Log.d("ChatViewModel", "nonStreamResponse: 新内存已保存. memoryId: $memoryId")

                _uiState.update { currentState ->
                    currentState.copy(
                        messages = currentState.messages + ChatMessage(id = UUID.randomUUID(), text = response, isFromUser = false),
                        isLoading = false
                    )
                }
                Log.d("ChatViewModel", "nonStreamResponse: UI 已更新.")

                val workRequest = OneTimeWorkRequestBuilder<MemoryProcessingWorker>()
                    .setInputData(workDataOf(MemoryProcessingWorker.KEY_MEMORY_ID to memoryId))
                    .build()
                WorkManager.getInstance(application).enqueue(workRequest)
                Log.d("ChatViewModel", "nonStreamResponse: WorkManager 任务已入队.")
            } else {
                Log.w("ChatViewModel", "nonStreamResponse: 获取到的响应为空。")
                _uiState.update { currentState ->
                    currentState.copy(
                        messages = currentState.messages + ChatMessage(id = UUID.randomUUID(), text = "未能获取到有效回复，请重试。", isFromUser = false),
                        isLoading = false
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "nonStreamResponse: 非流式响应过程中发生错误", e) // Log exceptions
            _uiState.update { currentState ->
                currentState.copy(
                    messages = currentState.messages + ChatMessage(id = UUID.randomUUID(), text = "非流式响应出错: ${e.message}", isFromUser = false),
                    isLoading = false
                )
            }
        }
    }

}
