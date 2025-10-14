package com.exp.memoria.data.repository

import com.exp.memoria.data.local.dao.RawMemoryDao
import com.exp.memoria.data.local.entity.RawMemory
import kotlinx.coroutines.delay
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

// 用于可测试性的接口
interface MemoryRepository {
    suspend fun getChatResponse(query: String): String
}

// 实现
class MemoryRepositoryImpl @Inject constructor(
    private val rawMemoryDao: RawMemoryDao
) : MemoryRepository {

    override suspend fun getChatResponse(query: String): String {
        // 模拟网络延迟
        delay(1500)

        // 目前，返回一个硬编码的响应。
        // 在实际的应用中，这将涉及RAG和LLM调用。
        val newMemory = RawMemory(
            user_query = query,
            llm_response = "你好，我是Memoria。这是来自模拟仓库的回答。",
            timestamp = System.currentTimeMillis()
        )
        // 模拟保存到数据库
        // rawMemoryDao.insert(newMemory)

        return newMemory.llm_response
    }
}