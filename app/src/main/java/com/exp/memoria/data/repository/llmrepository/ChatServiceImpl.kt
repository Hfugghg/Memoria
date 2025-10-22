package com.exp.memoria.data.repository.llmrepository

import android.util.Log
import com.exp.memoria.data.remote.api.LlmApiService
import com.exp.memoria.data.remote.dto.ChatContent
import com.exp.memoria.data.remote.dto.LlmRequest
import com.exp.memoria.data.remote.dto.LlmResponse
import com.exp.memoria.data.remote.dto.SystemInstruction
import com.exp.memoria.data.repository.ChatChunkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 聊天功能实现
 */
@Singleton
class ChatServiceImpl @Inject constructor(
    private val llmApiService: LlmApiService,
    private val helpers: LlmRepositoryHelpers
) : ChatService {

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
    override fun chatResponse(
        history: List<ChatContent>,
        systemInstruction: String?,
        responseSchema: String?,
        isStreaming: Boolean
    ): Flow<ChatChunkResult> = flow {
        // 使用重构后的函数获取组件
        val components = helpers.buildLlmRequestComponents(responseSchema)
        val generationConfig = components.generationConfig
        val safetySettings = components.safetySettings

        // 如果 systemInstruction 不为空，则将其从 JSON 字符串解析为 SystemInstruction 对象
        val systemInstructionObject = try {
            systemInstruction?.takeIf { it.isNotBlank() }?.let {
                helpers.jsonEncoder.decodeFromString<SystemInstruction>(it)
            }
        } catch (e: Exception) {
            Log.e("ChatServiceImpl", "解析 systemInstruction JSON 失败: $systemInstruction", e)
            // 根据业务需求决定如何处理错误，这里选择忽略无效的指令
            null
        }

        val request = LlmRequest(
            contents = history,
            systemInstruction = systemInstructionObject,
            generationConfig = generationConfig,
            safetySettings = safetySettings
        )
        val modelId = helpers.getChatModel()
        val apiKey = helpers.getApiKey()

        if (isStreaming) {
            Log.d("ChatServiceImpl", "LLM 聊天流式请求体: ${helpers.jsonEncoder.encodeToString(request)}")
            val requestUrl =
                "${helpers.baseLlmApiUrl}v1beta/models/${modelId}:streamGenerateContent?alt=sse&key=${apiKey}"
            Log.d("ChatServiceImpl", "LLM 聊天流式请求 URL: $requestUrl")

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
                                        Log.d("ChatServiceImpl", "收到流式结束标记 [DONE]")
                                        break
                                    }

                                    // 跳过空的数据
                                    if (jsonString.isBlank()) {
                                        continue
                                    }
                                    // 解析并处理数据块，如果结果非空则发出
                                    parseAndProcessStreamData(jsonString)?.let { emit(it) }
                                }
                            }
                        } finally {
                            reader.close()
                            inputStream.close()
                        }
                    } else {
                        Log.w("ChatServiceImpl", "流式响应体为空 (body is null)")
                        emit(ChatChunkResult.Error("抱歉，收到了空的流式响应体。"))
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "无错误详情"
                    Log.e("ChatServiceImpl", "获取 LLM 聊天流式响应失败，状态码: ${response.code()}, 错误: $errorBody")
                    emit(ChatChunkResult.Error("抱歉，无法获取流式回复。错误码: ${response.code()}, 错误: $errorBody"))
                }

            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("ChatServiceImpl", "获取 LLM 聊天流式响应时发生异常", e)
                emit(ChatChunkResult.Error("抱歉，无法获取流式回复。错误：${e.localizedMessage}"))
            }
        } else {
            Log.d("ChatServiceImpl", "LLM 聊天请求体: ${helpers.jsonEncoder.encodeToString(request)}")
            val requestUrl = "${helpers.baseLlmApiUrl}v1beta/models/$modelId:generateContent?key=$apiKey"
            Log.d("ChatServiceImpl", "LLM 聊天请求 URL: $requestUrl")

            try {
                val response = llmApiService.getChatResponse(modelId, apiKey, request)
                Log.d("ChatServiceImpl", "LLM 聊天响应: $response")
                emit(processLlmResponse(response))

            } catch (_: CancellationException) {
                Log.d(
                    "ChatServiceImpl",
                    "Flow was cancelled by the collector, which is expected for non-streaming calls."
                )
            } catch (e: Exception) {
                Log.e("ChatServiceImpl", "获取 LLM 聊天响应失败", e)
                emit(ChatChunkResult.Error("抱歉，无法获取回复。错误：${e.localizedMessage}"))
            }
        }
    }

    /**
     * 处理来自 LLM 的非流式响应。
     *
     * @param llmResponse 由 Retrofit 解析的 LlmResponse 对象。
     * @return ChatChunkResult.Success 包含提取的文本，或 ChatChunkResult.Error 如果文本为空或被阻止。
     */
    private fun processLlmResponse(llmResponse: LlmResponse): ChatChunkResult {
        val text = llmResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        val totalTokenCount = llmResponse.usageMetadata?.totalTokenCount

        return if (!text.isNullOrEmpty()) {
            ChatChunkResult.Success(text, totalTokenCount)
        } else {
            val errorMsg = "抱歉，无法获取回复（响应为空或被阻止）。"
            Log.w("ChatServiceImpl", "非流式响应文本为空: $errorMsg")
            ChatChunkResult.Error(errorMsg)
        }
    }

    /**
     * 解析并处理单个流式响应数据块 (JSON)。
     *
     * @param jsonString 从流中读取的 JSON 字符串。
     * @return 如果数据块有效且包含文本或错误，则返回 ChatChunkResult；如果数据块应被忽略，则返回 null。
     */
    private fun parseAndProcessStreamData(jsonString: String): ChatChunkResult? {
        return try {
            val llmResponse = helpers.jsonEncoder.decodeFromString<LlmResponse>(jsonString)
            val text = llmResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            val totalTokenCount = llmResponse.usageMetadata?.totalTokenCount

            if (!text.isNullOrEmpty()) {
                Log.d("ChatServiceImpl", "收到流式文本片段: $text")
                ChatChunkResult.Success(text, totalTokenCount)
            } else if (totalTokenCount != null) {
                // 这个数据块可能只包含元数据（例如，在流的末尾）。
                // 发出一个带有空文本的 Success 事件，以便 ViewModel 可以更新令牌计数。
                Log.d("ChatServiceImpl", "收到流式元数据，总令牌数: $totalTokenCount")
                ChatChunkResult.Success("", totalTokenCount)
            } else {
                if (llmResponse.candidates?.isNotEmpty() == true) {
                    val errorMsg = "响应被阻止或为空。"
                    Log.w("ChatServiceImpl", "流式响应块文本为空，可能被阻止。")
                    ChatChunkResult.Error(errorMsg)
                } else {
                    // 可能是其他类型的 SSE 消息（例如，只有 safetyRatings），应该忽略。
                    Log.d("ChatServiceImpl", "流式响应无有效候选项或文本。")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("ChatServiceImpl", "解析流式响应 JSON 片段失败: $jsonString", e)
            ChatChunkResult.Error("解析响应数据失败: ${e.localizedMessage}")
        }
    }
}
