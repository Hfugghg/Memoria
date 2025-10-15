package com.exp.memoria.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * [LLM 对话/摘要 API的请求体]
 *
 * 职责:
 * 1.  精确定义发往 Gemini `generateContent` API 的JSON请求体结构。
 * 2.  **类型安全**: 此 DTO 现在明确规定其`contents`列表必须包含`ChatContent`对象。`ChatContent`类强制要求包含`role`字段，从而在编译时就保证了请求的合规性，避免了运行时的`HTTP 400`错误。
 *
 * 关联:
 * -  由`LlmRepository`在调用`getChatResponse`或`getSummary`时创建实例。
 * -  作为`LlmApiService`中`getChatResponse`和`getSummary`方法的`@Body`参数类型。
 */
@Serializable
data class LlmRequest(
    /** 对话内容列表，每个元素都是一个带有角色的内容块。 */
    val contents: List<ChatContent> // <-- 关键修改：从 Content 改为 ChatContent
)