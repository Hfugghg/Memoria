package com.exp.memoria.data.remote.dto

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

/**
 * [LLM 向量化 API 的响应体]
 *
 * 这是一个数据传输对象 (DTO)，用于精确匹配和解析从 Gemini `embedContent` API 返回的 JSON 响应结构。
 *
 * @property embedding 包含了实际向量化结果的顶层对象。
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class EmbeddingResponse(
    val embedding: Embedding
) {
    /**
     * [向量化结果的内部容器]
     *
     * @property values 一个浮点数列表，代表了输入内容经过模型计算后得到的语义向量。这个向量可以用于相似度计算、聚类等下游任务。
     */
    @Serializable
    data class Embedding(
        val values: List<Float>
    )
}