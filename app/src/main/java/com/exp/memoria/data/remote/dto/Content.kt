package com.exp.memoria.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * 该文件定义了与Gemini API进行数据交换时所使用的核心数据传输对象 (DTOs)。
 * 为了类型安全和API的精确性，我们为不同的API端点定义了不同的Content结构。
 */

/**
 * 代表对话或摘要请求中的一段内容。
 *
 * 根据Gemini API的`generateContent`规范，每一段内容都必须明确其来源角色。
 * @property role 内容的角色，通常是 "user" (表示用户输入) 或 "model" (表示AI的回复)。
 * @property parts 内容的具体组成部分，通常是一个文本片段。
 */
@Serializable
data class ChatContent(
    val role: String,
    val parts: List<Part>
)

/**
 * 代表向量化 (Embedding) 请求中的内容。
 *
 * 根据Gemini API的`embedContent`规范，这部分内容不应包含`role`字段。
 * @property parts 内容的具体组成部分。
 */
@Serializable
data class EmbeddingContent(
    val parts: List<Part>
)

/**
 * 代表内容的最小可分割单元。
 *
 * @property text 具体的文本字符串。
 */
@Serializable
data class Part(
    val text: String
)