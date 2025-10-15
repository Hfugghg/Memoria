package com.exp.memoria.domain.usecase

import com.exp.memoria.data.repository.LlmRepository
import com.exp.memoria.data.repository.MemoryRepository
import javax.inject.Inject

/**
 * [处理记忆的业务用例 (Use Case)]
 *
 * 职责:
 * 1. 封装将一条“热记忆”转换为可检索的“冷记忆”的完整后台处理流程 [cite: 17, 28]。
 * 2. 执行流程：
 * a. 接收一条待处理的原始记忆(RawMemory)或其ID。
 * b. 调用 LlmRepository 为其生成摘要(summary_text) [cite: 25]。
 * c. 调用 LlmRepository (或Embedding服务) 为其生成高精度向量(vector_float32) [cite: 25]。
 * d. (可选优化)对向量进行量化，生成vector_int8 [cite: 29, 47]。
 * e. 调用 MemoryRepository 将生成的摘要、向量存储到对应的CondensedMemory记录中，并将状态从"NEW"更新为"INDEXED" [cite: 67]。
 *
 * 关联:
 * - 注入 MemoryRepository 和 LlmRepository。
 * - MemoryProcessingWorker 会在后台任务中调用这个Use Case来处理积压的记忆。
 *
 * 实现指导 (在 execute/invoke 方法中):
 * 1. 接收一条待处理的浓缩记忆对象(CondensedMemory)作为参数。
 * 2. **生成摘要**: 将原始对话内容发送给 LlmRepository.getSummary()，获取浓缩后的摘要文本 [cite: 25]。
 * 3. **生成向量**: 将摘要文本发送给 LlmRepository.getEmbedding()，获取高精度浮点向量 [cite: 25]。
 * 4. **向量量化**: 将 float32 向量转换为 int8 向量以节省空间并加速计算 [cite: 29, 47]。
 * 5. **更新数据库**: 调用 MemoryRepository.updateProcessedMemory(...)，将生成的摘要、量化向量存入数据库，并更新该记忆的状态为 "INDEXED" [cite: 67]。
 */
class ProcessMemoryUseCase @Inject constructor(
    private val memoryRepository: MemoryRepository,
    private val llmRepository: LlmRepository
) {
    // 使用Long类型的ID来调用此用例
    suspend operator fun invoke(memoryId: Long) {
        // 根据ID获取原始记忆，如果找不到则直接返回
        val memory = memoryRepository.getMemoryById(memoryId) ?: return

        // 将用户问题和LLM的回答拼接起来，为其生成摘要
        val summary = llmRepository.getSummary(memory.user_query + "\n" + memory.llm_response)
        // 为生成的摘要创建向量嵌入
        val embedding = llmRepository.getEmbedding(summary)

        // 将处理好的摘要和向量更新到数据库
        memoryRepository.updateProcessedMemory(memoryId, summary, embedding)
    }
}
