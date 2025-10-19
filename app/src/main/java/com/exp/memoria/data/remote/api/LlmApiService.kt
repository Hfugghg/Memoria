package com.exp.memoria.data.remote.api

import com.exp.memoria.data.remote.dto.EmbeddingRequest
import com.exp.memoria.data.remote.dto.EmbeddingResponse
import com.exp.memoria.data.remote.dto.LlmRequest
import com.exp.memoria.data.remote.dto.LlmResponse
import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.Response // <-- 关键修改：添加导入
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

// 响应模型列表的DTO
@Serializable
data class ModelsResponse(
    val models: List<ModelDetail>,
    val nextPageToken: String? = null
)

@Serializable
data class ModelDetail(
    val name: String,
    val version: String,
    val displayName: String,
    val description: String = "",
    val inputTokenLimit: Int,
    val outputTokenLimit: Int,
    val supportedGenerationMethods: List<String>
)

/**
 * [LLM网络服务接口]
 *
 * 职责:
 * 1.  使用Retrofit定义与大语言模型(LLM) API通信的抽象方法。
 * 2.  **强类型契约**: 此接口现在是类型安全的。它明确声明了每个API端点所期望的请求体类型（`LlmRequest`或`EmbeddingRequest`），这些请求体内部又使用了更具体的`ChatContent`或`EmbeddingContent`。这形成了一个完整的、在编译时即可验证的类型安全链。
 * 3.  **流式支持**: 新增了 `streamChatResponse` 方法，用于获取 LLM 的流式回复。
 *
 * 关联:
 * - 其具体实现由Retrofit在运行时动态生成。
 * - `NetworkModule`会提供这个接口的单例。
 * - `LlmRepository`会注入并调用此接口中定义的方法来执行网络请求。
 */
interface LlmApiService {

    /** 获取对话回复 */
//    @Streaming
    @POST("v1beta/models/{modelId}:generateContent")
    suspend fun getChatResponse(
        @Path("modelId") modelId: String,
        @Query("key") apiKey: String,
        @Body request: LlmRequest
    ): LlmResponse

    /** 获取对话回复的流式版本 */
    @Streaming
    @POST("v1beta/models/{modelId}:streamGenerateContent?alt=sse") // 注意这里是 streamGenerateContent
    suspend fun streamChatResponse(
        @Path("modelId") modelId: String,
        @Query("key") apiKey: String,
        @Body request: LlmRequest
    ): Response<ResponseBody> // <-- 关键修改：返回 Response<ResponseBody>

    /** 获取文本摘要 */
    @POST("v1beta/models/{modelId}:generateContent")
    suspend fun getSummary(
        @Path("modelId") modelId: String,
        @Query("key") apiKey: String,
        @Body request: LlmRequest
    ): LlmResponse

    /** 获取文本向量 */
    @POST("v1beta/models/embedding-gecko-001:embedContent")
    suspend fun getEmbedding(
        @Query("key") apiKey: String,
        @Body request: EmbeddingRequest
    ): EmbeddingResponse

    /** 获取所有可用的LLM模型列表 */
    @GET("v1beta/models")
    suspend fun getAvailableModels(
        @Query("key") apiKey: String,
        @Query("pageToken") pageToken: String? = null
    ): ModelsResponse
}
