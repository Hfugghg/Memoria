package com.exp.memoria.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exp.memoria.data.local.entity.ConversationInfo
import com.exp.memoria.data.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * [对话历史页面的 ViewModel]
 *
 * 职责:
 * 1. 作为对话历史界面 (ConversationHistoryScreen) 和数据仓库 (MemoryRepository) 之间的桥梁。
 * 2. 负责从 MemoryRepository 获取所有对话的摘要信息列表 (List<ConversationInfo>)。
 * 3. 使用 StateFlow 将对话列表暴露给 UI，以便在数据变化时自动更新界面。
 *
 * @property conversations 一个 StateFlow，它持有从仓库获取的对话列表，并将其暴露给 UI。
 */
@HiltViewModel
class ConversationHistoryViewModel @Inject constructor(
    private val memoryRepository: MemoryRepository
) : ViewModel() {

    private val _conversations = MutableStateFlow<List<ConversationInfo>>(emptyList())
    val conversations = _conversations.asStateFlow()

    init {
        loadConversations()
    }

    /**
     * 从数据仓库加载所有对话的摘要信息。
     */
    private fun loadConversations() {
        viewModelScope.launch {
            _conversations.value = memoryRepository.getConversations()
        }
    }

    /**
     * 在数据仓库中创建一个新的对话头部记录。
     */
    fun createNewConversation(conversationId: String) {
        viewModelScope.launch {
            memoryRepository.createNewConversation(conversationId)
            // 重新加载对话列表以显示新创建的对话
            loadConversations()
        }
    }
}
