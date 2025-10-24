package com.exp.memoria.data.repository

import android.util.Log
import com.exp.memoria.data.local.dao.ConversationHeaderDao
import com.exp.memoria.data.local.dao.RawMemoryDao
import com.exp.memoria.data.local.entity.ConversationHeader
import com.exp.memoria.data.local.entity.ConversationInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ConversationRepositoryImpl @Inject constructor(
    private val conversationHeaderDao: ConversationHeaderDao,
    private val rawMemoryDao: RawMemoryDao
) : ConversationRepository {

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

    override suspend fun getConversationHeaderById(conversationId: String): ConversationHeader? {
        return conversationHeaderDao.getConversationHeaderById(conversationId)
    }

    override suspend fun updateTotalTokenCount(conversationId: String, totalTokenCount: Int) {
        conversationHeaderDao.updateTotalTokenCount(conversationId, totalTokenCount)
    }
}
