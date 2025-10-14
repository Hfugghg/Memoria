package com.exp.memoria.data.repository

import com.exp.memoria.data.local.dao.RawMemoryDao
import com.exp.memoria.data.local.entity.RawMemory
import kotlinx.coroutines.delay
import javax.inject.Inject

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