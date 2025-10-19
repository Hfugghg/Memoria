package com.exp.memoria.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * [LLM API响应的数据传输对象 (DTO)]
 *
 * 职责:
 * 1.  **精确映射JSON结构**: 此文件内的所有数据类共同构成了对Gemini API `generateContent` 端点返回的JSON响应的完整、强类型映射。
 * 2.  **反序列化支持**: 所有类都使用了 `@Serializable` 注解。
 * 3.  **封装与隔离**: 封装响应数据结构，隔离网络层与应用业务逻辑层。
 */
@Serializable
data class LlmResponse(
    /** 包含一个或多个候选回复的列表。通常我们只关心第一个。 */
    val candidates: List<Candidate>? = null,
    /** [新增] 关于本次调用的 Token 计数等元数据。在流式响应的每个片段中都可能出现。 */
    val usageMetadata: UsageMetadata? = null,
    /** [新增] 模型版本信息。 */
    val modelVersion: String? = null
    // val promptFeedback: PromptFeedback? = null // 可选字段，如果需要可以取消注释。
)

/**
 * 代表一个由模型生成的候选回复。
 *
 * @property content 候选回复的核心内容。
 * @property finishReason 模型停止生成内容的原因，例如 "STOP" (正常完成)。
 * @property safetyRatings 关于此候选回复内容的安全评估列表。
 */
@Serializable
data class Candidate(
    val content: Content? = null,
    val finishReason: String? = null,
    val index: Int? = null,
    val safetyRatings: List<SafetyRating>? = null
)

/**
 * 代表API响应中的内容块。
 *
 * @property parts 组成内容的一个或多个部分。对于简单的文本聊天，这里通常只有一个Part。
 * @property role 内容的角色，对于模型的回复，这个字段的值通常是 "model"。
 */
@Serializable
data class Content(
    val parts: List<Part>,
    val role: String
)

/**
 * 对内容的安全评估。
 *
 * @property category 安全问题的类别。
 * @property probability 该类别问题的可能性评估。
 */
@Serializable
data class SafetyRating(
    val category: String,
    val probability: String
)

/**
 * [新增] 关于Token使用情况的元数据。
 */
@Serializable
data class UsageMetadata(
    val promptTokenCount: Int? = null,
    val candidatesTokenCount: Int? = null,
    val totalTokenCount: Int? = null
    // val promptTokensDetails: List<PromptTokensDetails>? = null // 您的日志中出现，但可以忽略或按需添加
)