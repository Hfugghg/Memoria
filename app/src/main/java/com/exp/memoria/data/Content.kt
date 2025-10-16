package com.exp.memoria.data

import kotlinx.serialization.Serializable

@Serializable
sealed class Content {
    @Serializable
    data class User(val parts: List<Part>) : Content()

    @Serializable
    data class Model(val parts: List<Part>) : Content()

    @Serializable
    data class Function(val parts: List<Part>) : Content()
}

@Serializable
sealed class Part {
    @Serializable
    data class Text(val text: String) : Part()

    @Serializable
    data class Image(val mimeType: String, val data: ByteArray) : Part()
}
