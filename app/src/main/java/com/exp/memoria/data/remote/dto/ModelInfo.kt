package com.exp.memoria.data.remote.dto

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ModelInfo(
    val name: String,
    val displayName: String,
    val inputTokenLimit: Int,
    val outputTokenLimit: Int
)
