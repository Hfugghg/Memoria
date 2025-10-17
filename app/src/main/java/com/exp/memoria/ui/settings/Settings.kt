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
 * @property useLocalStorage 一个布尔值，指示是否应将记忆（对话历史）存储在本地设备上。
 * @property harassment 安全设置：骚扰内容的屏蔽阈值。
 * @property hateSpeech 安全设置：仇恨言论内容的屏蔽阈值。
 * @property sexuallyExplicit 安全设置：色情内容的屏蔽阈值。
 * @property dangerousContent 安全设置：危险内容内容的屏蔽阈值。
 */
data class Settings(
    val apiKey: String = "",
    val chatModel: String = "",
    val temperature: Float = 0.8f,
    val topP: Float = 0.95f,
    val useLocalStorage: Boolean = true,
    val harassment: Float = 0.0f,
    val hateSpeech: Float = 0.0f,
    val sexuallyExplicit: Float = 0.0f,
    val dangerousContent: Float = 0.0f
)
