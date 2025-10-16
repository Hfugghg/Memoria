package com.exp.memoria.data.repository

import com.exp.memoria.data.local.dao.CondensedMemoryDao
import com.exp.memoria.data.local.dao.RawMemoryDao
import com.exp.memoria.data.local.entity.CondensedMemory
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
 * - `getHotMemories()`: 从RawMemoryDao获取最近的对话作为“热记忆” [cite: 16, 32]。
 * - `saveNewMemory(qa: Pair<String, String>)`: 保存一个新的问答对，同时在 RawMemory 表和 CondensedMemory 表中创建初始记录（状态为"NEW"）。
 * - `findRelevantColdMemories(query: String)`: 实现“FTS预过滤 + 向量精排”的二级检索逻辑 [cite: 33]。此方法会调用CondensedMemoryDao中的FTS搜索和向量查询方法。
 * - `getMemoriesToProcess()`: 获取所有状态为"NEW"的浓缩记忆，供后台Worker使用。
 * - `updateProcessedMemory(...)`: 更新已处理的浓缩记忆（摘要、向量、状态等）。
 *
 * 关联:
 * - 它会注入 RawMemoryDao 和 CondensedMemoryDao。
 * - GetChatResponseUseCase 和 ProcessMemoryUseCase 会注入并使用这个Repository来管理记忆数据。
 */

interface MemoryRepository {
    // 保存一条新的记忆，并返回其在数据库中的`Long`类型ID。
    suspend fun saveNewMemory(query: String, response: String): Long
    // 根据`Long`类型的ID获取一条原始记忆。
    suspend fun getMemoryById(id: Long): RawMemory?
    // 根据`Long`类型的ID更新一条已处理的记忆。
    suspend fun updateProcessedMemory(id: Long, summary: String, vector: List<Float>)
    // 获取所有原始记忆
    suspend fun getAllRawMemories(): List<RawMemory>
}

class MemoryRepositoryImpl @Inject constructor(
    private val rawMemoryDao: RawMemoryDao,
    private val condensedMemoryDao: CondensedMemoryDao
) : MemoryRepository {

    override suspend fun saveNewMemory(query: String, response: String): Long {
        val newMemory = RawMemory(
            user_query = query,
            llm_response = response,
            timestamp = System.currentTimeMillis()
        )
        // 插入原始记忆并获取其Long类型的ID
        val rawMemoryId = rawMemoryDao.insert(newMemory)

        val condensedMemory = CondensedMemory(
            raw_memory_id = rawMemoryId,
            summary_text = "", // 初始摘要为空
            vector_int8 = null, // 初始向量为空
            status = "NEW", // 初始状态为“新”
            timestamp = System.currentTimeMillis()
        )
        // 插入对应的浓缩记忆记录
        condensedMemoryDao.insert(condensedMemory)

        return rawMemoryId
    }

    override suspend fun getMemoryById(id: Long): RawMemory? {
        // 此处ID类型已匹配DAO层，无需转换
        return rawMemoryDao.getById(id)
    }

    override suspend fun updateProcessedMemory(id: Long, summary: String, vector: List<Float>) {
        // 将Float列表转换为ByteArray以便存入数据库
        val byteBuffer = ByteBuffer.allocate(vector.size * 4).order(ByteOrder.nativeOrder())
        for (value in vector) {
            byteBuffer.putFloat(value)
        }
        val byteArray = byteBuffer.array()

        // 此处ID类型已匹配DAO层，无需转换
        condensedMemoryDao.updateProcessedMemory(id, summary, byteArray)
    }

    override suspend fun getAllRawMemories(): List<RawMemory> {
        return rawMemoryDao.getAll()
    }
}
