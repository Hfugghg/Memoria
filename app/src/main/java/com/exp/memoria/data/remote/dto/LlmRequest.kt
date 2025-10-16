package com.exp.memoria.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * [LLM 对话/摘要 API的请求体]
 *
 * 职责:
 * 1.  精确定义发往 Gemini `generateContent` API 的JSON请求体结构。
 * 2.  **类型安全**: 此 DTO 现在明确规定其`contents`列表必须包含`ChatContent`对象。`ChatContent`类强制要求包含`role`字段，从而在编译时就保证了请求的合规性，避免了运行时的`HTTP 400`错误。
 * 3.  **可扩展性**: 包含对更高级功能的占位，如`systemInstruction`, `tools`, `safetySettings`, 和 `generationConfig`，允许未来在不破坏现有代码的情况下轻松添加这些功能。
 *
 * 关联:
 * -  由`LlmRepository`在调用`getChatResponse`或`getSummary`时创建实例。
 * -  作为`LlmApiService`中`getChatResponse`和`getSummary`方法的`@Body`参数类型。
 */
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

/** 系统指令（占位）*/
@Serializable
data class SystemInstruction(
    val placeholder: String? = null // 尚未实现
)

/** 工具（占位）*/
@Serializable
data class Tool(
    val placeholder: String? = null // 尚未实现
)

/** 工具配置（占位）*/
@Serializable
data class ToolConfig(
    val placeholder: String? = null // 尚未实现
)

/** 安全设置（占位）*/
@Serializable
data class SafetySetting(
    val placeholder: String? = null // 尚未实现
)

/** 生成配置（占位）*/
@Serializable
data class GenerationConfig(
    val placeholder: String? = null // 尚未实现
)