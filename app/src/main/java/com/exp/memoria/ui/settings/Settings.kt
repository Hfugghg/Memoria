package com.exp.memoria.ui.settings

/**
 * [应用设置数据类]
 *
 * 这是一个数据类，用于封装应用的所有可配置项。它通常用于与设置界面进行数据绑定，
 * 并持久化到 SharedPreferences 或 DataStore 中。
 *
 * @property apiKey 用于访问大语言模型 (LLM) 服务的 API 密钥。
 * @property chatModel 指定使用的聊天模型名称，例如 "gemini-pro"。
 * @property temperature 控制生成文本的随机性。值越高，输出越随机、越有创意。
 * @property topP 一种替代温度采样的方法，称为核心采样。模型会从概率质量超过 topP 值的词汇中进行选择。
 * @property topK 一种替代温度采样的方法，模型会考虑概率最高的 topK 个令牌。0表示不设置。
 * @property maxOutputTokens 设置模型响应中允许的最大令牌数。
 * @property stopSequences 一个字符串列表，当模型在响应中遇到其中任何一个字符串时，将立即停止生成内容。
 * @property responseMimeType 指定响应的MIME类型。
 * @property responseLogprobs 是否返回响应的对数概率。
 * @property frequencyPenalty 频率惩罚。正值会根据一个 token 在当前生成内容中出现的“次数”来惩罚它，从而减少重复。
 * @property presencePenalty 存在惩罚。正值会根据一个 token 是否在当前生成内容中“存在”来惩罚它，有助于模型探讨更广泛的话题。
 * @property candidateCount 指定要返回的响应变体（候选答案）数量。
 * @property seed 用于采样的随机种子，以提高响应的确定性。
 * @property useLocalStorage 一个布尔值，指示是否应将记忆（对话历史）存储在本地设备上。
 * @property harassment 安全设置：骚扰内容的屏蔽阈值。
 * @property hateSpeech 安全设置：仇恨言论内容的屏蔽阈值。
 * @property sexuallyExplicit 安全设置：色情内容的屏蔽阈值。
 * @property dangerousContent 安全设置：危险内容内容的屏蔽阈值。
 * @property isStreamingEnabled 是否启用流式输出。
 */
data class Settings(
    val apiKey: String = "",
    val chatModel: String = "",
    val temperature: Float = 0.8f,
    val topP: Float = 0.95f,
    val topK: Int? = null,
    val maxOutputTokens: Int? = null,
    val stopSequences: String = "",
    val responseMimeType: String = "",
    val responseLogprobs: Boolean = false,
    val frequencyPenalty: Float = 0.0f,
    val presencePenalty: Float = 0.0f,
    val candidateCount: Int = 1,
    val seed: Int? = null,
    val useLocalStorage: Boolean = true,
    val harassment: Float = 0.0f,
    val hateSpeech: Float = 0.0f,
    val sexuallyExplicit: Float = 0.0f,
    val dangerousContent: Float = 0.0f,
    val isStreamingEnabled: Boolean = false
)
