package com.exp.memoria.data.repository

import com.exp.memoria.data.local.entity.RawMemory
import com.exp.memoria.data.model.FileAttachment

interface MessageRepository {
    /**
     * 保存一个新的问答对。
     *
     * @param query 用户查询文本。
     * @param response AI响应文本。
     * @param conversationId 对话ID。
     * @param attachments 与用户消息关联的文件附件列表。
     * @return 模型消息的ID。
     */
    suspend fun saveNewMemory(query: String, response: String, conversationId: String, attachments: List<FileAttachment>): Long

    /**
     * 只保存AI的回复（用于重说等场景）。
     *
     * @param userQuery 用户查询文本。
     * @param response AI响应文本。
     * @param conversationId 对话ID。
     * @return 模型消息的ID。
     */
    suspend fun saveOnlyAiResponse(userQuery: String, response: String, conversationId: String): Long

    /**
     * 根据ID获取指定的原始记忆。
     *
     * @param id 记忆的ID。
     * @return 对应的RawMemory对象，如果不存在则为null。
     */
    suspend fun getMemoryById(id: Long): RawMemory?

    /**
     * 更新一个已处理的记忆。
     *
     * @param id 记忆的ID。
     * @param summary 记忆的摘要文本。
     * @param vector 记忆的向量表示。
     */
    suspend fun updateProcessedMemory(id: Long, summary: String, vector: List<Float>)

    /**
     * 获取所有原始记忆。
     *
     * @return 所有RawMemory对象的列表。
     */
    suspend fun getAllRawMemories(): List<RawMemory>

    /**
     * 获取特定对话的所有原始记忆。
     *
     * @param conversationId 对话ID。
     * @return 特定对话的所有RawMemory对象的列表。
     */
    suspend fun getAllRawMemoriesForConversation(conversationId: String): List<RawMemory>

    /**
     * 获取分页的原始记忆。
     *
     * @param conversationId 对话ID。
     * @param limit 返回的最大记忆数量。
     * @param offset 跳过的记忆数量。
     * @return 分页的RawMemory对象的列表。
     */
    suspend fun getRawMemories(conversationId: String, limit: Int, offset: Int): List<RawMemory>

    /**
     * 更新指定记忆的文本内容。
     *
     * @param memoryId 记忆ID。
     * @param newText 新的文本内容。
     */
    suspend fun updateMemoryText(memoryId: Long, newText: String)

    /**
     * 删除指定ID及其之后的所有记忆。
     *
     * @param conversationId 对话ID。
     * @param id 起始删除的记忆ID。
     */
    suspend fun deleteFrom(conversationId: String, id: Long)

    /**
     * 保存用户消息。
     *
     * @param query 用户查询文本。
     * @param conversationId 对话ID。
     * @return 插入的用户消息ID。
     */
    suspend fun saveUserMemory(query: String, conversationId: String): Long
}
