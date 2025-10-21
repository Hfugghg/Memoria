package com.exp.memoria.data

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

@Serializable
sealed class Content {
    @SuppressLint("UnsafeOptInUsageError")
    @Serializable
    data class User(val parts: List<Part>) : Content()

    @SuppressLint("UnsafeOptInUsageError")
    @Serializable
    data class Model(val parts: List<Part>) : Content()

    @SuppressLint("UnsafeOptInUsageError")
    @Serializable
    data class Function(val parts: List<Part>) : Content()
}

@Serializable
sealed class Part {
    @SuppressLint("UnsafeOptInUsageError")
    @Serializable
    data class Text(val text: String) : Part()

    @SuppressLint("UnsafeOptInUsageError")
    @Serializable
    data class Image(val mimeType: String, val data: ByteArray) : Part()
}
