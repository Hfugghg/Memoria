package com.exp.memoria.data.repository

import com.exp.memoria.data.remote.api.ModelDetail
import com.exp.memoria.data.remote.dto.ChatContent
import com.exp.memoria.data.repository.llmrepository.ChatService
import com.exp.memoria.data.repository.llmrepository.UtilityService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 封装聊天响应的密封类，用于区分成功的数据块和错误信息。
 * 这允许上游 (ViewModel) 正确处理 UI 显示和数据库存储。
 */
sealed class ChatChunkResult {
    /**
     * 成功的文本块
     * @param text 收到的文本片段。
     * @param totalTokenCount (可选) 截止到当前数据块，本次请求已消耗的总令牌数。
     */
    data class Success(val text: String, val totalTokenCount: Int? = null) : ChatChunkResult()

    /** 发生的错误 */
    data class Error(val message: String) : ChatChunkResult()
}

/**
 * [LLM 数据仓库]
 *
 * 职责:
 * 1. 封装所有与远程大语言模型 (LLM) API 的交互细节。
 * 2. 对上层（Use Cases）屏蔽网络请求、JSON 序列化/反序列化、API 密钥管理和错误处理的复杂性。
 * 3. 提供简洁、高级的接口，用于调用不同的 LLM 功能，例如获取聊天回复、生成摘要和创建文本向量。
 * 4. 提供获取可用 LLM 模型列表的功能，支持分页。
 *
 * 【重构说明】: 此文件现在作为主要的公共接口和服务的委托者。
 * 具体实现已拆分到同目录下 `llmrepository` 文件夹中的 `ChatServiceImpl.kt` 和 `UtilityServiceImpl.kt`。
 */
@Singleton
class LlmRepository @Inject constructor(
    private val chatService: ChatService,
    private val utilityService: UtilityService
) {
    /**
     * 调用 LLM API 以获取对话回复，支持流式和非流式输出。
     *
     * @param history 一个包含完整对话历史的 `ChatContent` 列表。
     * @param systemInstruction (可选) 指导模型行为的系统指令。
     * @param responseSchema (可选) 定义期望响应格式的 JSON Schema。
     * @param isStreaming 是否开启流式输出。默认为 `false` (非流式)。
     * @return 一个 `Flow<ChatChunkResult>`，用于接收来自 LLM 的文本回复片段或错误。
     * 如果 `isStreaming` 为 `false`，则 `Flow` 只会发出一个完整的 `Success` 或 `Error`。
     */
    fun chatResponse(
        history: List<ChatContent>,
        systemInstruction: String?,
        responseSchema: String?,
        isStreaming: Boolean = false
    ): Flow<ChatChunkResult> {
        return chatService.chatResponse(history, systemInstruction, responseSchema, isStreaming)
    }

    /**
     * 调用 LLM API 为给定的文本生成摘要。
     *
     * @param text 需要被总结的原始文本。
     * @param responseSchema (可选) 定义期望响应格式的 JSON Schema。
     * @return 从 LLM 返回的摘要文本。如果请求失败或响应格式不正确，则返回一个默认的错误消息。
     */
    suspend fun getSummary(text: String, responseSchema: String?): String {
        return utilityService.getSummary(text, responseSchema)
    }

    /**
     * 调用 LLM API 为给定的文本生成向量嵌入 (embedding)。
     *
     * @param text 需要被向量化的文本。
     * @return 一个代表文本语义的浮点数列表（即向量）。如果请求失败，此函数可能会抛出异常。
     */
    suspend fun getEmbedding(text: String): List<Float> {
        return utilityService.getEmbedding(text)
    }

    /**
     * 调用 LLM API 获取所有可用的模型列表，支持分页。
     *
     * @param apiKey 用于认证的 API 密钥。
     * @param pageToken 如果存在，则用于获取下一页模型列表的令牌。
     * @return Pair<List<ModelDetail>, String?> 包含模型列表和下一页的令牌。
     */
    suspend fun getAvailableModels(apiKey: String, pageToken: String?): Pair<List<ModelDetail>, String?> {
        return utilityService.getAvailableModels(apiKey, pageToken)
    }
}
