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
 */

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val getChatResponseUseCase: GetChatResponseUseCase
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
