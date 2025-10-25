package com.exp.memoria.data.repository

import com.exp.memoria.data.local.entity.ConversationHeader
import com.exp.memoria.data.local.entity.ConversationInfo
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    /**
     * 获取所有对话的列表。
     *
     * @return 包含ConversationInfo对象的Flow。
     */
    fun getConversations(): Flow<List<ConversationInfo>>

    /**
     * 创建一个新的对话头部记录。
     *
     * @param conversationId 新对话的ID。
     */
    suspend fun createNewConversation(conversationId: String)

    /**
     * 更新对话的最后更新时间。
     *
     * @param conversationId 对话ID。
     * @param timestamp 最后更新时间戳。
     */
    suspend fun updateConversationLastUpdate(conversationId: String, timestamp: Long)

    /**
     * 删除指定 ID 的对话及其所有相关记忆。
     *
     * @param conversationId 要删除的对话ID。
     */
    suspend fun deleteConversation(conversationId: String)

    /**
     * 重命名指定 ID 的对话。
     *
     * @param conversationId 要重命名的对话ID。
     * @param newName 新的对话名称。
     */
    suspend fun renameConversation(conversationId: String, newName: String)

    /**
     * 更新对话的响应模式。
     *
     * @param conversationId 对话ID。
     * @param responseSchema 新的响应模式字符串，如果为null则清除。
     */
    suspend fun updateResponseSchema(conversationId: String, responseSchema: String?)

    /**
     * 更新对话的系统指令。
     *
     * @param conversationId 对话ID。
     * @param systemInstruction 新的系统指令字符串，如果为null则清除。
     */
    suspend fun updateSystemInstruction(conversationId: String, systemInstruction: String?)

    /**
     * 根据ID获取对话头部。
     *
     * @param conversationId 对话ID。
     * @return 对应的ConversationHeader对象，如果不存在则为null。
     */
    suspend fun getConversationHeaderById(conversationId: String): ConversationHeader?

    /**
     * 更新对话的总令牌计数。
     *
     * @param conversationId 对话ID。
     * @param totalTokenCount 新的总令牌计数。
     */
    suspend fun updateTotalTokenCount(conversationId: String, totalTokenCount: Int)
}
