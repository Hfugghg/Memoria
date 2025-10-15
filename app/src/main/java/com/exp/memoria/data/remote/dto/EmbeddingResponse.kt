package com.exp.memoria.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class EmbeddingResponse(
    val embedding: Embedding
) {
    @Serializable
    data class Embedding(
        val values: List<Float>
    )
}