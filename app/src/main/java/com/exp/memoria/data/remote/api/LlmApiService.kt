package com.exp.memoria.data.remote.api

import com.exp.memoria.data.remote.dto.EmbeddingRequest
import com.exp.memoria.data.remote.dto.EmbeddingResponse
import com.exp.memoria.data.remote.dto.LlmRequest
import com.exp.memoria.data.remote.dto.LlmResponse
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path // 新增导入 Path
import retrofit2.http.Query

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
 *
 * 关联:
 * -  其具体实现由Retrofit在运行时动态生成。
 * -  `NetworkModule`会提供这个接口的单例。
 * -  `LlmRepository`会注入并调用此接口中定义的方法来执行网络请求。
 */
interface LlmApiService {

    /** 获取对话回复 */
    @POST("v1beta/models/{modelId}:generateContent") // 将模型名称改为路径参数
    suspend fun getChatResponse(
        @Path("modelId") modelId: String, // 添加 modelId 路径参数
        @Query("key") apiKey: String,
        @Body request: LlmRequest // Retrofit会使用LlmRequest(内含List<ChatContent>)来构建请求
    ): LlmResponse

    /** 获取文本摘要 */
    @POST("v1beta/models/{modelId}:generateContent") // 将模型名称改为路径参数
    suspend fun getSummary(
        @Path("modelId") modelId: String, // 添加 modelId 路径参数
        @Query("key") apiKey: String,
        @Body request: LlmRequest // 同上
    ): LlmResponse

    /** 获取文本向量 */
    @POST("v1beta/models/embedding-gecko-001:embedContent")
    suspend fun getEmbedding(
        @Query("key") apiKey: String,
        @Body request: EmbeddingRequest // Retro上会使用EmbeddingRequest(内含EmbeddingContent)来构建请求
    ): EmbeddingResponse

    /** 获取所有可用的LLM模型列表 */
    @GET("v1beta/models") // 注意：这里是 GET 请求，且路径为 /models
    suspend fun getAvailableModels(
        @Query("key") apiKey: String,
        @Query("pageToken") pageToken: String? = null // 添加 pageToken 参数
    ): ModelsResponse
}
