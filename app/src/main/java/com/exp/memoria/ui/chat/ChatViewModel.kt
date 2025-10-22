package com.exp.memoria.ui.chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.exp.memoria.core.workers.MemoryProcessingWorker
import com.exp.memoria.data.repository.ChatChunkResult
import com.exp.memoria.data.repository.MemoryRepository
import com.exp.memoria.data.repository.SettingsRepository
import com.exp.memoria.domain.usecase.GetChatResponseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

/**
 * [ChatViewModel]
 *
 * 职责:
 * 1. 作为UI(ChatScreen)和数据/业务逻辑层(Use Cases, Repositories)之间的桥梁，遵循MVVM架构。
 * 2. 持有并管理聊天界面的UI状态(ChatState)，例如消息列表、加载状态等，通过StateFlow暴露给UI。
 * 3. 处理用户交互，如发送消息，并调用相应的业务逻辑。
 * 4. 从仓库加载并显示历史消息。
 * 5. 在收到新消息时，触发后台任务处理记忆。
 * 6. 处理流式响应：通过逐字更新UI状态并引入微小延迟，实现平滑的打字机效果，避免UI卡顿。
 *
 * 关联:
 * - `getChatResponseUseCase`: 用于获取聊天回复的用例。
 * - `memoryRepository`: 用于操作记忆数据的仓库。
 * - `settingsRepository`: 用于获取应用设置的仓库。
 * - `application`: 应用上下文，用于访问WorkManager等。
 * - `savedStateHandle`: 用于处理进程重建后的状态保存和恢复。
 *
 * 未实现的功能职责:
 * - **加载更多历史消息**: 虽然有分页逻辑 (`loadMoreMessages`), 但没有提供给用户一个触发方式（例如，滚动到顶部时自动加载）。
 * - **精细的错误处理**: 目前的错误处理只是在UI上显示一条通用错误消息，可以改进为针对不同错误类型（网络、API等）显示更具体的信息。
 * - **取消响应**: 对于正在进行的流式响应，没有提供取消机制。
 * - **完整的状态恢复**: 没有完全处理进程死亡后的状态恢复，例如，用户在输入框中但尚未发送的文本将会丢失。
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

    // 用于存储和暴露总令牌数
    private val _totalTokenCount = MutableStateFlow<Int?>(null)
    val totalTokenCount = _totalTokenCount.asStateFlow()

    private var currentPage = 0
    private val pageSize = 50

    init {
        Log.d("ChatViewModel", "使用 conversationId 初始化: $conversationId")
        loadMoreMessages()
        // 加载持久化的令牌数
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
                    // 从数据库的 Long ID 生成一个稳定的 UUID
                    id = UUID.nameUUIDFromBytes(memory.id.toString().toByteArray()),
                    text = memory.text,
                    isFromUser = memory.sender == "user",
                    memoryId = memory.id // 填充数据库ID
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

            // 1. 更新UI状态并获取要更新的memoryId
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

            // 2. 如果找到了memoryId，则执行数据库更新
            memoryIdToUpdate?.let { memoryId ->
                Log.d("ChatViewModel", "准备更新数据库... Memory ID: $memoryId, 新文本: $newText")
                try {
                    // 假设 memoryRepository 中有 updateMemoryText 方法
                    memoryRepository.updateMemoryText(memoryId, newText)
                    Log.d("ChatViewModel", "数据库更新成功。")
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "数据库更新失败。", e)
                    // 如果数据库更新失败，将UI状态恢复到原始文本
                    _uiState.update { currentState ->
                        val restoredMessages = currentState.messages.map { message ->
                            if (message.id == messageId) {
                                message.copy(text = originalText ?: newText) // 恢复旧文本
                            } else {
                                message
                            }
                        }
                        currentState.copy(messages = restoredMessages)
                    }
                    // TODO: 向用户显示一个错误提示
                }
            }
        }
    }


    fun regenerateResponse(aiMessageId: UUID) {
        // TODO: 重说功能目前只是重新请求，并未覆盖数据库中旧的AI回复。后续应实现替换逻辑。
        viewModelScope.launch {
            val messages = _uiState.value.messages
            val aiMessageIndex = messages.indexOfFirst { it.id == aiMessageId }

            // 确保我们找到了AI消息，并且它不是列表中的第一条消息
            if (aiMessageIndex > 0) {
                val userMessage = messages[aiMessageIndex - 1]
                // 确保前一条消息是用户的
                if (userMessage.isFromUser) {
                    // 从UI中移除旧的AI回复。注意：这没有从数据库中删除它。
                    _uiState.update { currentState ->
                        currentState.copy(
                            messages = currentState.messages.filterNot { it.id == aiMessageId }
                        )
                    }
                    // 使用之前的用户查询重新发送消息
                    sendMessage(userMessage.text)
                }
            }
        }
    }

    fun sendMessage(query: String) {
        if (query.isBlank()) return

        // 为用户消息创建一个唯一的ID，这对于Compose的性能至关重要
        val userMessage = ChatMessage(
            id = UUID.randomUUID(),
            text = query,
            isFromUser = true
            // memoryId 初始为 null，因为它还没被保存
        )

        _uiState.update { currentState ->
            currentState.copy(
                messages = currentState.messages + userMessage,
                isLoading = true
            )
        }

        // 重置令牌计数，因为这是一个新的请求
        _totalTokenCount.value = null

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
                        messages = currentState.messages + ChatMessage(
                            id = UUID.randomUUID(),
                            text = "出错了，请重试",
                            isFromUser = false
                        ),
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

        // [修改]：用于累积成功文本和标记错误
        val successfulTextBuffer = StringBuilder()
        var hasErrorOccurred = false
        var uiErrorMessage = "" // 用于在UI上显示的错误

        try {
            // 2. [修改]：收集 ChatChunkResult
            getChatResponseUseCase.invoke(query, conversationId, true).collect { result ->
                when (result) {
                    is ChatChunkResult.Success -> {
                        val chunk = result.text
                        // [修改]：只累积成功的文本
                        successfulTextBuffer.append(chunk)

                        // 更新并保存总令牌数
                        result.totalTokenCount?.let { count ->
                            _totalTokenCount.value = count
                            viewModelScope.launch {
                                memoryRepository.updateTotalTokenCount(conversationId, count)
                            }
                        }

                        // 模拟打字效果，逐字更新
                        for (char in chunk) {
                            _uiState.update { currentState ->
                                val updatedMessages = currentState.messages.toMutableList()
                                val messageIndex = updatedMessages.indexOfFirst { it.id == aiMessageId }

                                // 仅当找到我们的AI响应占位符时才更新
                                if (messageIndex != -1) {
                                    val currentMessage = updatedMessages[messageIndex]
                                    // [修改]：确保我们只在当前累积的文本上附加
                                    // 如果之前有错误，UI上会显示错误，但我们这里用 buffer 的内容（虽然这里只追加char）
                                    // 更好的方式是直接使用 buffer 的内容更新UI
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

                    is ChatChunkResult.Error -> {
                        // [修改]：发生错误，设置标志并更新UI
                        hasErrorOccurred = true
                        uiErrorMessage = result.message
                        Log.w("ChatViewModel", "streamResponse: 收到错误: $uiErrorMessage")
                        _uiState.update { currentState ->
                            val updatedMessages = currentState.messages.toMutableList()
                            val messageIndex = updatedMessages.indexOfFirst { it.id == aiMessageId }
                            if (messageIndex != -1) {
                                // [修改]：在UI上显示错误。这 *不会* 被保存
                                updatedMessages[messageIndex] =
                                    updatedMessages[messageIndex].copy(text = uiErrorMessage)
                                currentState.copy(messages = updatedMessages)
                            } else {
                                currentState
                            }
                        }
                    }
                }
            }
            Log.d("ChatViewModel", "streamResponse: 流收集完成。")

        } catch (e: Exception) {
            Log.e("ChatViewModel", "streamResponse: 流收集过程中发生错误", e)
            // [修改]：捕获异常也算作错误
            hasErrorOccurred = true
            uiErrorMessage = "流式响应出错: ${e.message}"
            _uiState.update { currentState ->
                val updatedMessages = currentState.messages.toMutableList()
                val messageIndex = updatedMessages.indexOfFirst { it.id == aiMessageId }
                if (messageIndex != -1) {
                    updatedMessages[messageIndex] = updatedMessages[messageIndex].copy(text = uiErrorMessage)
                    currentState.copy(messages = updatedMessages)
                } else {
                    // 如果占位符消息由于某种原因不存在，则添加一条新的错误消息
                    currentState.copy(
                        messages = currentState.messages + ChatMessage(
                            id = UUID.randomUUID(),
                            text = uiErrorMessage,
                            isFromUser = false
                        )
                    )
                }
            }
        } finally {
            // 3. 流完成后（或失败），更新加载状态。
            _uiState.update { it.copy(isLoading = false) }
            Log.d("ChatViewModel", "streamResponse: 设置 isLoading 为 false。")

            // 4. [修改]：将最终的完整响应保存到数据库。
            //    读取 successfulTextBuffer 而不是 UI state
            if (!hasErrorOccurred && successfulTextBuffer.isNotEmpty()) {
                val finalResponseText = successfulTextBuffer.toString()
                Log.d("ChatViewModel", "streamResponse: 正在保存最终响应（${finalResponseText.length} 字符）到记忆中。")
                val memoryId = memoryRepository.saveNewMemory(query, finalResponseText, conversationId)
                Log.d("ChatViewModel", "streamResponse: 新记忆已保存。 ID: $memoryId")

                // 安排后台处理。
                val workRequest = OneTimeWorkRequestBuilder<MemoryProcessingWorker>()
                    .setInputData(workDataOf(MemoryProcessingWorker.KEY_MEMORY_ID to memoryId))
                    .build()
                WorkManager.getInstance(application).enqueue(workRequest)
                Log.d("ChatViewModel", "streamResponse: WorkManager 任务已入队。")
            } else if (hasErrorOccurred) {
                // [修改]：如果发生错误，保存空字符串
                Log.w("ChatViewModel", "streamResponse: 发生错误，正在保存空响应。错误: $uiErrorMessage")
                val memoryId = memoryRepository.saveNewMemory(query, "", conversationId)
                Log.d("ChatViewModel", "streamResponse: 已保存空记忆。 ID: $memoryId")
            } else {
                // [修改]：如果响应为空，也保存空字符串
                Log.w("ChatViewModel", "streamResponse: 最终响应为空，保存空响应。")
                val memoryId = memoryRepository.saveNewMemory(query, "", conversationId)
                Log.d("ChatViewModel", "streamResponse: 已保存空记忆。 ID: $memoryId")
            }
        }
    }

    private suspend fun nonStreamResponse(query: String) {
        Log.d("ChatViewModel", "nonStreamResponse: 开始处理查询: $query")
        try {
            // 使用 .first() 来获取非流式响应的单个结果
            Log.d("ChatViewModel", "nonStreamResponse: 调用 getChatResponseUseCase (非流式模式)...")
            // [修改]：获取 ChatChunkResult
            val result = getChatResponseUseCase.invoke(query, conversationId, false).first()
            Log.d("ChatViewModel", "nonStreamResponse: 获取到的结果: $result")

            // [修改]：使用 when 处理结果
            when (result) {
                is ChatChunkResult.Success -> {
                    val response = result.text
                    // 更新并保存总令牌数
                    result.totalTokenCount?.let { count ->
                        _totalTokenCount.value = count
                        memoryRepository.updateTotalTokenCount(conversationId, count)
                    }

                    if (response.isNotEmpty()) {
                        Log.d("ChatViewModel", "nonStreamResponse: 为 conversationId: $conversationId 保存新内存")
                        val memoryId = memoryRepository.saveNewMemory(query, response, conversationId)
                        Log.d("ChatViewModel", "nonStreamResponse: 新内存已保存. memoryId: $memoryId")

                        _uiState.update { currentState ->
                            currentState.copy(
                                messages = currentState.messages + ChatMessage(
                                    id = UUID.randomUUID(),
                                    text = response,
                                    isFromUser = false
                                ),
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
                        // [修改]：保存空响应
                        memoryRepository.saveNewMemory(query, "", conversationId)
                        _uiState.update { currentState ->
                            currentState.copy(
                                messages = currentState.messages + ChatMessage(
                                    id = UUID.randomUUID(),
                                    text = "未能获取到有效回复，请重试。",
                                    isFromUser = false
                                ),
                                isLoading = false
                            )
                        }
                    }
                }

                is ChatChunkResult.Error -> {
                    val errorMessage = result.message
                    Log.w("ChatViewModel", "nonStreamResponse: 收到错误: $errorMessage")
                    // [修改]：保存空响应
                    memoryRepository.saveNewMemory(query, "", conversationId)
                    // [修改]：只在UI上显示错误
                    _uiState.update { currentState ->
                        currentState.copy(
                            messages = currentState.messages + ChatMessage(
                                id = UUID.randomUUID(),
                                text = errorMessage,
                                isFromUser = false
                            ),
                            isLoading = false
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "nonStreamResponse: 非流式响应过程中发生错误", e) // Log exceptions
            // [修改]：保存空响应
            memoryRepository.saveNewMemory(query, "", conversationId)
            _uiState.update { currentState ->
                currentState.copy(
                    messages = currentState.messages + ChatMessage(
                        id = UUID.randomUUID(),
                        text = "非流式响应出错: ${e.message}",
                        isFromUser = false
                    ),
                    isLoading = false
                )
            }
        }
    }

}
