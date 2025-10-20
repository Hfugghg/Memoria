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
 * 4. 提供创建、删除和重命名对话的业务逻辑。
 *
 * 关联:
 * - `memoryRepository`: 用于操作对话数据的仓库。
 *
 * 未实现的功能职责:
 * - **数据加载效率**: 目前，每次创建、删除或重命名对话后，都会从数据库重新加载整个对话列表 (`loadConversations()`)。可以优化为使用 Flow 直接从数据库观察数据变化，或者只在内存中对当前列表进行增删改，以提高效率。
 * - **错误处理**: 当前的数据库操作（加载、创建、删除、重命名）没有包含任何错误处理逻辑。如果数据库操作失败，UI 不会收到任何反馈。
 * - **分页加载**: 如果对话数量非常多，一次性加载所有对话 (`getConversations()`) 可能会影响性能。未来可以考虑实现分页加载。
 * - **排序功能**: 没有提供对对话列表进行排序的选项（例如按时间、按名称排序）。
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

    /**
     * 从数据仓库中删除指定 ID 的对话。
     */
    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            memoryRepository.deleteConversation(conversationId)
            loadConversations()
        }
    }

    /**
     * 重命名指定 ID 的对话。
     */
    fun renameConversation(conversationId: String, newName: String) {
        viewModelScope.launch {
            memoryRepository.renameConversation(conversationId, newName)
            loadConversations()
        }
    }
}
