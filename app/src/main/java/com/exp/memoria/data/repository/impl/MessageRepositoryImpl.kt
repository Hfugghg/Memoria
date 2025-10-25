package com.exp.memoria.data.repository.impl

import com.exp.memoria.data.local.dao.CondensedMemoryDao
import com.exp.memoria.data.local.dao.RawMemoryDao
import com.exp.memoria.data.local.entity.CondensedMemory
import com.exp.memoria.data.local.entity.MessageFile
import com.exp.memoria.data.local.entity.RawMemory
import com.exp.memoria.data.model.FileAttachment
import com.exp.memoria.data.repository.FileAttachmentRepository
import com.exp.memoria.data.repository.MessageRepository
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

/**
 * [MessageRepository] 的实现，用于管理原始记忆（消息）和精简记忆。
 *
 * @property rawMemoryDao 用于访问原始记忆数据的 DAO。
 * @property condensedMemoryDao 用于访问精简记忆数据的 DAO。
 * @property fileAttachmentRepository 用于管理文件附件的仓库。
 */
class MessageRepositoryImpl @Inject constructor(
    private val rawMemoryDao: RawMemoryDao,
    private val condensedMemoryDao: CondensedMemoryDao,
    private val fileAttachmentRepository: FileAttachmentRepository
) : MessageRepository {

    /**
     * 保存一次完整的交互，包括用户查询、AI 响应和附件。
     *
     * @param query 用户的查询文本。
     * @param response AI 的响应文本。
     * @param conversationId 对话的唯一标识符。
     * @param attachments 与用户查询关联的文件附件列表。
     * @return 返回保存的 AI 响应 [RawMemory] 的 ID。
     */
    override suspend fun saveNewMemory(query: String, response: String, conversationId: String, attachments: List<FileAttachment>): Long {
        val now = System.currentTimeMillis()

        // 插入用户消息 (query)
        val userMemory = RawMemory(
            conversationId = conversationId,
            sender = "user",
            text = query,
            timestamp = now - 1 // 确保顺序
        )
        val userMemoryId = rawMemoryDao.insert(userMemory)

        // 保存与用户消息关联的附件
        attachments.forEach { attachment ->
            val messageFile = MessageFile(
                rawMemoryId = userMemoryId,
                fileName = attachment.fileName,
                fileType = attachment.fileType ?: "application/octet-stream",
                fileContentBase64 = attachment.base64Data
            )
            fileAttachmentRepository.saveMessageFile(messageFile)
        }

        // 插入模型消息 (response)
        val modelMemory = RawMemory(
            conversationId = conversationId,
            sender = "model",
            text = response,
            timestamp = now
        )
        val modelMemoryId = rawMemoryDao.insert(modelMemory)

        // 创建一个与模型响应关联的精简记忆，等待后续处理
        val condensedMemory = CondensedMemory(
            raw_memory_id = modelMemoryId,
            conversationId = conversationId,
            summary_text = "",
            vector_int8 = null,
            status = "NEW",
            timestamp = now
        )
        condensedMemoryDao.insert(condensedMemory)

        return modelMemoryId
    }

    /**
     * 仅保存用户的查询消息。
     *
     * @param query 用户的查询文本。
     * @param conversationId 对话的唯一标识符。
     * @return 返回保存的用户消息 [RawMemory] 的 ID。
     */
    override suspend fun saveUserMemory(query: String, conversationId: String): Long {
        val now = System.currentTimeMillis()

        // 插入用户消息 (query)
        val userMemory = RawMemory(
            conversationId = conversationId,
            sender = "user",
            text = query,
            timestamp = now
        )
        return rawMemoryDao.insert(userMemory)
    }

    /**
     * 仅保存 AI 的响应消息，并为其创建待处理的精简记忆。
     *
     * @param userQuery 触发此响应的用户查询（当前未使用，但为上下文保留）。
     * @param response AI 的响应文本。
     * @param conversationId 对话的唯一标识符。
     * @return 返回保存的 AI 响应 [RawMemory] 的 ID。
     */
    override suspend fun saveOnlyAiResponse(userQuery: String, response: String, conversationId: String): Long {
        val now = System.currentTimeMillis()

        // 插入模型消息 (response)
        val modelMemory = RawMemory(
            conversationId = conversationId,
            sender = "model",
            text = response,
            timestamp = now
        )
        val modelMemoryId = rawMemoryDao.insert(modelMemory)

        // 创建一个与模型响应关联的精简记忆，等待后续处理
        val condensedMemory = CondensedMemory(
            raw_memory_id = modelMemoryId,
            conversationId = conversationId,
            summary_text = "", // 摘要将由后台工作器生成
            vector_int8 = null,
            status = "NEW",
            timestamp = now
        )
        condensedMemoryDao.insert(condensedMemory)

        return modelMemoryId
    }

    /**
     * 根据 ID 获取原始记忆。
     *
     * @param id 记忆的 ID。
     * @return 如果找到，则返回 [RawMemory] 实体；否则返回 null。
     */
    override suspend fun getMemoryById(id: Long): RawMemory? {
        return rawMemoryDao.getById(id)
    }

    /**
     * 更新已处理的精简记忆，存入摘要和向量数据。
     *
     * @param id 原始记忆的 ID，与精简记忆关联。
     * @param summary 生成的摘要文本。
     * @param vector 生成的浮点数向量。
     */
    override suspend fun updateProcessedMemory(id: Long, summary: String, vector: List<Float>) {
        val byteBuffer = ByteBuffer.allocate(vector.size * 4).order(ByteOrder.nativeOrder())
        for (value in vector) {
            byteBuffer.putFloat(value)
        }
        val byteArray = byteBuffer.array()

        condensedMemoryDao.updateProcessedMemory(id, summary, byteArray)
    }

    /**
     * 获取数据库中所有的原始记忆。
     *
     * @return 所有 [RawMemory] 实体的列表。
     */
    override suspend fun getAllRawMemories(): List<RawMemory> {
        return rawMemoryDao.getAll()
    }

    /**
     * 获取特定对话的所有原始记忆。
     *
     * @param conversationId 对话的唯一标识符。
     * @return 该对话的 [RawMemory] 实体列表。
     */
    override suspend fun getAllRawMemoriesForConversation(conversationId: String): List<RawMemory> {
        return rawMemoryDao.getAllForConversation(conversationId)
    }

    /**
     * 使用分页获取特定对话的原始记忆。
     *
     * @param conversationId 对话的唯一标识符。
     * @param limit 要获取的记忆数量。
     * @param offset 查询的起始偏移量。
     * @return 分页后的 [RawMemory] 实体列表。
     */
    override suspend fun getRawMemories(conversationId: String, limit: Int, offset: Int): List<RawMemory> {
        return rawMemoryDao.getWithLimitOffset(conversationId, limit, offset)
    }

    /**
     * 更新特定记忆的文本内容。
     *
     * @param memoryId 要更新的记忆的 ID。
     * @param newText 新的文本内容。
     */
    override suspend fun updateMemoryText(memoryId: Long, newText: String) {
        rawMemoryDao.updateTextById(memoryId, newText)
    }

    /**
     * 从特定对话中删除某个时间点之后的所有记忆。
     *
     * @param conversationId 对话的唯一标识符。
     * @param id 作为删除起点的记忆 ID。
     */
    override suspend fun deleteFrom(conversationId: String, id: Long) {
        rawMemoryDao.deleteFrom(conversationId, id)
        condensedMemoryDao.deleteFrom(conversationId, id)
    }
}
