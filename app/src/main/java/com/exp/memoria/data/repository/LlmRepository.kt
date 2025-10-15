package com.exp.memoria.data.repository

import com.exp.memoria.data.remote.api.LlmApiService
import com.exp.memoria.data.remote.dto.ChatContent
import com.exp.memoria.data.remote.dto.EmbeddingContent
import com.exp.memoria.data.remote.dto.EmbeddingRequest
import com.exp.memoria.data.remote.dto.LlmRequest
import com.exp.memoria.data.remote.dto.Part
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [LLM数据仓库]
 *
 * 职责:
 * 1. 封装所有与远程LLM API的交互。
 * 2. 提供一个高级接口，屏蔽网络请求、JSON解析和错误处理的复杂性。
 * 3. 提供方法：
 * - `getChatResponse(context: String)`: 调用主LLM获取对话回复。
 * - `getSummary(text: String)`: 调用浓缩LLM获取摘要 [cite: 14, 25]。
 * - `getEmbedding(text: String)`: 获取文本的向量 [cite: 25]。
 *
 * 关联:
 * - 它会注入 LlmApiService。
 * - GetChatResponseUseCase 和 ProcessMemoryUseCase 会注入并使用这个Repository来与远程AI模型交互。
 *
 * @param llmApiService 由Hilt注入的Retrofit服务接口实例。
 * @param apiKey 用于API请求认证的API密钥，通常通过DI从安全的地方提供。
 */
@Singleton // 建议将Repository设为单例，以保证在应用全局只有一个实例。
class LlmRepository @Inject constructor(
    private val llmApiService: LlmApiService,
    private val apiKey: String // 假设apiKey通过Hilt注入
) {

    /**
     * 根据提供的上下文，调用主LLM获取对话回复。
     *
     * @param context 用户输入或组装好的对话历史记录。
     * @return 返回一个包含AI生成回复的字符串。
     * @throws retrofit2.HttpException 如果API调用返回非2xx的HTTP状态码。
     * @throws java.io.IOException 如果发生网络连接问题。
     */
    suspend fun getChatResponse(context: String): String {
        // 构建符合`generateContent` API规范的请求体
        val request = LlmRequest(
            contents = listOf(
                // 使用`ChatContent`，并明确指定role为"user"
                ChatContent(
                    role = "user",
                    parts = listOf(Part(text = context))
                )
            )
        )
        val response = llmApiService.getChatResponse(apiKey, request)
        // 安全地解析响应，如果为空则返回提示信息
        return response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "抱歉，无法获取回复。"
    }

    /**
     * 调用浓缩LLM对给定文本进行摘要。
     *
     * @param text 需要被摘要的长文本。
     * @return 返回一个包含摘要结果的字符串。
     * @throws retrofit2.HttpException 如果API调用返回非2xx的HTTP状态码。
     */
    suspend fun getSummary(text: String): String {
        // 同样使用`ChatContent`，因为摘要功能也依赖`generateContent`接口
        val request = LlmRequest(
            contents = listOf(
                ChatContent(
                    role = "user",
                    parts = listOf(Part(text = "请总结以下文本:\n$text")) // 可以添加提示词以获得更好的摘要效果
                )
            )
        )
        val response = llmApiService.getSummary(apiKey, request)
        return response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "无法生成摘要。"
    }

    /**
     * 获取给定文本的向量表示 (Embedding)。
     *
     * @param text 需要被向量化的文本。
     * @return 返回一个浮点数列表，代表文本的向量。
     * @throws retrofit2.HttpException 如果API调用返回非2xx的HTTP状态码。
     */
    suspend fun getEmbedding(text: String): List<Float> {
        // 构建符合`embedContent` API规范的请求体
        val request = EmbeddingRequest(
            // 使用`EmbeddingContent`，它不包含`role`字段
            content = EmbeddingContent(
                parts = listOf(Part(text = text))
            )
        )
        val response = llmApiService.getEmbedding(apiKey, request)
        // 如果API没有返回值，则返回空列表
        return response.embedding.values
    }
}