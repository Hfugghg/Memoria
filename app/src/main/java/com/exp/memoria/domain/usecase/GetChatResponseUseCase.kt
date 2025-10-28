package com.exp.memoria.domain.usecase

import android.util.Log
import com.exp.memoria.data.model.FileAttachment
import com.exp.memoria.data.remote.dto.ChatContent
import com.exp.memoria.data.remote.dto.InlineData
import com.exp.memoria.data.remote.dto.Part
import com.exp.memoria.data.repository.ChatChunkResult
import com.exp.memoria.data.repository.LlmRepository
import com.exp.memoria.data.repository.LlmRepositoryHelpers // 导入 LlmRepositoryHelpers
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
    private val llmRepository: LlmRepository, // 注入LLM仓库
    private val llmRepositoryHelpers: LlmRepositoryHelpers // 注入 LlmRepositoryHelpers
) {
    suspend operator fun invoke(
        query: String?, // 将 query 参数改为可空
        conversationId: String,
        isStreaming: Boolean = false,
        attachments: List<FileAttachment> = emptyList()
    ): Flow<ChatChunkResult> {
        // 在未来的开发中，这里将实现完整的“热记忆”+“冷记忆”+“查询”的上下文组装逻辑

        // 1. 获取当前对话的所有原始记忆
        val allMemories = memoryRepository.getAllRawMemoriesForConversation(conversationId)
        Log.d(
            "GetChatResponseUseCase",
            "[诊断] UseCase: 为 conversationId: $conversationId 获取了 ${allMemories.size} 条记忆用于构建上下文。"
        )

        // 2. 将原始记忆转换为符合API数据结构的 ChatContent 对象列表
        val history = allMemories.map { memory -> // RawMemory 实体有 id 属性
            // 为每条记忆构建其 parts 列表，先添加文本部分
            val parts = mutableListOf(Part(text = memory.text))

            // 根据记忆ID获取其关联的所有附件文件
            val memoryAttachments = memoryRepository.getMessageFilesForMemory(memory.id)

            // 将附件转换为 Part 并添加到列表中
            memoryAttachments.forEach { attachment ->
                parts.add(Part(inlineData = InlineData(mimeType = attachment.fileType, data = attachment.fileContentBase64)))
            }

            // 使用包含文本和所有附件的 parts 列表创建 ChatContent
            ChatContent(
                role = memory.sender,
                parts = parts
            )
        }.toMutableList()

        // 核心修复：仅当 query 不为 null 时，才添加新的用户消息。
        // 这可以防止在“重说”场景下（此时调用者应传递 null）重复添加用户消息。
        if (query != null) {
            val currentUserParts = mutableListOf<Part>()
            if (query.isNotBlank()) {
                currentUserParts.add(Part(text = query))
            }
            attachments.forEach { attachment ->
                currentUserParts.add(
                    Part(inlineData = InlineData(mimeType = attachment.fileType ?: "application/octet-stream", data = attachment.base64Data))
                )
            }

            if (currentUserParts.isNotEmpty()) {
                history.add(ChatContent(role = "user", parts = currentUserParts))
                Log.d("GetChatResponseUseCase", "[诊断] 已将包含 ${currentUserParts.size} 个部分的新用户消息添加到历史记录中。")
            }
        } else {
            Log.d("GetChatResponseUseCase", "[诊断] query 为 null，跳过添加新用户消息。这是“重说”的预期行为。")
        }

        // 4. 获取对话的 header，从中提取 systemInstruction 和 responseSchema
        val conversationHeader = memoryRepository.getConversationHeaderById(conversationId)
        val systemInstruction = conversationHeader?.systemInstruction
        val responseSchema = conversationHeader?.responseSchema

        // 获取当前模型的 inputTokenLimit
        val inputTokenLimit = llmRepositoryHelpers.getCurrentChatModelInputTokenLimit()
        Log.d("GetChatResponseUseCase", "[诊断] 当前模型的 inputTokenLimit: $inputTokenLimit")

        // 使用 conversationHeader 中存储的 totalTokenCount 作为当前上下文的近似 token 长度
        val currentContextTotalTokenCount = conversationHeader?.totalTokenCount ?: 0

        // 新的上下文组装逻辑：当上下文长度超过 inputTokenLimit 的 90% 或已标记需要压缩时启用
        val shouldCompactContext = conversationHeader?.contextCompactionRequired == true ||
                                   (inputTokenLimit != null && currentContextTotalTokenCount > (inputTokenLimit * 0.9).toInt())

        if (shouldCompactContext) {
            Log.d("GetChatResponseUseCase", "[诊断] 上下文总 token 数 (${currentContextTotalTokenCount}) 超过 inputTokenLimit 的 90% (${(inputTokenLimit?.times(0.9))?.toInt() ?: "N/A"}) 或已标记需要压缩，启用新的上下文组装逻辑。")

            // 如果当前对话尚未标记为需要上下文压缩，则更新标志
            if (conversationHeader?.contextCompactionRequired == false) {
                memoryRepository.updateContextCompactionRequired(conversationId, true)
                Log.d("GetChatResponseUseCase", "[诊断] 对话 $conversationId 已标记为需要上下文压缩。")
            }

            val tokenThresholdOneThirdId = conversationHeader?.tokenThresholdOneThirdId
            val tokenThresholdTwoThirdsId = conversationHeader?.tokenThresholdTwoThirdsId

            if (tokenThresholdOneThirdId != null && tokenThresholdTwoThirdsId != null) {
                // 提取第一部分：从第一条消息到 tokenThresholdOneThirdId
                val firstPart = allMemories.takeWhile { it.id <= tokenThresholdOneThirdId }

                // 提取第三部分：从 tokenThresholdTwoThirdsId 到最新消息
                val thirdPart = allMemories.dropWhile { it.id < tokenThresholdTwoThirdsId }

                Log.d("GetChatResponseUseCase", "[上下文压缩] 第一部分消息 ID 范围: ${firstPart.firstOrNull()?.id} - ${firstPart.lastOrNull()?.id}")
                Log.d("GetChatResponseUseCase", "[上下文压缩] 第三部分消息 ID 范围: ${thirdPart.firstOrNull()?.id} - ${thirdPart.lastOrNull()?.id}")

                // TODO: 中间部分的摘要逻辑
            }
        }

        // 5. 调用LLM仓库，这将正确返回 Flow<ChatChunkResult>，并传入 systemInstruction 和 responseSchema
        return llmRepository.chatResponse(history, systemInstruction, responseSchema, isStreaming)
    }
}