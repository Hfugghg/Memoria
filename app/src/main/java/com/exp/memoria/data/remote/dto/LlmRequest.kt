package com.exp.memoria.data.remote.dto

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement // 添加这行导入

/**
 * [LLM 对话/摘要 API的请求体]
 *
 * 职责:
 * 1.  精确定义发往 Gemini `generateContent` API 的JSON请求体结构。
 * 2.  **类型安全**: 此 DTO 现在明确规定其`contents`列表必须包含`ChatContent`对象。`ChatContent`类强制要求包含`role`字段，从而在编译时就保证了请求的合规性，避免了运行时的`HTTP 400`错误。
 * 3.  **可扩展性**: 包含对更高级功能的占位，如`systemInstruction`, `tools`, `toolConfig`, `safetySettings`, 和 `generationConfig`，允许未来在不破坏现有代码的情况下轻松添加这些功能。
 *
 * 关联:
 * -  由`LlmRepository`在调用`getChatResponse`或`getSummary`时创建实例。
 * -  作为`LlmApiService`中`getChatResponse`和`getSummary`方法的`@Body`参数类型。
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class LlmRequest(
    /** 对话内容列表，每个元素都是一个带有角色的内容块。 */
    val contents: List<ChatContent>, // <-- 关键修改：从 Content 改为 ChatContent
    /** 系统指令，用于指导模型的行为。 */
    val systemInstruction: SystemInstruction? = null,
    /** 工具列表，模型可以用来与外部系统交互。 */
    val tools: List<Tool>? = null,
    /** 工具配置，用于控制工具的行为。 */
    val toolConfig: ToolConfig? = null,
    /** 安全设置，用于过滤不安全的内容。 */
    val safetySettings: List<SafetySetting>? = null,
    /** 生成配置，用于控制模型的生成过程。 */
    val generationConfig: GenerationConfig? = null
)

/**
 * [系统指令]
 *
 * 用于向模型提供高级别的、持久性的指令，以指导其在整个对话过程中的行为、风格和角色。
 * 它的结构与 `Content` 类似，包含一个 `Part` 列表。
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class SystemInstruction(
    val parts: List<Part>
)

/**
 * [工具]
 *
 * 定义模型可以调用的外部工具或函数，以获取额外信息或执行特定操作。
 * 这是实现 Function Calling 功能的关键部分。
 * 目前是一个占位符，为将来的功能扩展做准备。
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Tool(
    val placeholder: String? = null // 尚未实现
)

/**
 * [工具配置]
 *
 * 用于配置模型如何使用其可用的工具。
 * 目前是一个占位符，为将来的功能扩展做准备。
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ToolConfig(
    val placeholder: String? = null // 尚未实现
)

/**
 * [安全设置]
 *
 * 用于配置 API 的安全过滤器，可以调整对不同类别（如骚扰、仇恨言论等）内容的屏蔽阈值。
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class SafetySetting(
    val category: String,
    val threshold: String
)

/**
 * [生成配置]
 *
 * 用于控制模型生成响应的参数，例如温度 (temperature)、Top-P、Top-K、最大输出长度等。
 * 可以在这里添加 responseMimeType 和 responseSchema。
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class GenerationConfig(
    // 1. 随机性控制 (Randomness Control)
    /**
     * 控制回复的随机程度。较低的值（接近 0.0）生成更可预测、更确定的回复，
     * 较高的值（接近 2.0）生成更多样、更有创意的回复。
     */
    val temperature: Float? = null,

    /**
     * Top-P（或核采样）参数。模型从概率累加和达到此值的 token 集中选择下一个 token。
     * 较低的值（接近 0.0）减少随机性。
     */
    val topP: Float? = null,

    /**
     * Top-K 参数。模型从概率最高的 K 个 token 中选择下一个 token。
     */
    val topK: Int? = null,

    // 2. 输出长度和停止条件 (Length and Stopping)
    /**
     * 指定响应中可生成的最大 token 数量。
     */
    val maxOutputTokens: Int? = null,

    /**
     * 一个字符串列表，当模型在响应中遇到其中任何一个字符串时，将立即停止生成内容。
     */
    val stopSequences: List<String>? = null,

    // 3. 结构化输出 (Structured Output)
    /**
     * 输出响应的 MIME 类型。例如，设置为 "application/json" 以启用 JSON 模式。
     */
    val responseMimeType: String? = null,

    /**
     * 定义输出 JSON 数据的结构（如果 responseMimeType 设置为 "application/json"）。
     * 它是符合 OpenAPI 3.0 规范的 JSON Schema 对象。
     */
    val responseSchema: JsonElement? = null,

    // 4. Token 惩罚 (Token Penalization)
    /**
     * 【频率惩罚】
     * 正值会根据一个 token 在当前生成内容中出现的“次数”来惩罚它。
     * 出现次数越多，惩罚越大，从而减少“重复”的 token，有助于提高多样性。
     */
    val frequencyPenalty: Float? = null,

    /**
     * 【存在惩罚】
     * 正值会根据一个 token 是否在当前生成内容中“存在”来惩罚它。
     * 只要出现过一次，惩罚就会生效，不考虑出现次数。这有助于模型探讨更广泛的话题。
     */
    val presencePenalty: Float? = null,

    // 5. 其他高级参数 (Other Advanced Parameters)
    /**
     * 指定要返回的响应变体（候选答案）数量。您将为所有候选的输出 token 付费。
     * 范围通常为 1 到 8。
     */
    val candidateCount: Int? = null,

    /**
     * 用于采样的随机种子。设置此值可以提高给定输入下的响应的确定性（可复现性）。
     */
    val seed: Int? = null,

    /**
     * 如果为 true，则在响应中导出输出 token 的对数概率（logprobs）。
     */
    val responseLogprobs: Boolean? = null,

    /**
     * 【嵌入输出维度】
     * 指定嵌入输出的维度。
     * 可选值为 768, 1536, 3072。
     */
    val outputDimensionality: Int? = null
)
