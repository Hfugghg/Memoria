package com.exp.memoria.domain.usecase

import android.util.Log
import com.exp.memoria.data.remote.dto.ChatContent
import com.exp.memoria.data.remote.dto.Part
import com.exp.memoria.data.repository.ChatChunkResult
import com.exp.memoria.data.repository.LlmRepository
import com.exp.memoria.data.repository.MemoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * [获取聊天回复的业务用例 (Use Case)]
 *
 * 职责:
 * 1. 封装获取 LLM 聊天回复的核心业务逻辑。
 * 2. 构建发送给 LLM 的上下文，该上下文包含当前对话的完整历史记录。
 * 3. 调用 LLM 服务并以流式或非流式方式返回响应。
 * 4. 未来将扩展为完整的检索增强生成（RAG）流程，结合“热记忆”和“冷记忆” [cite: 18]。
 *
 * 当前执行流程：
 *    a. 接收用户当前的问题(Query)和对话ID(conversationId)。
 *    b. 调用 MemoryRepository 获取该对话的全部历史消息。
 *    c. 将历史消息和当前问题组装成一个 `ChatContent` 对象列表。
 *    d. 调用 LlmRepository 将组装好的上下文发送给 LLM。
 *    e. 返回 LLM 生成的回答。
 *
 * 关联:
 * - 注入 MemoryRepository 和 LlmRepository。
 * - ChatViewModel 会调用这个Use Case来获取AI的回复。
 */
class GetChatResponseUseCase @Inject constructor(
    private val memoryRepository: MemoryRepository, // 注入记忆仓库
    private val llmRepository: LlmRepository // 注入LLM仓库
) {
    suspend operator fun invoke(
        query: String,
        conversationId: String,
        isStreaming: Boolean = false
    ): Flow<ChatChunkResult> {
        // 在未来的开发中，这里将实现完整的“热记忆”+“冷记忆”+“查询”的上下文组装逻辑

        // 1. 获取当前对话的所有原始记忆
        val allMemories = memoryRepository.getAllRawMemoriesForConversation(conversationId)
        Log.d("GetChatResponseUseCase", "[诊断] UseCase: 为 conversationId: $conversationId 获取了 ${allMemories.size} 条记忆用于构建上下文。")

        // 2. 将原始记忆转换为符合API数据结构的 ChatContent 对象列表
        val history = allMemories.map { memory ->
            ChatContent(
                role = memory.sender,
                parts = listOf(Part(text = memory.text))
            )
        }.toMutableList()

        // 3. 将用户当前的查询作为最新的一条“user”消息，添加到历史记录末尾
        history.add(ChatContent(role = "user", parts = listOf(Part(text = query))))

        // 4. 获取对话的 header，从中提取 systemInstruction 和 responseSchema
        val conversationHeader = memoryRepository.getConversationHeaderById(conversationId)
        val systemInstruction = conversationHeader?.systemInstruction
        val responseSchema = conversationHeader?.responseSchema

        // 5. 调用LLM仓库，这将正确返回 Flow<ChatChunkResult>，并传入 systemInstruction 和 responseSchema
        return llmRepository.chatResponse(history, systemInstruction, responseSchema, isStreaming)
    }
}
