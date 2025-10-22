package com.exp.memoria.data.repository

import android.util.Log
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
 *    - `saveOnlyAiResponse(...)`: 只保存AI的回复（用于重说等场景）。
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
 *    - `updateMemoryText(...)`: 更新指定记忆的文本内容。
 *    - `deleteFrom(...)`: 删除指定ID及其之后的所有记忆。
 *
 * 关联:
 * - 它会注入 RawMemoryDao 和 CondensedMemoryDao。
 * - GetChatResponseUseCase 会注入并使用这个Repository来管理记忆数据。
 */
interface MemoryRepository {
    suspend fun saveNewMemory(query: String, response: String, conversationId: String): Long
    suspend fun saveOnlyAiResponse(userQuery: String, response: String, conversationId: String): Long
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
    suspend fun updateResponseSchema(conversationId: String, responseSchema: String?)
    suspend fun updateSystemInstruction(conversationId: String, systemInstruction: String?)
    suspend fun getConversationHeaderById(conversationId: String): ConversationHeader?
    suspend fun updateTotalTokenCount(conversationId: String, totalTokenCount: Int)
    suspend fun updateMemoryText(memoryId: Long, newText: String)
    suspend fun deleteFrom(conversationId: String, id: Long)
}

class MemoryRepositoryImpl @Inject constructor(
    private val rawMemoryDao: RawMemoryDao,
    private val condensedMemoryDao: CondensedMemoryDao,
    private val conversationHeaderDao: ConversationHeaderDao
) : MemoryRepository {

    override suspend fun saveNewMemory(query: String, response: String, conversationId: String): Long {
        val now = System.currentTimeMillis()

        // Ensure conversation header exists or create a new one, and update timestamp
        if (conversationHeaderDao.countConversationHeaders(conversationId) == 0) {
            conversationHeaderDao.insert(ConversationHeader(conversationId, "新对话", now, now))
        } else {
            conversationHeaderDao.getConversationHeaderById(conversationId)?.let {
                conversationHeaderDao.update(it.copy(lastUpdateTimestamp = now))
            }
        }

        // Insert user message (query)
        val userMemory = RawMemory(
            conversationId = conversationId,
            sender = "user",
            text = query,
            timestamp = now - 1 // To preserve order
        )
        rawMemoryDao.insert(userMemory)

        // Insert model message (response)
        val modelMemory = RawMemory(
            conversationId = conversationId,
            sender = "model",
            text = response,
            timestamp = now
        )
        val modelMemoryId = rawMemoryDao.insert(modelMemory)

        // Create a condensed memory linked to the model's response
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

    override suspend fun saveOnlyAiResponse(userQuery: String, response: String, conversationId: String): Long {
        val now = System.currentTimeMillis()

        // Ensure conversation header exists and update its timestamp
        conversationHeaderDao.getConversationHeaderById(conversationId)?.let {
            conversationHeaderDao.update(it.copy(lastUpdateTimestamp = now))
        } // If header doesn't exist, we assume it's an anomaly and proceed, as saveNewMemory should have created it.

        // Insert model message (response)
        val modelMemory = RawMemory(
            conversationId = conversationId,
            sender = "model",
            text = response,
            timestamp = now
        )
        val modelMemoryId = rawMemoryDao.insert(modelMemory)

        // Create a condensed memory linked to the model's response
        val condensedMemory = CondensedMemory(
            raw_memory_id = modelMemoryId,
            conversationId = conversationId,
            summary_text = "", // The summary will be generated by the worker
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
        conversationHeaderDao.getConversationHeaderById(conversationId)?.let { header ->
            Log.d("MemoryRepository", "Found existing header: $header")
            val updatedHeader = header.copy(
                name = newName,
                lastUpdateTimestamp = System.currentTimeMillis()
            )
            Log.d("MemoryRepository", "Updating header to: $updatedHeader")
            conversationHeaderDao.update(updatedHeader)
            Log.d("MemoryRepository", "Conversation renamed successfully in DAO.")
        } ?: run {
            Log.w("MemoryRepository", "Conversation header not found for ID: $conversationId. Cannot rename.")
        }
    }

    override suspend fun updateResponseSchema(conversationId: String, responseSchema: String?) {
        Log.d("MemoryRepository", "[Schema] Attempting to update for ID: $conversationId")
        conversationHeaderDao.getConversationHeaderById(conversationId)?.let { header ->
            Log.d(
                "MemoryRepository",
                "[Schema] Found header. Current schema: '${header.responseSchema}'. New schema: '$responseSchema'"
            )
            val updatedHeader = header.copy(responseSchema = responseSchema)
            conversationHeaderDao.update(updatedHeader)
            Log.d("MemoryRepository", "[Schema] DAO update called for ID: $conversationId")
        } ?: run {
            Log.w("MemoryRepository", "[Schema] Header NOT FOUND for ID: $conversationId. Update failed.")
        }
    }

    override suspend fun updateSystemInstruction(conversationId: String, systemInstruction: String?) {
        Log.d("MemoryRepository", "[Instruction] Attempting to update for ID: $conversationId")
        conversationHeaderDao.getConversationHeaderById(conversationId)?.let { header ->
            Log.d(
                "MemoryRepository",
                "[Instruction] Found header. Current instruction: '${header.systemInstruction}'. New instruction: '$systemInstruction'"
            )
            val updatedHeader = header.copy(systemInstruction = systemInstruction)
            conversationHeaderDao.update(updatedHeader)
            Log.d("MemoryRepository", "[Instruction] DAO update called for ID: $conversationId")
        } ?: run {
            Log.w("MemoryRepository", "[Instruction] Header NOT FOUND for ID: $conversationId. Update failed.")
        }
    }

    override suspend fun getConversationHeaderById(conversationId: String): ConversationHeader? {
        return conversationHeaderDao.getConversationHeaderById(conversationId)
    }

    override suspend fun updateTotalTokenCount(conversationId: String, totalTokenCount: Int) {
        conversationHeaderDao.updateTotalTokenCount(conversationId, totalTokenCount)
    }

    override suspend fun updateMemoryText(memoryId: Long, newText: String) {
        rawMemoryDao.updateTextById(memoryId, newText)
    }

    override suspend fun deleteFrom(conversationId: String, id: Long) {
        rawMemoryDao.deleteFrom(conversationId, id)
        // 考虑到 CondensedMemory 是 RawMemory 的子集，当 RawMemory 被删除时，
        // 对应的 CondensedMemory 也应该被删除。这里假设 CondensedMemory 的 id 和 RawMemory 的 id 相同。
        // 如果不是，需要调整删除逻辑。
        condensedMemoryDao.deleteFrom(conversationId, id)
    }
}
