package com.exp.memoria.data.repository

import android.util.Log
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
 *    - `deleteConversation(...)`: 删除指定 ID 的对话及其所有相关记忆。
 *    - `renameConversation(...)`: 重命名指定 ID 的对话。
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
    suspend fun getAllRawMemoriesForConversation(conversationId: String): List<RawMemory>
    suspend fun getRawMemories(conversationId: String, limit: Int, offset: Int): List<RawMemory>
    suspend fun getConversations(): List<ConversationInfo>
    suspend fun createNewConversation(conversationId: String)
    suspend fun updateConversationLastUpdate(conversationId: String, timestamp: Long)
    suspend fun deleteConversation(conversationId: String)
    suspend fun renameConversation(conversationId: String, newName: String)
}

class MemoryRepositoryImpl @Inject constructor(
    private val rawMemoryDao: RawMemoryDao,
    private val condensedMemoryDao: CondensedMemoryDao,
    private val conversationHeaderDao: ConversationHeaderDao
) : MemoryRepository {

    override suspend fun saveNewMemory(query: String, response: String, conversationId: String): Long {
        if (conversationHeaderDao.countConversationHeaders(conversationId) == 0) {
            val now = System.currentTimeMillis()
            conversationHeaderDao.insert(ConversationHeader(conversationId, "新对话", now, now))
        } else {
            val existingHeader = conversationHeaderDao.getConversationHeaderById(conversationId)
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
            conversationId = conversationId, // 修复：添加 conversationId 参数
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
        val conversationHeaders = conversationHeaderDao.getAllConversationHeaders()
        return conversationHeaders.map { header ->
            val latestRawMemory = rawMemoryDao.getLatestMemoryForConversation(header.conversationId)
            ConversationInfo(
                conversationId = header.conversationId,
                name = header.name,
                lastTimestamp = latestRawMemory?.timestamp ?: header.creationTimestamp
            )
        }
    }

    override suspend fun createNewConversation(conversationId: String) {
        val now = System.currentTimeMillis()
        val newHeader = ConversationHeader(conversationId, "新对话", now, now)
        conversationHeaderDao.insert(newHeader)
    }

    override suspend fun updateConversationLastUpdate(conversationId: String, timestamp: Long) {
        val header = conversationHeaderDao.getConversationHeaderById(conversationId)
        header?.let {
            conversationHeaderDao.update(it.copy(lastUpdateTimestamp = timestamp))
        }
    }

    override suspend fun deleteConversation(conversationId: String) {
        rawMemoryDao.deleteByConversationId(conversationId)
        condensedMemoryDao.deleteByConversationId(conversationId)
        conversationHeaderDao.deleteByConversationId(conversationId)
    }

    override suspend fun renameConversation(conversationId: String, newName: String) {
        Log.d("MemoryRepository", "Attempting to rename conversation. ID: $conversationId, New Name: $newName")
        val header = conversationHeaderDao.getConversationHeaderById(conversationId)
        header?.let {
            Log.d("MemoryRepository", "Found existing header: $it")
            val updatedHeader = ConversationHeader(
                conversationId = it.conversationId,
                name = newName,
                creationTimestamp = it.creationTimestamp,
                lastUpdateTimestamp = System.currentTimeMillis()
            )
            Log.d("MemoryRepository", "Updating header to: $updatedHeader")
            conversationHeaderDao.update(updatedHeader)
            Log.d("MemoryRepository", "Conversation renamed successfully in DAO.")
        } ?: run {
            Log.w("MemoryRepository", "Conversation header not found for ID: $conversationId. Cannot rename.")
        }
    }
}
