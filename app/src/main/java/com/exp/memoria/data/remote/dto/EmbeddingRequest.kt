package com.exp.memoria.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class EmbeddingRequest(
    val content: Content
)