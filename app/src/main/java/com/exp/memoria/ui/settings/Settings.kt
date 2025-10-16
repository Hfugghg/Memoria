package com.exp.memoria.ui.settings

data class Settings(
    val apiKey: String = "",
    val chatModel: String = "",
    val temperature: Float = 0.8f,
    val topP: Float = 0.95f,
    val useLocalStorage: Boolean = true,
    val harassment: Float = 0.0f,
    val hateSpeech: Float = 0.0f,
    val sexuallyExplicit: Float = 0.0f,
    val dangerousContent: Float = 0.0f
)
