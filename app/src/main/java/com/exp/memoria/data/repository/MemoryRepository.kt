package com.exp.memoria.data.repository

import com.exp.memoria.data.local.dao.RawMemoryDao
import com.exp.memoria.data.local.entity.RawMemory
import kotlinx.coroutines.delay
import javax.inject.Inject

// Interface for testability
interface MemoryRepository {
    suspend fun getChatResponse(query: String): String
}

// Implementation
class MemoryRepositoryImpl @Inject constructor(
    private val rawMemoryDao: RawMemoryDao
) : MemoryRepository {

    override suspend fun getChatResponse(query: String): String {
        // Simulate network delay
        delay(1500)

        // For now, return a hardcoded response.
        // In the real app, this will involve RAG [cite: 18] and LLM calls.
        val newMemory = RawMemory(
            user_query = query,
            llm_response = "你好，我是Memoria。这是来自模拟仓库的回答。",
            timestamp = System.currentTimeMillis()
        )
        // Simulate saving to DB
        // rawMemoryDao.insert(newMemory)

        return newMemory.llm_response
    }
}