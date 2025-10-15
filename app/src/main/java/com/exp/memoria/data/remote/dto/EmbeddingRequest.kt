package com.exp.memoria.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * [LLM 向量化 API的请求体]
 *
 * 职责:
 * 1.  精确定义发往 Gemini `embedContent` API 的JSON请求体结构。
 * 2.  **类型安全**: 此 DTO 明确规定其`content`字段必须是`EmbeddingContent`类型。`EmbeddingContent`类不包含`role`字段，这同样在编译时保证了请求的合- 规性，因为`embedContent` API 不接受`role`。
 *
 * 关联:
 * -  由`LlmRepository`在调用`getEmbedding`时创建实例。
 * -  作为`LlmApiService`中`getEmbedding`方法的`@Body`参数类型。
 */
@Serializable
data class EmbeddingRequest(
    /** 需要被向量化的内容。 */
    val content: EmbeddingContent // <-- 关键修改：从 Content 改为 EmbeddingContent
)