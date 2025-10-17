package com.exp.memoria.data.repository

import com.exp.memoria.data.Content
import com.exp.memoria.data.Part
import com.exp.memoria.data.local.dao.CondensedMemoryDao
import com.exp.memoria.data.local.dao.ConversationHeaderDao
import com.exp.memoria.data.local.dao.RawMemoryDao
import com.exp.memoria.data.local.entity.CondensedMemory
import com.exp.memoria.data.local.entity.ConversationHeader
import com.exp.memoria.data.local.entity.ConversationInfo
import com.exp.memoria.data.local.entity.RawMemory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

/**
 * [记忆数据仓库]
 *
 * 职责:
 * 1. 作为ViewModel与本地数据源（DAO）之间的唯一中间层。
 * 2. 封装所有与记忆存储和检索相关的业务逻辑，对上层屏蔽数据库细节。
 * 3. 提供方法：
 *    - `saveNewMemory(...)`: 保存一个新的问答对。
 *    - `getMemoryById(...)`: 获取指定的记忆。
 *    - `updateProcessedMemory(...)`: 更新一个已处理的记忆。
 *    - `getAllRawMemories()`: 获取所有原始记忆。
 *    - `getRawMemories(...)`: 获取分页的原始记忆。
 *    - `getConversations()`: 获取所有对话的列表。
 *    - `createNewConversation(...)`: 创建一个新的对话头部记录。
 *    - `updateConversationLastUpdate(...)`: 更新对话的最后更新时间。
 *    - `getAllRawMemoriesForConversation(...)`: 获取特定对话的所有原始记忆。
 *
 * 关联:
 * - 它会注入 RawMemoryDao 和 CondensedMemoryDao。
 * - GetChatResponseUseCase 会注入并使用这个Repository来管理记忆数据。
 */
interface MemoryRepository {
    suspend fun saveNewMemory(query: String, response: String, conversationId: String): Long
    suspend fun getMemoryById(id: Long): RawMemory?
    suspend fun updateProcessedMemory(id: Long, summary: String, vector: List<Float>)
    suspend fun getAllRawMemories(): List<RawMemory>
    suspend fun getAllRawMemoriesForConversation(conversationId: String): List<RawMemory> // 新增方法
    suspend fun getRawMemories(conversationId: String, limit: Int, offset: Int): List<RawMemory>
    suspend fun getConversations(): List<ConversationInfo>
    suspend fun createNewConversation(conversationId: String)
    suspend fun updateConversationLastUpdate(conversationId: String, timestamp: Long)
}

class MemoryRepositoryImpl @Inject constructor(
    private val rawMemoryDao: RawMemoryDao,
    private val condensedMemoryDao: CondensedMemoryDao,
    private val conversationHeaderDao: ConversationHeaderDao // 注入 ConversationHeaderDao
) : MemoryRepository {

    override suspend fun saveNewMemory(query: String, response: String, conversationId: String): Long {
        // 确保对话头部存在并更新最后更新时间
        if (conversationHeaderDao.countConversationHeaders(conversationId) == 0) {
            val now = System.currentTimeMillis()
            conversationHeaderDao.insert(ConversationHeader(conversationId, now, now))
        } else {
            // 如果对话已存在，更新其最后更新时间
            val existingHeader = conversationHeaderDao.getAllConversationHeaders().firstOrNull { it.conversationId == conversationId }
            existingHeader?.let {
                conversationHeaderDao.update(it.copy(lastUpdateTimestamp = System.currentTimeMillis()))
            }
        }

        val newContents = listOf(
            Content.User(parts = listOf(Part.Text(query))),
            Content.Model(parts = listOf(Part.Text(response)))
        )

        val newMemory = RawMemory(
            conversationId = conversationId,
            contents = newContents,
            timestamp = System.currentTimeMillis()
        )
        val rawMemoryId = rawMemoryDao.insert(newMemory)

        val condensedMemory = CondensedMemory(
            raw_memory_id = rawMemoryId,
            summary_text = "",
            vector_int8 = null,
            status = "NEW",
            timestamp = System.currentTimeMillis()
        )
        condensedMemoryDao.insert(condensedMemory)

        return rawMemoryId
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

    override suspend fun getConversations(): List<ConversationInfo> {
        // 从 ConversationHeader 获取所有对话，并尝试获取其最新的 RawMemory 记录来补充信息
        val conversationHeaders = conversationHeaderDao.getAllConversationHeaders()
        return conversationHeaders.map { header ->
            val latestRawMemory = rawMemoryDao.getLatestMemoryForConversation(header.conversationId)
            ConversationInfo(
                conversationId = header.conversationId,
                lastTimestamp = latestRawMemory?.timestamp ?: header.creationTimestamp // 如果没有聊天记录，使用创建时间
            )
        }
    }

    override suspend fun createNewConversation(conversationId: String) {
        val now = System.currentTimeMillis()
        val newHeader = ConversationHeader(conversationId, now, now)
        conversationHeaderDao.insert(newHeader)
    }

    override suspend fun updateConversationLastUpdate(conversationId: String, timestamp: Long) {
        val header = conversationHeaderDao.getAllConversationHeaders().firstOrNull { it.conversationId == conversationId }
        header?.let {
            conversationHeaderDao.update(it.copy(lastUpdateTimestamp = timestamp))
        }
    }
}
