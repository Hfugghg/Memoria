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
 *【LLM数据仓库]
 *
 * 职责:
 *1，封装所有与远程LLMAPI的交互。
 *2．提供一个高级接口，屏蔽网络请求、JSON解析和错误处理的复杂性。
 *3.提供方法:
 *-｀getChatResponse(context：String)｀：调用主LLM获取对话回复。
 *getSummary(text:String)’：调用浓缩LLM获取摘要[cite:14，25]。
 *getEmbedding(text:String)’：获取文本的向量[cite:25]。
 *
 * 关联:
 *-它会注入LlmApiService。
 *-GetChatResponseUseCase和ProcessMemoryUseCase会注入并使用这个Repository来与远程AI模型交互。
 *
 *@paramllmApiService由Hilt注入的Retrofit服务接口实例。
 *@paramapiKey用于API请求认证的API密钥，通常通过DI从安全的地方提供。
 */

@Singleton
class LlmRepository @Inject constructor(
    private val llmApiService: LlmApiService,
    private val settingsRepository: SettingsRepository
) {

    private suspend fun getApiKey(): String {
        return settingsRepository.settingsFlow.first().apiKey
    }

    suspend fun getChatResponse(context: String): String {
        val request = LlmRequest(
            contents = listOf(
                ChatContent(
                    role = "user",
                    parts = listOf(Part(text = context))
                )
            )
        )
        Log.d("LlmRepository", "Chat Request: $request")
        val response = llmApiService.getChatResponse(getApiKey(), request)
        Log.d("LlmRepository", "Chat Response: $response")
        return response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "抱歉，无法获取回复。"
    }

    suspend fun getSummary(text: String): String {
        val request = LlmRequest(
            contents = listOf(
                ChatContent(
                    role = "user",
                    parts = listOf(Part(text = "请总结以下文本:\n$text"))
                )
            )
        )
        Log.d("LlmRepository", "Summary Request: $request")
        val response = llmApiService.getSummary(getApiKey(), request)
        Log.d("LlmRepository", "Summary Response: $response")
        return response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "无法生成摘要。"
    }

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