package com.exp.memoria.ui.settings

data class Settings(
    val apiKey: String = "",
    val chatModel: String = "",
    val temperature: Float = 0.8f,
    val topP: Float = 0.95f,
    val useLocalStorage: Boolean = true
)
