package com.exp.memoria.data.repository

import com.exp.memoria.data.local.dao.CondensedMemoryDao
import com.exp.memoria.data.local.dao.RawMemoryDao
import com.exp.memoria.data.local.entity.CondensedMemory
import com.exp.memoria.data.local.entity.RawMemory
import com.exp.memoria.data.model.FileAttachment
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val rawMemoryDao: RawMemoryDao,
    private val condensedMemoryDao: CondensedMemoryDao,
    private val fileAttachmentRepository: FileAttachmentRepository
) : MessageRepository {

    override suspend fun saveNewMemory(query: String, response: String, conversationId: String, attachments: List<FileAttachment>): Long {
        val now = System.currentTimeMillis()

        // 插入用户消息 (query)
        val userMemory = RawMemory(
            conversationId = conversationId,
            sender = "user",
            text = query,
            timestamp = now - 1 // To preserve order
        )
        val userMemoryId = rawMemoryDao.insert(userMemory)

        // 保存与用户消息关联的附件
        attachments.forEach { attachment ->
            val messageFile = com.exp.memoria.data.local.entity.MessageFile(
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

        // 创建一个与模型响应关联的精简记忆
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

        // 创建一个与模型响应关联的精简记忆
        val condensedMemory = CondensedMemory(
            raw_memory_id = modelMemoryId,
            conversationId = conversationId,
            summary_text = "", // 摘要将由工作器生成
            vector_int8 = null,
            status = "NEW",
            timestamp = now
        )
        condensedMemoryDao.insert(condensedMemory)

        return modelMemoryId
    }

    override suspend fun getMemoryById(id: Long): RawMemory? {
        return rawMemoryDao.getById(id)
    }

    override suspend fun updateProcessedMemory(id: Long, summary: String, vector: List<Float>) {
        val byteBuffer = ByteBuffer.allocate(vector.size * 4).order(ByteOrder.nativeOrder())
        for (value in vector) {
            byteBuffer.putFloat(value)
        }
        val byteArray = byteBuffer.array()

        condensedMemoryDao.updateProcessedMemory(id, summary, byteArray)
    }

    override suspend fun getAllRawMemories(): List<RawMemory> {
        return rawMemoryDao.getAll()
    }

    override suspend fun getAllRawMemoriesForConversation(conversationId: String): List<RawMemory> {
        return rawMemoryDao.getAllForConversation(conversationId)
    }

    override suspend fun getRawMemories(conversationId: String, limit: Int, offset: Int): List<RawMemory> {
        return rawMemoryDao.getWithLimitOffset(conversationId, limit, offset)
    }

    override suspend fun updateMemoryText(memoryId: Long, newText: String) {
        rawMemoryDao.updateTextById(memoryId, newText)
    }

    override suspend fun deleteFrom(conversationId: String, id: Long) {
        rawMemoryDao.deleteFrom(conversationId, id)
        condensedMemoryDao.deleteFrom(conversationId, id)
    }
}
