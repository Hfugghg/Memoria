package com.exp.memoria.data.repository

import android.util.Log
import com.exp.memoria.data.remote.api.LlmApiService
import com.exp.memoria.data.remote.api.ModelDetail
import com.exp.memoria.data.remote.dto.ChatContent
import com.exp.memoria.data.remote.dto.EmbeddingContent
import com.exp.memoria.data.remote.dto.EmbeddingRequest
import com.exp.memoria.data.remote.dto.LlmRequest
import com.exp.memoria.data.remote.dto.Part
import com.exp.memoria.data.remote.dto.GenerationConfig
import com.exp.memoria.data.remote.dto.SafetySetting
import com.exp.memoria.ui.settings.JsonSchemaProperty
import com.exp.memoria.ui.settings.JsonSchemaPropertyType
import com.exp.memoria.ui.settings.StringFormat
import com.exp.memoria.ui.settings.HarmBlockThreshold
import kotlinx.coroutines.flow.Flow 
import kotlinx.coroutines.flow.flow 
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.decodeFromString 
import com.exp.memoria.data.remote.dto.LlmResponse 
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [LLM 数据仓库]
 *
 * 职责:
 * 1. 封装所有与远程大语言模型 (LLM) API 的交互细节。
 * 2. 对上层（Use Cases）屏蔽网络请求、JSON 序列化/反序列化、API 密钥管理和错误处理的复杂性。
 * 3. 提供简洁、高级的接口，用于调用不同的 LLM 功能，例如获取聊天回复、生成摘要和创建文本向量。
 * 4. 提供获取可用 LLM 模型列表的功能，支持分页。
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

    private val baseLlmApiUrl = "https://generativelanguage.googleapis.com/"

    // 配置 Json 序列化器，使其编码所有默认值（包括 null）
    private val jsonEncoder = Json {
        // 启用忽略未知键，这是解决 'usageMetadata' 报错的关键！
        ignoreUnknownKeys = true
        // 保持格式化，方便调试
        prettyPrint = true
        // 允许编码/解码非结构化的 JSON 原始值（例如，纯字符串）
        allowStructuredMapKeys = false
    }

    // 定义标准安全类别
    private companion object {
        const val HARM_CATEGORY_HARASSMENT = "HARM_CATEGORY_HARASSMENT"
        const val HARM_CATEGORY_HATE_SPEECH = "HARM_CATEGORY_HATE_SPEECH"
        const val HARM_CATEGORY_SEXUALLY_EXPLICIT = "HARM_CATEGORY_SEXUALLY_EXPLICIT"
        const val HARM_CATEGORY_DANGEROUS_CONTENT = "HARM_CATEGORY_DANGEROUS_CONTENT"
    }

    /**
     * 从 SettingsRepository 异步获取当前配置的 API 密钥。
     * 这是访问 LLM 服务所必需的。
     */
    private suspend fun getApiKey(): String {
        return settingsRepository.settingsFlow.first().apiKey
    }

    /**
     * 从 SettingsRepository 异步获取当前配置的聊天模型名称。
     * 这是访问 LLM 服务所必需的。
     */
    private suspend fun getChatModel(): String {
        val model = settingsRepository.settingsFlow.first().chatModel
        // 修复：无论如何，都移除 "models/" 前缀
        return model.removePrefix("models/")
    }

    /**
     * 将图形化编辑的属性列表转换为 JSON Schema 对象。
     * @param properties 待转换的属性列表。
     * @return 对应的 JSON Schema 对象。
     */
    private fun convertGraphicalSchemaToJson(properties: List<JsonSchemaProperty>): JsonElement {
        if (properties.isEmpty()) {
            return JsonNull
        }

        val propertiesMap = buildJsonObject {
            properties.forEach { prop ->
                putJsonObject(prop.name) {
                    put("type", prop.type.name.lowercase())
                    if (prop.description.isNotBlank()) {
                        put("description", prop.description)
                    }
                    when (prop.type) {
                        JsonSchemaPropertyType.STRING -> {
                            if (prop.stringFormat != StringFormat.NONE) {
                                put("format", prop.stringFormat.name.lowercase())
                            }
                        }
                        JsonSchemaPropertyType.NUMBER -> {
                            prop.numberMinimum?.let { put("minimum", it) }
                            prop.numberMaximum?.let { put("maximum", it) }
                        }
                        else -> {
                            // 对于 OBJECT 和 ARRAY 简化处理，暂时不添加嵌套属性
                        }
                    }
                }
            }
        }

        val requiredProperties = properties.filter { it.required }.map { it.name }

        return buildJsonObject {
            put("type", "object")
            put("properties", propertiesMap)
            if (requiredProperties.isNotEmpty()) {
                put("required", buildJsonArray {
                    requiredProperties.forEach { add(it) }
                })
            }
        }
    }

    /**
     * 将 Float 类型的安全阈值映射为 String 类型。
     * 假设：Float 值是 0.0f, 1.0f, 2.0f, 3.0f 分别对应不同的屏蔽等级。
     * @param floatThreshold Float 类型的安全阈值。
     * @return 对应的 String 类型阈值常量。
     */
    private fun getThresholdStringFromFloat(floatThreshold: Float): String {
        return when (floatThreshold) {
            0.0f -> HarmBlockThreshold.BLOCK_NONE
            1.0f -> HarmBlockThreshold.BLOCK_ONLY_HIGH
            2.0f -> HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE
            3.0f -> HarmBlockThreshold.BLOCK_LOW_AND_ABOVE
            else -> HarmBlockThreshold.HARM_BLOCK_THRESHOLD_UNSPECIFIED
        }
    }

    /**
     * 调用 LLM API 以获取对话回复，支持流式和非流式输出。
     *
     * @param history 一个包含完整对话历史的 `ChatContent` 列表。
     * @param isStreaming 是否开启流式输出。默认为 `false` (非流式)。
     * @return 一个 `Flow<String>`，用于接收来自 LLM 的文本回复片段。
     *         如果 `isStreaming` 为 `false`，则 `Flow` 只会发出一个完整的回复。
     */
    suspend fun chatResponse(history: List<ChatContent>, isStreaming: Boolean = false): Flow<String> = flow {
        val currentSettings = settingsRepository.settingsFlow.first()

        val effectiveResponseSchema: JsonElement? = if (currentSettings.isGraphicalSchemaMode) {
            // 使用宽松的 jsonEncoder 来处理 JSON 元素
            convertGraphicalSchemaToJson(currentSettings.graphicalResponseSchema)
        } else {
            currentSettings.responseSchema.takeIf { it.isNotBlank() }?.let { jsonEncoder.parseToJsonElement(it) } // 使用宽松的 jsonEncoder
        }

        val responseMimeType = if (effectiveResponseSchema != null && effectiveResponseSchema != JsonNull) "application/json" else "text/plain"
        val generationConfig = GenerationConfig(
            temperature = currentSettings.temperature,
            topP = currentSettings.topP,
            topK = currentSettings.topK,
            responseMimeType = responseMimeType,
            responseSchema = effectiveResponseSchema.takeIf { it != JsonNull }
        )

        val safetySettings = listOf(
            SafetySetting(HARM_CATEGORY_HARASSMENT, getThresholdStringFromFloat(currentSettings.harassment)),
            SafetySetting(HARM_CATEGORY_HATE_SPEECH, getThresholdStringFromFloat(currentSettings.hateSpeech)),
            SafetySetting(HARM_CATEGORY_SEXUALLY_EXPLICIT, getThresholdStringFromFloat(currentSettings.sexuallyExplicit)),
            SafetySetting(HARM_CATEGORY_DANGEROUS_CONTENT, getThresholdStringFromFloat(currentSettings.dangerousContent))
        )

        val request = LlmRequest(
            contents = history,
            generationConfig = generationConfig,
            safetySettings = safetySettings
        )
        val modelId = getChatModel()
        val apiKey = getApiKey()

        if (isStreaming) {
            Log.d("LlmRepository", "LLM 聊天流式请求体: ${jsonEncoder.encodeToString(request)}")
            val requestUrl = "${baseLlmApiUrl}v1beta/models/${modelId}:streamGenerateContent?alt=sse&key=${apiKey}"
            Log.d("LlmRepository", "LLM 聊天流式请求 URL: $requestUrl")

            try {
                val response = llmApiService.streamChatResponse(modelId, apiKey, request)

                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        val inputStream = responseBody.byteStream()
                        val reader = inputStream.bufferedReader()

                        try {
                            var line: String?
                            while (reader.readLine().also { line = it } != null) {
                                val currentLine = line ?: continue

                                // 跳过空行和注释行
                                if (currentLine.isBlank() || currentLine.startsWith(":")) {
                                    continue
                                }

                                // 处理 SSE 格式的数据行
                                if (currentLine.startsWith("data: ")) {
                                    val jsonString = currentLine.substringAfter("data: ").trim()

                                    // 检查结束标记
                                    if (jsonString == "[DONE]") {
                                        Log.d("LlmRepository", "收到流式结束标记 [DONE]")
                                        break
                                    }

                                    // 跳过空的数据
                                    if (jsonString.isBlank()) {
                                        continue
                                    }

                                    try {
                                        // 关键修改点 3: 使用宽松的 jsonEncoder 进行解码
                                        val llmResponse = jsonEncoder.decodeFromString<LlmResponse>(jsonString)
                                        val text = llmResponse.candidates
                                            ?.firstOrNull()
                                            ?.content
                                            ?.parts
                                            ?.firstOrNull()
                                            ?.text

                                        if (!text.isNullOrEmpty()) {
                                            Log.d("LlmRepository", "收到流式文本片段: $text")
                                            // 逐字输出：每次 emit 都会发送一个文本片段，在 UI 层实现逐字效果
                                            emit(text)
                                        } else {
                                            Log.d("LlmRepository", "流式响应文本为空")
                                        }
                                    } catch (e: Exception) {
                                        // 这里的 E 级日志会捕获到 'usageMetadata' 的报错，但现在应该能解决
                                        Log.e("LlmRepository", "解析流式响应 JSON 片段失败: $jsonString", e)
                                    }
                                }
                            }
                        } finally {
                            reader.close()
                            inputStream.close()
                        }
                    } else {
                        Log.w("LlmRepository", "流式响应体为空 (body is null)")
                        emit("抱歉，收到了空的流式响应体。")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "无错误详情"
                    Log.e("LlmRepository", "获取 LLM 聊天流式响应失败，状态码: ${response.code()}, 错误: $errorBody")
                    emit("抱歉，无法获取流式回复。错误码: ${response.code()}, 错误: $errorBody")
                }

            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("LlmRepository", "获取 LLM 聊天流式响应时发生异常", e)
                emit("抱歉，无法获取流式回复。错误：${e.localizedMessage}")
            }
        } else {
            Log.d("LlmRepository", "LLM 聊天请求体: ${jsonEncoder.encodeToString(request)}")
            val requestUrl = "${baseLlmApiUrl}v1beta/models/$modelId:generateContent?key=$apiKey"
            Log.d("LlmRepository", "LLM 聊天请求 URL: $requestUrl")

            try {
                val response = llmApiService.getChatResponse(modelId, apiKey, request)
                Log.d("LlmRepository", "LLM 聊天响应: $response")
                emit(response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "抱歉，无法获取回复。")
            } catch (e: CancellationException) {
                // This is an expected cancellation from the downstream collector (e.g., .first()).
                // We should not log this as an error or emit an error message. We just let the flow terminate gracefully.
                Log.d("LlmRepository", "Flow was cancelled by the collector, which is expected for non-streaming calls.")
            } catch (e: Exception) {
                // This is a real, unexpected error (e.g., network issue, JSON parsing error).
                Log.e("LlmRepository", "获取 LLM 聊天响应失败", e)
                emit("抱歉，无法获取回复。错误：${e.localizedMessage}")
            }
        }
    }

    /**
     * 调用 LLM API 为给定的文本生成摘要。
     *
     * @param text 需要被总结的原始文本。
     * @return 从 LLM 返回的摘要文本。如果请求失败或响应格式不正确，则返回一个默认的错误消息。
     */
    suspend fun getSummary(text: String): String {
        val currentSettings = settingsRepository.settingsFlow.first()

        val effectiveResponseSchema: JsonElement? = if (currentSettings.isGraphicalSchemaMode) {
            convertGraphicalSchemaToJson(currentSettings.graphicalResponseSchema)
        } else {
            currentSettings.responseSchema.takeIf { it.isNotBlank() }?.let { Json.parseToJsonElement(it) }
        }

        val responseMimeType = if (effectiveResponseSchema != null && effectiveResponseSchema != JsonNull) "application/json" else "text/plain"
        val generationConfig = GenerationConfig(
            temperature = currentSettings.temperature,
            topP = currentSettings.topP,
            topK = currentSettings.topK,
            responseMimeType = responseMimeType,
            responseSchema = effectiveResponseSchema.takeIf { it != JsonNull }
        )

        val safetySettings = listOf(
            SafetySetting(HARM_CATEGORY_HARASSMENT, getThresholdStringFromFloat(currentSettings.harassment)),
            SafetySetting(HARM_CATEGORY_HATE_SPEECH, getThresholdStringFromFloat(currentSettings.hateSpeech)),
            SafetySetting(HARM_CATEGORY_SEXUALLY_EXPLICIT, getThresholdStringFromFloat(currentSettings.sexuallyExplicit)),
            SafetySetting(HARM_CATEGORY_DANGEROUS_CONTENT, getThresholdStringFromFloat(currentSettings.dangerousContent))
        )

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
        val modelId = getChatModel()
        val apiKey = getApiKey()

        Log.d("LlmRepository", "LLM 摘要请求 contents: ${jsonEncoder.encodeToString(request.contents)}")
        // 重新添加打印完整请求体的日志
        Log.d("LlmRepository", "LLM 摘要请求体: ${jsonEncoder.encodeToString(request)}")

        val requestUrl = "${baseLlmApiUrl}v1beta/models/$modelId:generateContent?key=$apiKey"
        Log.d("LlmRepository", "LLM 摘要请求 URL: $requestUrl")

        val response = llmApiService.getSummary(modelId, apiKey, request)
        Log.d("LlmRepository", "LLM 摘要响应: $response")
        return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "无法生成摘要。"
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
        val apiKey = getApiKey()
        val requestUrl = "${baseLlmApiUrl}v1beta/models/embedding-gecko-001:embedContent?key=$apiKey"
        Log.d("LlmRepository", "LLM 嵌入请求 URL: $requestUrl")
        Log.d("LlmRepository", "LLM 嵌入请求体: $request")

        val response = llmApiService.getEmbedding(apiKey, request)
        Log.d("LlmRepository", "LLM 嵌入响应: $response")
        return response.embedding.values
    }

    /**
     * 调用 LLM API 获取所有可用的模型列表，支持分页。
     *
     * @param apiKey 用于认证的 API 密钥。
     * @param pageToken 如果存在，则用于获取下一页模型列表的令牌。
     * @return Pair<List<ModelDetail>, String?> 包含模型列表和下一页的令牌。
     */
    suspend fun getAvailableModels(apiKey: String, pageToken: String?): Pair<List<ModelDetail>, String?> {
        val requestUrl = "${baseLlmApiUrl}v1beta/models?key=$apiKey" + (pageToken?.let { "&pageToken=$it" } ?: "")
        Log.d("LlmRepository", "获取可用模型请求 URL: $requestUrl")
        Log.d("LlmRepository", "获取可用模型请求 pageToken: $pageToken")
        val response = llmApiService.getAvailableModels(apiKey, pageToken)
        Log.d("LlmRepository", "获取可用模型响应, 模型数量: ${response.models.size}, 下一页token: ${response.nextPageToken}")
        return Pair(response.models, response.nextPageToken)
    }
}