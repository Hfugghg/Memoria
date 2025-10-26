package com.exp.memoria.data.repository.impl

import android.util.Log
import com.exp.memoria.data.remote.api.LlmApiService
import com.exp.memoria.data.remote.api.ModelDetail
import com.exp.memoria.data.remote.dto.*
import com.exp.memoria.data.repository.LlmRepositoryHelpers
import com.exp.memoria.data.repository.SettingsRepository
import com.exp.memoria.data.repository.UtilityService
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 辅助功能实现 (摘要, 嵌入, 模型列表)
 */
@Singleton
class UtilityServiceImpl @Inject constructor(
    private val llmApiService: LlmApiService,
    private val helpers: LlmRepositoryHelpers,
    private val settingsRepository: SettingsRepository
) : UtilityService {

    /**
     * 调用 LLM API 为给定的文本生成摘要。
     *
     * @param text 需要被总结的原始文本。
     * @param responseSchema (可选) 定义期望响应格式的 JSON Schema。
     * @return 从 LLM 返回的摘要文本。如果请求失败或响应格式不正确，则返回一个默认的错误消息。
     */
    override suspend fun getSummary(text: String, responseSchema: String?): String {
        if (settingsRepository.settingsFlow.first().disableSummaryAndEmbedding) {
            Log.d("UtilityServiceImpl", "摘要功能已通过设置禁用。")
            return "摘要功能已禁用。"
        }

        val components = helpers.buildLlmRequestComponents(responseSchema)
        val generationConfig = components.generationConfig
        val safetySettings = components.safetySettings

        val request = LlmRequest(
            contents = listOf(
                ChatContent(
                    role = "user",
                    parts = listOf(Part(text = "请总结以下文本:$text"))
                )
            ),
            generationConfig = generationConfig,
            safetySettings = safetySettings
        )
        val modelId = helpers.getChatModel()
        val apiKey = helpers.getApiKey()

        Log.d("UtilityServiceImpl", "LLM 摘要请求 contents: ${helpers.jsonEncoder.encodeToString(request.contents)}")
        Log.d("UtilityServiceImpl", "LLM 摘要请求体: ${helpers.jsonEncoder.encodeToString(request)}")

        val requestUrl = "${helpers.baseLlmApiUrl}v1beta/models/$modelId:generateContent?key=$apiKey"
        Log.d("UtilityServiceImpl", "LLM 摘要请求 URL: $requestUrl")

        return try {
            val response = llmApiService.getSummary(modelId, apiKey, request)
            Log.d("UtilityServiceImpl", "LLM 摘要响应: $response")
            val summaryText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "无法生成摘要。"
            Log.d("UtilityServiceImpl", "解析后的摘要文本: $summaryText") // 新增的日志
            summaryText
        } catch (e: Exception) {
            Log.e("UtilityServiceImpl", "获取 LLM 摘要失败", e)
            "抱歉，无法生成摘要。错误：${e.localizedMessage}"
        }
    }

    /**
     * 调用 LLM API 为给定的文本生成向量嵌入 (embedding)。
     *
     * @param text 需要被向量化的文本。
     * @return 一个代表文本语义的浮点数列表（即向量）。如果请求失败，此函数可能会抛出异常。
     */
    override suspend fun getEmbedding(text: String): List<Float> {
        if (settingsRepository.settingsFlow.first().disableSummaryAndEmbedding) {
            Log.d("UtilityServiceImpl", "嵌入功能已通过设置禁用。")
            return emptyList()
        }

        val request = EmbeddingRequest(
            content = EmbeddingContent(
                parts = listOf(Part(text = text))
            )
        )
        val modelId = helpers.getEmbeddingModel() // 使用辅助函数获取模型ID
        val apiKey = helpers.getApiKey()
        val requestUrl = "${helpers.baseLlmApiUrl}v1beta/models/$modelId:embedContent?key=$apiKey"
        Log.d("UtilityServiceImpl", "LLM 嵌入请求 URL: $requestUrl")
        Log.d("UtilityServiceImpl", "LLM 嵌入请求体: $request")

        return try {
            val response = llmApiService.getEmbedding(modelId, apiKey, request) // 传入模型ID
            Log.d("UtilityServiceImpl", "LLM 嵌入响应: $response")
            response.embedding.values
        } catch (e: Exception) {
            Log.e("UtilityServiceImpl", "获取 LLM 嵌入失败", e)
            emptyList()
        }
    }

    /**
     * 调用 LLM API 获取所有可用的模型列表，支持分页。
     *
     * @param apiKey 用于认证的 API 密钥。
     * @param pageToken 如果存在，则用于获取下一页模型列表的令牌。
     * @return Pair<List<ModelDetail>, String?> 包含模型列表和下一页的令牌。
     */
    override suspend fun getAvailableModels(apiKey: String, pageToken: String?): Pair<List<ModelDetail>, String?> {
        val requestUrl =
            "${helpers.baseLlmApiUrl}v1beta/models?key=$apiKey" + (pageToken?.let { "&pageToken=$it" } ?: "")
        Log.d("UtilityServiceImpl", "获取可用模型请求 URL: $requestUrl")
        Log.d("UtilityServiceImpl", "获取可用模型请求 pageToken: $pageToken")
        val response = llmApiService.getAvailableModels(apiKey, pageToken)
        Log.d(
            "UtilityServiceImpl",
            "获取可用模型响应, 模型数量: ${response.models.size}, 下一页token: ${response.nextPageToken}"
        )
        return Pair(response.models, response.nextPageToken)
    }
}
