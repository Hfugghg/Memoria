package com.exp.memoria.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * [LLM API响应的数据传输对象 (DTO)]
 *
 * 职责:
 * 1.  **精确映射JSON结构**: 此文件内的所有数据类共同构成了对Gemini API `generateContent` 端点返回的JSON响应的完整、强类型映射。这是确保网络数据能被安全、正确解析为Kotlin对象的关键。
 * 2.  **反序列化支持**: 所有类都使用了 `@Serializable` 注解，这是为了让 `kotlinx.serialization` 库能够自动将API返回的JSON文本流转换为这些Kotlin类的实例。
 * 3.  **封装与隔离**: 将响应数据结构封装在专门的DTO文件中，可以有效隔离网络层与应用业务逻辑层。即使未来API响应格式发生变化，我们也只需要修改这个文件，而不会影响到Repository或ViewModel等上层代码。
 *
 * 关联:
 * - `LlmApiService` 接口中，`getChatResponse` 和 `getSummary` 等挂起函数的返回值类型被定义为 `LlmResponse`。
 * - `LlmRepository` 在接收到网络响应后，会得到一个由Retrofit和Kotlinx.serialization自动填充好的 `LlmResponse` 实例，并从中提取所需的数据（例如，第一位候选者的第一个文本部分）。
 */
@Serializable
data class LlmResponse(
    /** 包含一个或多个候选回复的列表。通常我们只关心第一个。 */
    val candidates: List<Candidate>
    // val promptFeedback: PromptFeedback? = null // 可选字段，如果需要分析安全反馈或提示词问题，可以取消此行注释并定义相应的数据类。
)

/**
 * 代表一个由模型生成的候选回复。
 *
 * @property content 候选回复的核心内容。这是我们将要提取并展示给用户的主要信息。
 * @property finishReason 模型停止生成内容的原因，例如 "STOP" (正常完成) 或 "MAX_TOKENS" (达到最大长度限制)。
 * @property safetyRatings 关于此候选回复内容的安全评估列表。
 */
@Serializable
data class Candidate(
    val content: Content, // 注意：这里的Content是响应专用的Content类
    val finishReason: String? = null,
    val index: Int? = null,
    val safetyRatings: List<SafetyRating>? = null
)

/**
 * 代表API响应中的内容块。
 *
 * 这是导致你之前报错 `Unresolved reference 'parts'` 的核心。
 * 我们必须在此处明确定义这个类，以匹配JSON响应的结构。
 *
 * @property parts 组成内容的一个或多个部分。对于简单的文本聊天，这里通常只有一个Part。
 * @property role 内容的角色，对于模型的回复，这个字段的值通常是 "model"。
 */
@Serializable
data class Content(
    val parts: List<Part>, // <-- 关键！这个属性现在被正确定义了
    val role: String
)

/**
 * 对内容的安全评估。
 *
 * @property category 安全问题的类别，例如 "HARM_CATEGORY_HARASSMENT"。
 * @property probability 该类别问题的可能性评估，例如 "NEGLIGIBLE" (可忽略)。
 */
@Serializable
data class SafetyRating(
    val category: String,
    val probability: String
)