package com.exp.memoria.data.repository

import com.exp.memoria.data.Content
import com.exp.memoria.data.Part
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
 *    - `saveNewMemory(...)`: 保存一个新的问答对。
 *    - `getMemoryById(...)`: 获取指定的记忆。
 *    - `updateProcessedMemory(...)`: 更新一个已处理的记忆。
 *    - `getAllRawMemories()`: 获取所有原始记忆。
 *    - `getRawMemories(...)`: 获取分页的原始记忆。
 *
 * 关联:
 * - 它会注入 RawMemoryDao 和 CondensedMemoryDao。
 * - GetChatResponseUseCase 会注入并使用这个Repository来管理记忆数据。
 */
interface MemoryRepository {
    /** 保存一条新的记忆，并返回其在数据库中的ID。 */
    suspend fun saveNewMemory(query: String, response: String): Long
    /** 根据ID获取一条原始记忆。 */
    suspend fun getMemoryById(id: Long): RawMemory?
    /** 根据ID更新一条已处理的记忆。 */
    suspend fun updateProcessedMemory(id: Long, summary: String, vector: List<Float>)
    /** 获取所有原始记忆。 */
    suspend fun getAllRawMemories(): List<RawMemory>
    /** 获取分页的原始记忆。 */
    suspend fun getRawMemories(limit: Int, offset: Int): List<RawMemory>
}

class MemoryRepositoryImpl @Inject constructor(
    private val rawMemoryDao: RawMemoryDao,
    private val condensedMemoryDao: CondensedMemoryDao
) : MemoryRepository {

    override suspend fun saveNewMemory(query: String, response: String): Long {
        // 创建结构化的内容列表
        val newContents = listOf(
            Content.User(parts = listOf(Part.Text(query))),
            Content.Model(parts = listOf(Part.Text(response)))
        )

        // 使用新结构创建RawMemory实体
        val newMemory = RawMemory(
            contents = newContents,
            timestamp = System.currentTimeMillis()
        )
        // 插入原始记忆并获取其ID
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
        return rawMemoryDao.getById(id)
    }

    override suspend fun updateProcessedMemory(id: Long, summary: String, vector: List<Float>) {
        // 将Float列表转换为ByteArray以便存入数据库
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

    override suspend fun getRawMemories(limit: Int, offset: Int): List<RawMemory> {
        return rawMemoryDao.getWithLimitOffset(limit, offset)
    }
}
