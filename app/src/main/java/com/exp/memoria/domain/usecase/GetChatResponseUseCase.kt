package com.exp.memoria.domain.usecase

import com.exp.memoria.data.repository.LlmRepository
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
 *
 * 实现指导 (在 execute/invoke 方法中):
 * 1. 接收用户当前的问题(Query)作为参数。
 * 2. **获取热记忆**: 调用 MemoryRepository 获取全部“热记忆” [cite: 32]。
 * 3. **检索冷记忆**: 调用 MemoryRepository.findRelevantColdMemories(query) 执行“FTS预过滤 + 向量精排”二级检索，找出最相关的历史记忆 [cite: 33]。
 * 4. **动态组装上下文**: 将“热记忆”、“检索出的冷记忆”和用户当前问题智能地组合成一个丰富的上下文Prompt [cite: 35]。
 * 5. **生成回答**: 调用 LlmRepository 将组合好的上下文发送给主LLM，并获取最终回答 [cite: 35]。
 * 6. 返回生成的回答给ViewModel。
 */

class GetChatResponseUseCase @Inject constructor(
    private val memoryRepository: MemoryRepository, // 注入记忆仓库
    private val llmRepository: LlmRepository // 注入LLM仓库
) {
    suspend operator fun invoke(query: String): String {
        //  这里我将暂时直接调用LLM，而不实现完整的RAG流程
        //  在未来的开发中，这里将实现完整的“热记忆”+“冷记忆”+“查询”的上下文组装逻辑
        return llmRepository.getChatResponse(query)
    }
}