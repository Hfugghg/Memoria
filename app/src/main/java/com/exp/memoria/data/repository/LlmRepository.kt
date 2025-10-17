package com.exp.memoria.data.repository

import android.util.Log
import com.exp.memoria.data.remote.api.LlmApiService
import com.exp.memoria.data.remote.dto.ChatContent
import com.exp.memoria.data.remote.dto.EmbeddingContent
import com.exp.memoria.data.remote.dto.EmbeddingRequest
import com.exp.memoria.data.remote.dto.LlmRequest
import com.exp.memoria.data.remote.dto.Part
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [LLM 数据仓库]
 *
 * 职责:
 * 1. 封装所有与远程大语言模型 (LLM) API 的交互细节。
 * 2. 对上层（Use Cases）屏蔽网络请求、JSON 序列化/反序列化、API 密钥管理和错误处理的复杂性。
 * 3. 提供简洁、高级的接口，用于调用不同的 LLM 功能，例如获取聊天回复、生成摘要和创建文本向量。
 *
 * 关联:
 * - 注入 `LlmApiService` 来执行实际的网络调用。
 * - 注入 `SettingsRepository` 来动态、安全地获取 API 密钥。
 * - 被 `GetChatResponseUseCase` 和 `ProcessMemoryUseCase` 等业务逻辑层注入和使用。
 */
@Singleton
class LlmRepository @Inject constructor(
    private val llmApiService: LlmApiService,
    private val settingsRepository: SettingsRepository
) {

    /**
     * 从 SettingsRepository 异步获取当前配置的 API 密钥。
     * 这是访问 LLM 服务所必需的。
     */
    private suspend fun getApiKey(): String {
        return settingsRepository.settingsFlow.first().apiKey
    }

    /**
     * 调用 LLM API 以获取对话回复。
     *
     * @param history 一个包含完整对话历史的 `ChatContent` 列表。
     * @return 从 LLM 返回的文本回复。如果请求失败或响应格式不正确，则返回一个默认的错误消息。
     */
    suspend fun getChatResponse(history: List<ChatContent>): String {
        val request = LlmRequest(
            contents = history
        )
        Log.d("LlmRepository", "Chat Request: $request")
        val response = llmApiService.getChatResponse(getApiKey(), request)
        Log.d("LlmRepository", "Chat Response: $response")
        return response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "抱歉，无法获取回复。"
    }

    /**
     * 调用 LLM API 为给定的文本生成摘要。
     *
     * @param text 需要被总结的原始文本。
     * @return 从 LLM 返回的摘要文本。如果请求失败或响应格式不正确，则返回一个默认的错误消息。
     */
    suspend fun getSummary(text: String): String {
        val request = LlmRequest(
            contents = listOf(
                ChatContent(
                    role = "user",
                    parts = listOf(Part(text = "请总结以下文本:$text"))
                )
            )
        )
        Log.d("LlmRepository", "Summary Request: $request")
        val response = llmApiService.getSummary(getApiKey(), request)
        Log.d("LlmRepository", "Summary Response: $response")
        return response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "无法生成摘要。"
    }

    /**
     * 调用 LLM API 为给定的文本生成向量嵌入 (embedding)。
     *
     * @param text 需要被向量化的文本。
     * @return 一个代表文本语义的浮点数列表（即向量）。如果请求失败，此函数可能会抛出异常。
     */
    suspend fun getEmbedding(text: String): List<Float> {
        val request = EmbeddingRequest(
            content = EmbeddingContent(
                parts = listOf(Part(text = text))
            )
        )
        Log.d("LlmRepository", "Embedding Request: $request")
        val response = llmApiService.getEmbedding(getApiKey(), request)
        Log.d("LlmRepository", "Embedding Response: $response")
        return response.embedding.values
    }
}
