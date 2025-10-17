package com.exp.memoria.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ModelInfo(
    val name: String,
    val displayName: String,
    val inputTokenLimit: Int,
    val outputTokenLimit: Int
)
