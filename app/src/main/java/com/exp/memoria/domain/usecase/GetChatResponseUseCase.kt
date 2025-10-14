package com.exp.memoria.domain.usecase

import com.exp.memoria.data.repository.MemoryRepository
import javax.inject.Inject

/**
 * [获取聊天回复的业务用例 (Use Case)]
 *
 * 职责:
 * 1. 封装完整的检索增强生成（RAG）核心业务流程 [cite: 18]。这是应用最核心的逻辑单元。
 * 2. 执行流程：
 * a. 接收用户当前的问题(Query)。
 * b. 调用 MemoryRepository 获取全部“热记忆” [cite: 32]。
 * c. 调用 MemoryRepository 根据用户问题从“冷记忆”库中检索最相关的内容 [cite: 33]。
 * d. 将“热记忆”、“检索出的冷记忆”和用户当前问题智能地组合成一个丰富的上下文(Context) [cite: 35]。
 * e. 调用 LlmRepository 将组合好的上下文发送给主LLM。
 * f. 返回最终生成的回答。
 * 3. 确保整个流程在后台线程执行，避免阻塞UI [cite: 41]。
 *
 * 关联:
 * - 注入 MemoryRepository 和 LlmRepository。
 * - ChatViewModel 会调用这个Use Case来获取AI的回复。
 */

class GetChatResponseUseCase @Inject constructor(
    private val repository: MemoryRepository
) {
    suspend operator fun invoke(query: String): String {
        return repository.getChatResponse(query)
    }
}