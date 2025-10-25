package com.exp.memoria.data.repository.impl

import com.exp.memoria.data.local.entity.ConversationHeader
import com.exp.memoria.data.local.entity.ConversationInfo
import com.exp.memoria.data.local.entity.MessageFile
import com.exp.memoria.data.local.entity.RawMemory
import com.exp.memoria.data.model.FileAttachment
import com.exp.memoria.data.repository.ConversationRepository
import com.exp.memoria.data.repository.FileAttachmentRepository
import com.exp.memoria.data.repository.MemoryRepository
import com.exp.memoria.data.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * [MemoryRepository] 的实现，作为数据源的统一入口，协调文件附件、消息和对话数据。
 *
 * @property fileAttachmentRepository 用于管理文件附件的仓库。
 * @property messageRepository 用于管理消息（原始记忆）的仓库。
 * @property conversationRepository 用于管理对话的仓库。
 */
class MemoryRepositoryImpl @Inject constructor(
    private val fileAttachmentRepository: FileAttachmentRepository,
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository
) : MemoryRepository {

    /**
     * 保存一个消息文件附件。
     *
     * @param file 要保存的 [MessageFile] 实体。
     * @return 插入的文件的 ID。
     */
    override suspend fun saveMessageFile(file: MessageFile): Long {
        return fileAttachmentRepository.saveMessageFile(file)
    }

    /**
     * 获取特定记忆的所有消息文件附件。
     *
     * @param rawMemoryId 原始记忆的 ID。
     * @return 一个 [MessageFile] 实体列表。
     */
    override suspend fun getMessageFilesForMemory(rawMemoryId: Long): List<MessageFile> {
        return fileAttachmentRepository.getMessageFilesForMemory(rawMemoryId)
    }

    /**
     * 删除一个消息文件附件。
     *
     * @param file 要删除的 [MessageFile] 实体。
     */
    override suspend fun deleteMessageFile(file: MessageFile) {
        fileAttachmentRepository.deleteMessageFile(file)
    }

    /**
     * 保存新的记忆，包括用户查询、AI 响应和附件。
     *
     * @param query 用户查询文本。
     * @param response AI 响应文本。
     * @param conversationId 对话的 ID。
     * @param attachments 附件列表。
     * @return 新保存记忆的 ID。
     */
    override suspend fun saveNewMemory(query: String, response: String, conversationId: String, attachments: List<FileAttachment>): Long {
        conversationRepository.updateConversationLastUpdate(conversationId, System.currentTimeMillis())
        return messageRepository.saveNewMemory(query, response, conversationId, attachments)
    }

    /**
     * 仅保存 AI 响应。
     *
     * @param userQuery 用户查询文本。
     * @param response AI 响应文本。
     * @param conversationId 对话的 ID。
     * @return 新保存记忆的 ID。
     */
    override suspend fun saveOnlyAiResponse(userQuery: String, response: String, conversationId: String): Long {
        conversationRepository.updateConversationLastUpdate(conversationId, System.currentTimeMillis())
        return messageRepository.saveOnlyAiResponse(userQuery, response, conversationId)
    }

    /**
     * 根据 ID 获取原始记忆。
     *
     * @param id 记忆的 ID。
     * @return 如果找到，则返回 [RawMemory]；否则返回 null。
     */
    override suspend fun getMemoryById(id: Long): RawMemory? {
        return messageRepository.getMemoryById(id)
    }

    /**
     * 更新已处理记忆的摘要和向量。
     *
     * @param id 记忆的 ID。
     * @param summary 记忆的摘要。
     * @param vector 记忆的向量表示。
     */
    override suspend fun updateProcessedMemory(id: Long, summary: String, vector: List<Float>) {
        messageRepository.updateProcessedMemory(id, summary, vector)
    }

    /**
     * 获取所有原始记忆。
     *
     * @return 所有原始记忆的列表。
     */
    override suspend fun getAllRawMemories(): List<RawMemory> {
        return messageRepository.getAllRawMemories()
    }

    /**
     * 获取特定对话的所有原始记忆。
     *
     * @param conversationId 对话的 ID。
     * @return 特定对话的所有原始记忆的列表。
     */
    override suspend fun getAllRawMemoriesForConversation(conversationId: String): List<RawMemory> {
        return messageRepository.getAllRawMemoriesForConversation(conversationId)
    }

    /**
     * 获取特定对话的原始记忆，支持分页。
     *
     * @param conversationId 对话的 ID。
     * @param limit 返回记忆的最大数量。
     * @param offset 偏移量，用于分页。
     * @return 原始记忆的列表。
     */
    override suspend fun getRawMemories(conversationId: String, limit: Int, offset: Int): List<RawMemory> {
        return messageRepository.getRawMemories(conversationId, limit, offset)
    }

    /**
     * 更新记忆的文本内容。
     *
     * @param memoryId 记忆的 ID。
     * @param newText 新的文本内容。
     */
    override suspend fun updateMemoryText(memoryId: Long, newText: String) {
        messageRepository.updateMemoryText(memoryId, newText)
    }

    /**
     * 从特定对话中删除记忆。
     *
     * @param conversationId 对话的 ID。
     * @param id 要删除的记忆的 ID。
     */
    override suspend fun deleteFrom(conversationId: String, id: Long) {
        messageRepository.deleteFrom(conversationId, id)
    }

    /**
     * 删除指定对话中的所有消息。
     *
     * @param conversationId 对话ID。
     */
    override suspend fun deleteAllMessagesInConversation(conversationId: String) {
        messageRepository.deleteAllMessagesInConversation(conversationId)
    }

    /**
     * 保存用户记忆。
     *
     * @param query 用户查询文本。
     * @param conversationId 对话的 ID。
     * @return 新保存用户记忆的 ID。
     */
    override suspend fun saveUserMemory(query: String, conversationId: String): Long {
        conversationRepository.updateConversationLastUpdate(conversationId, System.currentTimeMillis())
        return messageRepository.saveUserMemory(query, conversationId)
    }

    /**
     * 获取所有对话的列表，并附带其最新信息。
     *
     * @return 一个 Flow，它发出一个 [ConversationInfo] 列表，按最后更新时间戳降序排序。
     */
    override fun getConversations(): Flow<List<ConversationInfo>> {
        return conversationRepository.getConversations()
    }

    /**
     * 创建一个新的对话。
     *
     * @param conversationId 要创建的对话的唯一标识符。
     */
    override suspend fun createNewConversation(conversationId: String) {
        conversationRepository.createNewConversation(conversationId)
    }

    /**
     * 更新对话的最后更新时间戳。
     *
     * @param conversationId 要更新的对话的 ID。
     * @param timestamp 新的时间戳。
     */
    override suspend fun updateConversationLastUpdate(conversationId: String, timestamp: Long) {
        conversationRepository.updateConversationLastUpdate(conversationId, timestamp)
    }

    /**
     * 删除一个对话及其所有相关的记忆。
     *
     * @param conversationId 要删除的对话的 ID。
     */
    override suspend fun deleteConversation(conversationId: String) {
        // 1. 获取该对话下的所有记忆，以便找到它们关联的文件
        val memories = messageRepository.getAllRawMemoriesForConversation(conversationId)
        for (memory in memories) {
            // 2. 删除每个记忆所关联的所有文件
            val files = fileAttachmentRepository.getMessageFilesForMemory(memory.id)
            for (file in files) {
                fileAttachmentRepository.deleteMessageFile(file)
            }
        }

        // 3. 删除该对话下的所有消息（raw_memory, condensed_memory等）
        messageRepository.deleteAllMessagesInConversation(conversationId)

        // 4. 最后删除对话头
        conversationRepository.deleteConversation(conversationId)
    }

    /**
     * 重命名一个对话。
     *
     * @param conversationId 要重命名的对话的 ID。
     * @param newName 对话的新名称。
     */
    override suspend fun renameConversation(conversationId: String, newName: String) {
        conversationRepository.renameConversation(conversationId, newName)
    }

    /**
     * 更新对话的响应模式。
     *
     * @param conversationId 要更新的对话的 ID。
     * @param responseSchema 新的响应模式。
     */
    override suspend fun updateResponseSchema(conversationId: String, responseSchema: String?) {
        conversationRepository.updateResponseSchema(conversationId, responseSchema)
    }

    /**
     * 更新对话的系统指令。
     *
     * @param conversationId 要更新的对话的 ID。
     * @param systemInstruction 新的系统指令。
     */
    override suspend fun updateSystemInstruction(conversationId: String, systemInstruction: String?) {
        conversationRepository.updateSystemInstruction(conversationId, systemInstruction)
    }

    /**
     * 按 ID 获取对话标题。
     *
     * @param conversationId 对话的 ID。
     * @return 如果找到，则返回 [ConversationHeader]；否则返回 null。
     */
    override suspend fun getConversationHeaderById(conversationId: String): ConversationHeader? {
        return conversationRepository.getConversationHeaderById(conversationId)
    }

    /**
     * 更新对话的总令牌数。
     *
     * @param conversationId 要更新的对话的 ID。
     * @param totalTokenCount 新的总令牌数。
     */
    override suspend fun updateTotalTokenCount(conversationId: String, totalTokenCount: Int) {
        conversationRepository.updateTotalTokenCount(conversationId, totalTokenCount)
    }
}
