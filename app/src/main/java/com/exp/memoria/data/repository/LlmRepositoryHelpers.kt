package com.exp.memoria.data.repository

import com.exp.memoria.data.remote.dto.GenerationConfig
import com.exp.memoria.data.remote.dto.SafetySetting
import com.exp.memoria.ui.settings.HarmBlockThreshold
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 封装 LLM 请求所需的三个核心组件：
 * MIME 类型, 生成配置和安全设置。
 */
internal data class LlmRequestComponents(
    val responseMimeType: String,
    val generationConfig: GenerationConfig,
    val safetySettings: List<SafetySetting>
)

/**
 * 通用的 LLM 仓库配置和工具类
 * 职责: 封装 API URL、JSON 序列化器和请求构建逻辑。
 */
@Singleton
class LlmRepositoryHelpers @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    internal val baseLlmApiUrl = "https://generativelanguage.googleapis.com/"

    // 配置 Json 序列化器，使其编码所有默认值（包括 null）
    internal val jsonEncoder = Json {
        // 启用忽略未知键，这是解决 'usageMetadata' 报错的关键！
        ignoreUnknownKeys = true
        // 保持格式化，方便调试
        prettyPrint = true
        // 允许编码/解码非结构化的 JSON 原始值（例如，纯字符串）
        allowStructuredMapKeys = false
    }

    // 定义标准安全类别
    internal companion object {
        const val HARM_CATEGORY_HARASSMENT = "HARM_CATEGORY_HARASSMENT"
        const val HARM_CATEGORY_HATE_SPEECH = "HARM_CATEGORY_HATE_SPEECH"
        const val HARM_CATEGORY_SEXUALLY_EXPLICIT = "HARM_CATEGORY_SEXUALLY_EXPLICIT"
        const val HARM_CATEGORY_DANGEROUS_CONTENT = "HARM_CATEGORY_DANGEROUS_CONTENT"
    }

    /**
     * 从 SettingsRepository 异步获取当前配置的 API 密钥。
     * 这是访问 LLM 服务所必需的。
     */
    internal suspend fun getApiKey(): String {
        return settingsRepository.settingsFlow.first().apiKey
    }

    /**
     * 从 SettingsRepository 异步获取当前配置的聊天模型名称。
     * 这是访问 LLM 服务所必需的。
     */
    internal suspend fun getChatModel(): String {
        val model = settingsRepository.settingsFlow.first().chatModel
        // 修复：无论如何，都移除 "models/" 前缀
        return model.removePrefix("models/")
    }

    /**
     * 获取当前配置的嵌入模型名称。
     * TODO: 未来可以考虑让此设置也从 SettingsRepository 读取。
     */
    internal fun getEmbeddingModel(): String {
        return "gemini-embedding-001"
    }

    /**
     * 将 0.0f 到 1.0f 范围内的浮点数安全阈值映射为对应的 HarmBlockThreshold 字符串常量。
     *
     * @param floatThreshold Float 类型的安全阈值，取值范围为 0.0f 到 1.0f。
     * @return 对应的 String 类型阈值常量。
     */
    private fun getThresholdStringFromFloat(floatThreshold: Float): String {
        return when {
            floatThreshold < 0.125f -> HarmBlockThreshold.HARM_BLOCK_THRESHOLD_UNSPECIFIED
            floatThreshold < 0.375f -> HarmBlockThreshold.BLOCK_NONE
            floatThreshold < 0.625f -> HarmBlockThreshold.BLOCK_ONLY_HIGH
            floatThreshold < 0.875f -> HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE
            else -> HarmBlockThreshold.BLOCK_LOW_AND_ABOVE
        }
    }

    /**
     * 从设置中构建 LLM 请求中可复用的配置组件：
     * responseMimeType, GenerationConfig, 和 safetySettings。
     *
     * @param responseSchema 当前对话的 Response Schema JSON 字符串。
     * @return 包含所有配置组件的 LlmRequestComponents 数据类。
     */
    internal suspend fun buildLlmRequestComponents(responseSchema: String?): LlmRequestComponents {
        val currentSettings = settingsRepository.settingsFlow.first()

        val effectiveResponseSchema: JsonElement? = responseSchema?.takeIf { it.isNotBlank() }
            ?.let { jsonEncoder.parseToJsonElement(it) }

        val responseMimeType =
            if (effectiveResponseSchema != null && effectiveResponseSchema != JsonNull) "application/json" else "text/plain"

        // 构建完整的 GenerationConfig
        val generationConfig = GenerationConfig(
            temperature = currentSettings.temperature,
            topP = currentSettings.topP,
            topK = currentSettings.topK?.takeIf { it > 0 },
            maxOutputTokens = currentSettings.maxOutputTokens?.takeIf { it > 0 },
            stopSequences = currentSettings.stopSequences.takeIf { it.isNotBlank() }
                ?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() },
            frequencyPenalty = currentSettings.frequencyPenalty.takeIf { it != 0.0f },
            presencePenalty = currentSettings.presencePenalty.takeIf { it != 0.0f },
            candidateCount = currentSettings.candidateCount.takeIf { it > 1 },
            seed = currentSettings.seed,
            responseMimeType = responseMimeType,
            responseSchema = effectiveResponseSchema.takeIf { it != JsonNull }
        )

        // 构建 SafetySettings
        val safetySettings = listOf(
            SafetySetting(HARM_CATEGORY_HARASSMENT, getThresholdStringFromFloat(currentSettings.harassment)),
            SafetySetting(HARM_CATEGORY_HATE_SPEECH, getThresholdStringFromFloat(currentSettings.hateSpeech)),
            SafetySetting(
                HARM_CATEGORY_SEXUALLY_EXPLICIT,
                getThresholdStringFromFloat(currentSettings.sexuallyExplicit)
            ),
            SafetySetting(
                HARM_CATEGORY_DANGEROUS_CONTENT,
                getThresholdStringFromFloat(currentSettings.dangerousContent)
            )
        )

        return LlmRequestComponents(responseMimeType, generationConfig, safetySettings)
    }
}