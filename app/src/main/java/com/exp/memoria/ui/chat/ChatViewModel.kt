package com.exp.memoria.ui.chat

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.exp.memoria.core.workers.MemoryProcessingWorker
import com.exp.memoria.data.repository.MemoryRepository
import com.exp.memoria.domain.usecase.GetChatResponseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * [聊天界面的ViewModel]
 *
 * 职责:
 * 1. 作为UI(ChatScreen)和数据/业务逻辑层(Use Cases, Repositories)之间的桥梁，遵循MVVM架构 [cite: 77]。
 * 2. 持有并管理聊天界面的UI状态(ChatState)，例如消息列表、加载状态、输入框文本等，通常使用StateFlow或LiveData暴露给UI。
 * 3. 当用户发送新消息时：
 * a. 更新UI状态为“加载中”。
 * b. 调用 GetChatResponseUseCase 获取AI回复。
 * c. 收到回复后，调用 MemoryRepository 保存这次新的问答记录。
 * d. 更新UI状态，将新消息和AI回复添加到消息列表中。
 * e. 触发后台任务(WorkManager)，调度 MemoryProcessingWorker 来处理刚刚生成的这条新记忆 [cite: 24, 29]。
 *
 * 关联:
 * - 注入 GetChatResponseUseCase 和 MemoryRepository，以及 WorkManager 的实例。
 * - ChatScreen 会观察这个ViewModel暴露出的UI状态并据此渲染界面。
 *
 * 实现指导:
 * - @HiltViewModel 注解，通过构造函数注入 GetChatResponseUseCase, MemoryRepository, WorkManager。
 * - 定义一个 `_uiState` (MutableStateFlow) 和一个 `uiState` (StateFlow) 来暴露UI状态。
 * - fun onSendMessage(message: String):
 * a. 更新UI状态，将用户消息添加到列表中并显示加载状态。
 * b. 在 viewModelScope.launch 中调用 `GetChatResponseUseCase(message)`。
 * c. 收到回复后，调用 `MemoryRepository.saveNewMemory(...)` 保存这次新的问答记录。
 * d. 更新UI状态，将AI回复添加到列表中并取消加载状态。
 * e. 向 WorkManager 提交一个 OneTimeWorkRequest，调度 `MemoryProcessingWorker` 在后台自动处理这条新记忆 [cite: 24, 29]。
 */

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val getChatResponseUseCase: GetChatResponseUseCase,
    private val memoryRepository: MemoryRepository,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatState())
    val uiState = _uiState.asStateFlow()

    fun sendMessage(query: String) {
        if (query.isBlank()) return

        // 立即将用户消息添加到 UI
        _uiState.update { currentState ->
            currentState.copy(
                messages = currentState.messages + ChatMessage(text = query, isFromUser = true),
                isLoading = true
            )
        }

        // 启动协程以获取 AI 响应
        viewModelScope.launch {
            try {
                val response = getChatResponseUseCase(query)

                // 保存到数据库
                val memoryId = memoryRepository.saveNewMemory(query, response)

                // 成功后更新UI
                _uiState.update { currentState ->
                    currentState.copy(
                        messages = currentState.messages + ChatMessage(text = response, isFromUser = false),
                        isLoading = false
                    )
                }

                // 启动后台任务
                val workRequest = OneTimeWorkRequestBuilder<MemoryProcessingWorker>()
                    .setInputData(workDataOf(MemoryProcessingWorker.KEY_MEMORY_ID to memoryId))
                    .build()
                WorkManager.getInstance(application).enqueue(workRequest)

            } catch (e: Exception) {
                // 捕获所有异常，包括 HttpException
                // 在这里处理错误，例如显示一条错误消息
                _uiState.update { currentState ->
                    currentState.copy(
                        messages = currentState.messages + ChatMessage(text = "出错了，请重试", isFromUser = false),
                        isLoading = false
                    )
                }
                // 可以在 logcat 中打印错误，方便调试
                e.printStackTrace()
            }
        }
    }
}