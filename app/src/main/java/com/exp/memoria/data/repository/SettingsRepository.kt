package com.exp.memoria.data.repository

import com.exp.memoria.ui.settings.Settings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settingsFlow: Flow<Settings>
    suspend fun updateApiKey(apiKey: String)
    suspend fun updateChatModel(chatModel: String)
    suspend fun updateTemperature(temperature: Float)
    suspend fun updateTopP(topP: Float)
    suspend fun updateUseLocalStorage(useLocalStorage: Boolean)
}
