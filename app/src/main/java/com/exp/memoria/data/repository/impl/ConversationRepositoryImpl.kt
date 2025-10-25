package com.exp.memoria.data.repository.impl

import android.util.Log
import com.exp.memoria.data.local.dao.ConversationHeaderDao
import com.exp.memoria.data.local.dao.RawMemoryDao
import com.exp.memoria.data.local.entity.ConversationHeader
import com.exp.memoria.data.local.entity.ConversationInfo
import com.exp.memoria.data.repository.ConversationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * [ConversationRepository] 的实现，用于管理对话数据。
 *
 * @property conversationHeaderDao 用于访问对话标题数据的 DAO。
 * @property rawMemoryDao 用于访问原始记忆数据的 DAO。
 */
class ConversationRepositoryImpl @Inject constructor(
    private val conversationHeaderDao: ConversationHeaderDao,
    private val rawMemoryDao: RawMemoryDao
) : ConversationRepository {

    /**
     * 获取所有对话的列表，并附带其最新信息。
     *
     * @return 一个 Flow，它发出一个 [ConversationInfo] 列表，按最后更新时间戳降序排序。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getConversations(): Flow<List<ConversationInfo>> {
        return conversationHeaderDao.getAllConversationHeaders()
            .flatMapLatest { headers ->
                if (headers.isEmpty()) {
                    return@flatMapLatest flowOf(emptyList())
                }
                val conversationInfoFlows = headers.map { header ->
                    rawMemoryDao.getLatestMemoryForConversation(header.conversationId)
                        .map { latestRawMemory ->
                            ConversationInfo(
                                conversationId = header.conversationId,
                                name = header.name,
                                lastTimestamp = latestRawMemory?.timestamp ?: header.creationTimestamp
                            )
                        }
                }
                combine(conversationInfoFlows) { conversationInfos ->
                    conversationInfos.sortedByDescending { it.lastTimestamp }
                }
            }
    }

    /**
     * 创建一个新的对话。
     *
     * @param conversationId 要创建的对话的唯一标识符。
     */
    override suspend fun createNewConversation(conversationId: String) {
        val now = System.currentTimeMillis()
        val newHeader = ConversationHeader(conversationId, "新对话", now, now)
        conversationHeaderDao.insert(newHeader)
    }

    /**
     * 更新对话的最后更新时间戳。
     *
     * @param conversationId 要更新的对话的 ID。
     * @param timestamp 新的时间戳。
     */
    override suspend fun updateConversationLastUpdate(conversationId: String, timestamp: Long) {
        val header = conversationHeaderDao.getConversationHeaderById(conversationId)
        header?.let {
            conversationHeaderDao.update(it.copy(lastUpdateTimestamp = timestamp))
        }
    }

    /**
     * 删除一个对话及其所有相关的记忆。
     *
     * @param conversationId 要删除的对话的 ID。
     */
    override suspend fun deleteConversation(conversationId: String) {
        conversationHeaderDao.deleteByConversationId(conversationId)
    }

    /**
     * 重命名一个对话。
     *
     * @param conversationId 要重命名的对话的 ID。
     * @param newName 对话的新名称。
     */
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

    /**
     * 更新对话的响应模式。
     *
     * @param conversationId 要更新的对话的 ID。
     * @param responseSchema 新的响应模式。
     */
    override suspend fun updateResponseSchema(conversationId: String, responseSchema: String?) {
        Log.d("MemoryRepository", "[Schema] Attempting to update for ID: $conversationId")
        conversationHeaderDao.getConversationHeaderById(conversationId)?.let {
            Log.d(
                "MemoryRepository",
                "[Schema] Found header. Current schema: '${it.responseSchema}'. New schema: '$responseSchema'"
            )
            val updatedHeader = it.copy(responseSchema = responseSchema)
            conversationHeaderDao.update(updatedHeader)
            Log.d("MemoryRepository", "[Schema] DAO update called for ID: $conversationId")
        } ?: run {
            Log.w("MemoryRepository", "[Schema] Header NOT FOUND for ID: $conversationId. Update failed.")
        }
    }

    /**
     * 更新对话的系统指令。
     *
     * @param conversationId 要更新的对话的 ID。
     * @param systemInstruction 新的系统指令。
     */
    override suspend fun updateSystemInstruction(conversationId: String, systemInstruction: String?) {
        Log.d("MemoryRepository", "[Instruction] Attempting to update for ID: $conversationId")
        conversationHeaderDao.getConversationHeaderById(conversationId)?.let {
            Log.d(
                "MemoryRepository",
                "[Instruction] Found header. Current instruction: '${it.systemInstruction}'. New instruction: '$systemInstruction'"
            )
            val updatedHeader = it.copy(systemInstruction = systemInstruction)
            conversationHeaderDao.update(updatedHeader)
            Log.d("MemoryRepository", "[Instruction] DAO update called for ID: $conversationId")
        } ?: run {
            Log.w("MemoryRepository", "[Instruction] Header NOT FOUND for ID: $conversationId. Update failed.")
        }
    }

    /**
     * 按 ID 获取对话标题。
     *
     * @param conversationId 对话的 ID。
     * @return 如果找到，则返回 [ConversationHeader]；否则返回 null。
     */
    override suspend fun getConversationHeaderById(conversationId: String): ConversationHeader? {
        return conversationHeaderDao.getConversationHeaderById(conversationId)
    }

    /**
     * 更新对话的总令牌数。
     *
     * @param conversationId 要更新的对话的 ID。
     * @param totalTokenCount 新的总令牌数。
     */
    override suspend fun updateTotalTokenCount(conversationId: String, totalTokenCount: Int) {
        conversationHeaderDao.updateTotalTokenCount(conversationId, totalTokenCount)
    }
}
