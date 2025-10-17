package com.exp.memoria.domain.usecase

import com.exp.memoria.data.remote.dto.ChatContent
import com.exp.memoria.data.remote.dto.Part
import com.exp.memoria.data.repository.LlmRepository
import com.exp.memoria.data.repository.MemoryRepository
import javax.inject.Inject

/**
 * [获取聊天回复的业务用例 (Use Case)]
 *
 * 职责:
 * 1. 封装完整的检索增强生成（RAG）核心业务流程 [cite: 18]。这是应用最核心的逻辑单元。
 * 2. 执行流程：
 *    a. 接收用户当前的问题(Query)。
 *    b. 调用 MemoryRepository 获取当前对话的“热记忆” [cite: 32]。
 *    c. 调用 MemoryRepository 根据用户问题从“冷记忆”库中检索最相关的内容 [cite: 33]。
 *    d. 将“热记忆”、“检索出的冷记忆”和用户当前问题，构建成一个符合API要求的、包含完整对话历史的`ChatContent`对象列表 [cite: 35]。
 *    e. 调用 LlmRepository 将组合好的上下文发送给主LLM。
 *    f. 返回最终生成的回答。
 * 3. 确保整个流程在后台线程执行，避免阻塞UI [cite: 41]。
 *
 * 关联:
 * - 注入 MemoryRepository 和 LlmRepository。
 * - ChatViewModel 会调用这个Use Case来获取AI的回复。
 *
 * 实现指导 (在 execute/invoke 方法中):
 * 1. 接收用户当前的问题(Query)和 conversationId 作为参数。
 * 2. **获取热记忆**: 调用 MemoryRepository 获取当前 `conversationId` 的“热记忆” [cite: 32]。
 * 3. **检索冷记忆**: 调用 MemoryRepository.findRelevantColdMemories(query) 执行“FTS预过滤 + 向量精排”二级检索，找出最相关的历史记忆 [cite: 33]。
 * 4. **构建对话历史**: 将从记忆库中获取的对话记录，结合用户当前问题，转换并组装成一个包含多轮对话的`ChatContent`对象列表，作为发送给LLM的上下文 [cite: 35]。
 * 5. **生成回答**: 调用 LlmRepository 将组合好的上下文发送给主LLM，并获取最终回答 [cite: 35]。
 * 6. 返回生成的回答给ViewModel。
 */
class GetChatResponseUseCase @Inject constructor(
    private val memoryRepository: MemoryRepository, // 注入记忆仓库
    private val llmRepository: LlmRepository // 注入LLM仓库
) {
    suspend operator fun invoke(query: String, conversationId: String): String {
        // 在未来的开发中，这里将实现完整的“热记忆”+“冷记忆”+“查询”的上下文组装逻辑

        // 1. 获取当前对话的所有原始记忆，确保使用 conversationId 进行过滤
        val allMemories = memoryRepository.getAllRawMemoriesForConversation(conversationId) // 修改这里

        // 2. 将原始记忆转换为符合API数据结构的 ChatContent 对象列表
        //    - flatMap 将多层列表（每个Memory里有一个contents列表）拍平为单层列表
        //    - mapNotNull 遍历每个 content，转换成 ChatContent，同时过滤掉不支持的类型（返回null的项）
        val history = allMemories.flatMap { memory ->
            memory.contents.mapNotNull { content ->
                when (content) {
                    // 处理用户消息
                    is com.exp.memoria.data.Content.User ->
                        ChatContent(role = "user", parts = content.parts.map { Part(text = (it as? com.exp.memoria.data.Part.Text)?.text ?: "") })
                    // 处理AI模型消息
                    is com.exp.memoria.data.Content.Model ->
                        ChatContent(role = "model", parts = content.parts.map { Part(text = (it as? com.exp.memoria.data.Part.Text)?.text ?: "") })
                    // 忽略其他不支持的 content 类型
                    else -> null
                }
            }
        }.toMutableList() // 转换为可变列表，以便添加当前查询

        // 3. 将用户当前的查询作为最新的一条“user”消息，添加到历史记录末尾
        history.add(ChatContent(role = "user", parts = listOf(Part(text = query))))

        // 4. 调用LLM仓库，传入构建好的完整对话历史以获取聊天响应
        return llmRepository.getChatResponse(history)
    }
}
