package com.exp.memoria.data.remote.dto

/**
 * [数据传输对象 (DTO)]
 *
 * 职责:
 * 1. 定义与LLM API进行网络通信时所用的数据结构。
 * 2. LlmRequest.kt: 对应API请求体的JSON结构。
 * 3. LlmResponse.kt: 对应API响应体的JSON结构。
 * 4. 使用 @Serializable 注解，以便 Kotlinx.serialization 库能自动进行JSON和Kotlin对象之间的转换 [cite: 83]。
 *
 * 关联:
 * - 这些类被用于 LlmApiService 接口方法的参数和返回值类型。
 * - LlmRepository 在调用API前后会创建或接收这些类的实例。
 */
class LlmResponse {
}