package com.exp.memoria.domain.usecase

import android.util.Log
import com.exp.memoria.data.repository.LlmRepository
import com.exp.memoria.data.repository.MemoryRepository
import javax.inject.Inject

/**
 * [处理记忆的业务用例 (Use Case)]
 *
 * 职责:
 * 1. 封装将一条“热记忆”（用户查询和AI回复）转换为可检索的“冷记忆”的后台处理流程 [cite: 17, 28]。
 * 2. 执行流程：
 *    a. 接收一个代表AI回复的原始记忆ID (RawMemory ID)。
 *    b. 查找与之配对的用户查询。
 *    c. 将查询和回复组合成一个对话片段。
 *    d. 调用 LlmRepository 为该对话片段生成摘要(summary_text) [cite: 25]。
 *    e. 调用 LlmRepository (或Embedding服务) 为摘要生成向量(vector) [cite: 25]。
 *    f. (可选优化)对向量进行量化 [cite: 29, 47]。
 *    g. 调用 MemoryRepository 将生成的摘要、向量存储到对应的CondensedMemory记录中，并将状态从"NEW"更新为"INDEXED" [cite: 67]。
 *
 * 关联:
 * - 注入 MemoryRepository 和 LlmRepository。
 * - MemoryProcessingWorker 会在后台任务中调用这个Use Case来处理新生成的记忆。
 */
class ProcessMemoryUseCase @Inject constructor(
    private val memoryRepository: MemoryRepository,
    private val llmRepository: LlmRepository
) {
    /**
     * 执行处理记忆的核心逻辑。
     *
     * 此函数接收一个代表模型回复的记忆ID，从数据库中获取对应的原始记忆
     * 及其关联的用户查询，然后通过调用 LLM API 为其生成摘要和向量嵌入，
     * 最后将这些处理过的数据更新回数据库。
     *
     * @param memoryId 需要被处理的、代表模型回复的原始记忆 (RawMemory) 的ID。
     */
    suspend operator fun invoke(memoryId: Long) {
        // 1. 根据ID获取模型回复的记忆，如果找不到则直接返回
        val modelMemory = memoryRepository.getMemoryById(memoryId)
        if (modelMemory == null || modelMemory.sender != "model") {
            Log.e("ProcessMemoryUseCase", "Memory with ID $memoryId not found or is not a model response.")
            return
        }

        // 2. 假设用户查询的ID是模型回复ID的前一个，并获取该查询
        val userMemory = memoryRepository.getMemoryById(memoryId - 1)

        val dialogue: String
        // 3. 验证用户查询是否存在且有效，然后构造用于生成摘要的对话字符串
        if (userMemory != null && userMemory.sender == "user" && userMemory.conversationId == modelMemory.conversationId) {
            dialogue = "User: ${userMemory.text}\nAI: ${modelMemory.text}"
        } else {
            // 如果找不到匹配的用户查询，记录警告并仅使用模型回复
            Log.w("ProcessMemoryUseCase", "Could not find a matching user query for memory ID $memoryId. Summarizing AI response only.")
            dialogue = "AI: ${modelMemory.text}"
        }

        // 4. 获取当前对话的 responseSchema
        val conversationHeader = memoryRepository.getConversationHeaderById(modelMemory.conversationId)
        val responseSchema = conversationHeader?.responseSchema

        // 5. 为拼接好的对话生成摘要
        val summary = llmRepository.getSummary(dialogue, responseSchema)
        // 6. 为生成的摘要创建向量嵌入
        val embedding = llmRepository.getEmbedding(summary)

        // 7. 将处理好的摘要和向量更新到数据库
        memoryRepository.updateProcessedMemory(memoryId, summary, embedding)
    }
}
