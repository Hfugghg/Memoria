package com.exp.memoria.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exp.memoria.data.local.entity.ConversationInfo
import com.exp.memoria.data.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * [对话历史页面的 ViewModel]
 *
 * 职责:
 * 1. 作为对话历史界面 (ConversationHistoryScreen) 和数据仓库 (MemoryRepository) 之间的桥梁。
 * 2. 负责从 MemoryRepository 获取所有对话的摘要信息列表 (List<ConversationInfo>)。
 * 3. 使用 StateFlow 将对话列表暴露给 UI，以便在数据变化时自动更新界面。
 * 4. 提供创建、删除和重命名对话的业务逻辑。
 *
 * 关联:
 * - `memoryRepository`: 用于操作对话数据的仓库。
 *
 * 已实现的功能职责:
 * - **实时更新与排序**: 对话列表现在会根据对话内最新消息的时间戳进行实时排序和更新。
 * - **创建、删除和重命名对话**: 提供相应的业务逻辑。
 */
@HiltViewModel
class ConversationHistoryViewModel @Inject constructor(
    private val memoryRepository: MemoryRepository
) : ViewModel() {

    private val _conversations = MutableStateFlow<List<ConversationInfo>>(emptyList())
    val conversations = _conversations.asStateFlow()

    init {
        // 收集来自数据仓库的对话列表Flow，实现实时更新
        viewModelScope.launch {
            memoryRepository.getConversations().collectLatest {
                _conversations.value = it
            }
        }
    }

    /**
     * 在数据仓库中创建一个新的对话头部记录。
     */
    fun createNewConversation(conversationId: String) {
        viewModelScope.launch {
            memoryRepository.createNewConversation(conversationId)
            // 数据仓库的Flow会自动发出更新，无需手动重新加载
        }
    }

    /**
     * 从数据仓库中删除指定 ID 的对话。
     */
    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            memoryRepository.deleteConversation(conversationId)
            // 数据仓库的Flow会自动发出更新，无需手动重新加载
        }
    }

    /**
     * 重命名指定 ID 的对话。
     */
    fun renameConversation(conversationId: String, newName: String) {
        viewModelScope.launch {
            memoryRepository.renameConversation(conversationId, newName)
            // 数据仓库的Flow会自动发出更新，无需手动重新加载
        }
    }
}
