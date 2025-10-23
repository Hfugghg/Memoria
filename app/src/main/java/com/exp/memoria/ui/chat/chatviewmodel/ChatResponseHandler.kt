package com.exp.memoria.ui.chat.chatviewmodel

import android.app.Application
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.exp.memoria.core.workers.MemoryProcessingWorker
import com.exp.memoria.data.model.FileAttachment
import com.exp.memoria.data.repository.ChatChunkResult
import com.exp.memoria.data.repository.MemoryRepository
import com.exp.memoria.data.repository.SettingsRepository
import com.exp.memoria.domain.usecase.GetChatResponseUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.*
import kotlin.coroutines.cancellation.CancellationException

class ChatResponseHandler(
    private val getChatResponseUseCase: GetChatResponseUseCase,
    private val memoryRepository: MemoryRepository,
    private val settingsRepository: SettingsRepository,
    private val application: Application,
    private val conversationId: String,
    private val _uiState: MutableStateFlow<ChatState>,
    private val _totalTokenCount: MutableStateFlow<Int?>,
    private val coroutineScope: CoroutineScope
) {

    suspend fun generateAiResponse(
        queryForLlm: String?,
        userQueryForSaving: String,
        attachments: List<FileAttachment>
    ) {
        _totalTokenCount.value = null // 重置令牌计数，因为这是一个新的请求
        val isStreaming = settingsRepository.settingsFlow.first().isStreamingEnabled

        try {
            if (isStreaming) {
                streamResponse(queryForLlm, userQueryForSaving, attachments)
            } else {
                nonStreamResponse(queryForLlm, userQueryForSaving, attachments)
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

    private suspend fun streamResponse(
        queryForLlm: String?,
        userQueryForSaving: String,
        attachments: List<FileAttachment>
    ) {
        Log.d(
            "ChatResponseHandler",
            "streamResponse: 开始处理查询: $queryForLlm (用于LLM), 用户查询: $userQueryForSaving (用于保存)"
        )

        // 1. 为AI响应添加一个带唯一ID的占位符。
        val aiMessageId = UUID.randomUUID()
        _uiState.update { currentState ->
            currentState.copy(
                messages = currentState.messages + ChatMessage(id = aiMessageId, text = "", isFromUser = false)
            )
        }
        Log.d("ChatResponseHandler", "streamResponse: 已为AI响应添加占位符。")

        val successfulTextBuffer = StringBuilder()
        var hasErrorOccurred = false
        var uiErrorMessage = "" // 用于在UI上显示的错误

        try {
            getChatResponseUseCase.invoke(queryForLlm, conversationId, true, attachments).collect { result ->
                when (result) {
                    is ChatChunkResult.Success -> {
                        val chunk = result.text
                        successfulTextBuffer.append(chunk)

                        result.totalTokenCount?.let { count ->
                            _totalTokenCount.value = count
                            coroutineScope.launch {
                                memoryRepository.updateTotalTokenCount(conversationId, count)
                            }
                        }

                        for (char in chunk) {
                            _uiState.update { currentState ->
                                val updatedMessages = currentState.messages.toMutableList()
                                val messageIndex = updatedMessages.indexOfFirst { it.id == aiMessageId }

                                if (messageIndex != -1) {
                                    val currentMessage = updatedMessages[messageIndex]
                                    val updatedMessage = currentMessage.copy(text = currentMessage.text + char)
                                    updatedMessages[messageIndex] = updatedMessage
                                    currentState.copy(messages = updatedMessages)
                                } else {
                                    currentState
                                }
                            }
                            delay(15) // 调整此延迟以控制打字速度，15ms是一个比较平滑的值
                        }
                    }

                    is ChatChunkResult.Error -> {
                        hasErrorOccurred = true
                        uiErrorMessage = result.message
                        Log.w("ChatResponseHandler", "streamResponse: 收到错误: $uiErrorMessage")
                        _uiState.update { currentState ->
                            val updatedMessages = currentState.messages.toMutableList()
                            val messageIndex = updatedMessages.indexOfFirst { it.id == aiMessageId }
                            if (messageIndex != -1) {
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
            Log.d("ChatResponseHandler", "streamResponse: 流收集完成。")

        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("ChatResponseHandler", "streamResponse: 流收集过程中发生错误", e)
            hasErrorOccurred = true
            uiErrorMessage = "流式响应出错: ${e.message}"
            _uiState.update { currentState ->
                val updatedMessages = currentState.messages.toMutableList()
                val messageIndex = updatedMessages.indexOfFirst { it.id == aiMessageId }
                if (messageIndex != -1) {
                    updatedMessages[messageIndex] = updatedMessages[messageIndex].copy(text = uiErrorMessage)
                    currentState.copy(messages = updatedMessages)
                } else {
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
            val finalResponseText = successfulTextBuffer.toString()
            val memoryId: Long

            if (!hasErrorOccurred && finalResponseText.isNotEmpty()) {
                Log.d(
                    "ChatResponseHandler",
                    "streamResponse: 正在保存最终响应（${finalResponseText.length} 字符）到记忆中。"
                )
                if (queryForLlm != null) { // 新消息，保存用户查询和AI响应
                    memoryId = memoryRepository.saveNewMemory(userQueryForSaving, finalResponseText, conversationId, attachments)
                } else { // 重说，只保存AI响应
                    memoryId =
                        memoryRepository.saveOnlyAiResponse(userQueryForSaving, finalResponseText, conversationId)
                }
                Log.d("ChatResponseHandler", "streamResponse: 新记忆已保存。 ID: $memoryId")
            } else {
                Log.w("ChatResponseHandler", "streamResponse: 发生错误或响应为空，保存空响应。错误: $uiErrorMessage")
                if (queryForLlm != null) { // 新消息，保存用户查询和空AI响应
                    memoryId = memoryRepository.saveNewMemory(userQueryForSaving, "", conversationId, attachments)
                } else { // 重说，只保存空AI响应
                    memoryId = memoryRepository.saveOnlyAiResponse(userQueryForSaving, "", conversationId)
                }
                Log.d("ChatResponseHandler", "streamResponse: 已保存空记忆。 ID: $memoryId")
            }

            // !!! 核心修复点：更新UI状态中的AI消息，为其填充正确的memoryId和稳定的id !!!
            _uiState.update { currentState ->
                val updatedMessages = currentState.messages.map { message ->
                    if (message.id == aiMessageId) {
                        Log.d(
                            "ChatResponseHandler",
                            "streamResponse.finally: Found and updating placeholder message. Old ID: ${message.id}, New Stable ID: ${
                                UUID.nameUUIDFromBytes(memoryId.toString().toByteArray())
                            }, New memoryId: $memoryId"
                        )
                        // 更新占位符消息，为其赋予稳定的ID和数据库ID
                        message.copy(
                            id = UUID.nameUUIDFromBytes(memoryId.toString().toByteArray()),
                            memoryId = memoryId
                        )
                    } else {
                        message
                    }
                }
                // 同时更新加载状态
                currentState.copy(
                    messages = updatedMessages,
                    isLoading = false
                )
            }
            Log.d("ChatResponseHandler", "streamResponse: UI state updated with final memoryId and stable UUID.")

            // 安排后台处理。
            val workRequest = OneTimeWorkRequestBuilder<MemoryProcessingWorker>()
                .setInputData(workDataOf(MemoryProcessingWorker.KEY_MEMORY_ID to memoryId))
                .build()
            WorkManager.getInstance(application).enqueue(workRequest)
            Log.d("ChatResponseHandler", "streamResponse: WorkManager 任务已入队。")
        }
    }

    private suspend fun nonStreamResponse(
        queryForLlm: String?,
        userQueryForSaving: String,
        attachments: List<FileAttachment>
    ) {
        Log.d(
            "ChatResponseHandler",
            "nonStreamResponse: 开始处理查询: $queryForLlm (用于LLM), 用户查询: $userQueryForSaving (用于保存)"
        )
        try {
            val result = getChatResponseUseCase.invoke(queryForLlm, conversationId, false, attachments).first()
            Log.d("ChatResponseHandler", "nonStreamResponse: 获取到的结果: $result")

            when (result) {
                is ChatChunkResult.Success -> {
                    val response = result.text
                    result.totalTokenCount?.let { count ->
                        _totalTokenCount.value = count
                        coroutineScope.launch {
                            memoryRepository.updateTotalTokenCount(conversationId, count)
                        }
                    }

                    val memoryId: Long
                    if (response.isNotEmpty()) {
                        Log.d("ChatResponseHandler", "nonStreamResponse: 为 conversationId: $conversationId 保存新内存")
                        if (queryForLlm != null) { // 新消息，保存用户查询和AI响应
                            memoryId = memoryRepository.saveNewMemory(userQueryForSaving, response, conversationId, attachments)
                        } else { // 重说，只保存AI响应
                            memoryId = memoryRepository.saveOnlyAiResponse(userQueryForSaving, response, conversationId)
                        }
                        Log.d("ChatResponseHandler", "nonStreamResponse: 新内存已保存. memoryId: $memoryId")

                        _uiState.update { currentState ->
                            currentState.copy(
                                messages = currentState.messages + ChatMessage(
                                    id = UUID.nameUUIDFromBytes(memoryId.toString().toByteArray()), // 使用稳定的UUID
                                    text = response,
                                    isFromUser = false,
                                    memoryId = memoryId // 填充数据库ID
                                ),
                                isLoading = false
                            )
                        }
                        Log.d("ChatResponseHandler", "nonStreamResponse: UI 已更新.")

                        val workRequest = OneTimeWorkRequestBuilder<MemoryProcessingWorker>()
                            .setInputData(workDataOf(MemoryProcessingWorker.KEY_MEMORY_ID to memoryId))
                            .build()
                        WorkManager.getInstance(application).enqueue(workRequest)
                        Log.d("ChatResponseHandler", "nonStreamResponse: WorkManager 任务已入队.")
                    } else {
                        Log.w("ChatResponseHandler", "nonStreamResponse: 获取到的响应为空。")
                        // 保存空响应
                        if (queryForLlm != null) { // 新消息，保存用户查询和空AI响应
                            memoryId = memoryRepository.saveNewMemory(userQueryForSaving, "", conversationId, attachments)
                        } else { // 重说，只保存空AI响应
                            memoryId = memoryRepository.saveOnlyAiResponse(userQueryForSaving, "", conversationId)
                        }
                        _uiState.update { currentState ->
                            currentState.copy(
                                messages = currentState.messages + ChatMessage(
                                    id = UUID.nameUUIDFromBytes(memoryId.toString().toByteArray()), // 使用稳定的UUID
                                    text = "未能获取到有效回复，请重试。",
                                    isFromUser = false,
                                    memoryId = memoryId // 填充数据库ID
                                ),
                                isLoading = false
                            )
                        }
                    }
                }

                is ChatChunkResult.Error -> {
                    val errorMessage = result.message
                    Log.w("ChatResponseHandler", "nonStreamResponse: 收到错误: $errorMessage")
                    // 保存空响应
                    val memoryId: Long
                    if (queryForLlm != null) { // 新消息，保存用户查询和空AI响应
                        memoryId = memoryRepository.saveNewMemory(userQueryForSaving, "", conversationId, attachments)
                    } else { // 重说，只保存空AI响应
                        memoryId = memoryRepository.saveOnlyAiResponse(userQueryForSaving, "", conversationId)
                    }
                    // 只在UI上显示错误
                    _uiState.update { currentState ->
                        currentState.copy(
                            messages = currentState.messages + ChatMessage(
                                id = UUID.nameUUIDFromBytes(memoryId.toString().toByteArray()), // 使用稳定的UUID
                                text = errorMessage,
                                isFromUser = false,
                                memoryId = memoryId // 填充数据库ID
                            ),
                            isLoading = false
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChatResponseHandler", "nonStreamResponse: 非流式响应过程中发生错误", e) // Log exceptions
            // 保存空响应
            val memoryId: Long
            if (queryForLlm != null) { // 新消息，保存用户查询和空AI响应
                memoryId = memoryRepository.saveNewMemory(userQueryForSaving, "", conversationId, attachments)
            } else { // 重说，只保存空AI响应
                memoryId = memoryRepository.saveOnlyAiResponse(userQueryForSaving, "", conversationId)
            }
            _uiState.update { currentState ->
                currentState.copy(
                    messages = currentState.messages + ChatMessage(
                        id = UUID.nameUUIDFromBytes(memoryId.toString().toByteArray()), // 使用稳定的UUID
                        text = "非流式响应出错: ${e.message}",
                        isFromUser = false,
                        memoryId = memoryId // 填充数据库ID
                    ),
                    isLoading = false
                )
            }
        }
    }
}
