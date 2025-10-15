package com.exp.memoria.data.remote.api

import com.exp.memoria.data.remote.dto.EmbeddingRequest
import com.exp.memoria.data.remote.dto.EmbeddingResponse
import com.exp.memoria.data.remote.dto.LlmRequest
import com.exp.memoria.data.remote.dto.LlmResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * [LLM网络服务接口]
 *
 * 职责:
 * 1. 使用Retrofit定义与大语言模型(LLM) API的通信接口 [cite: 80]。
 * 2. 定义一个方法用于发送组装好的上下文并获取AI回答（主LLM）[cite: 35]。
 * 3. 定义一个方法用于发送原始对话，获取其摘要文本（浓缩LLM）[cite: 14, 25]。
 * 4. (可选) 定义一个方法用于获取文本的向量表示(Embedding)。如果Embedding服务和LLM是分开的，就需要额外定义。
 *
 * 关联:
 * - 这是一个Retrofit接口，使用 @POST, @Body, @Header 等注解。
 * - 其实现由Retrofit在运行时动态生成。
 * - LlmRepository 会持有该接口的实例，并发起网络调用。
 */

interface LlmApiService {

    @POST("v1beta/models/gemini-pro:generateContent")
    suspend fun getChatResponse(
        @Query("key") apiKey: String,
        @Body request: LlmRequest
    ): LlmResponse

    @POST("v1beta/models/gemini-pro:generateContent")
    suspend fun getSummary(
        @Query("key") apiKey: String,
        @Body request: LlmRequest
    ): LlmResponse

    @POST("v1beta/models/embedding-001:embedContent")
    suspend fun getEmbedding(
        @Query("key") apiKey: String,
        @Body request: EmbeddingRequest
    ): EmbeddingResponse
}
