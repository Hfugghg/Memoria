package com.exp.memoria.data.repository


/**
 * [LLM数据仓库]
 *
 * 职责:
 * 1. 封装所有与远程LLM API的交互。
 * 2. 提供一个高级接口，屏蔽网络请求、JSON解析和错误处理的复杂性。
 * 3. 提供方法：
 * - `getChatResponse(context: String)`: 调用主LLM获取对话回复。
 * - `getSummary(text: String)`: 调用浓缩LLM获取摘要 [cite: 14, 25]。
 * - `getEmbedding(text: String)`: 获取文本的向量 [cite: 25]。
 *
 * 关联:
 * - 它会注入 LlmApiService。
 * - GetChatResponseUseCase 和 ProcessMemoryUseCase 会注入并使用这个Repository来与远程AI模型交互。
 */
class LlmRepository {
}