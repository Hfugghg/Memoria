package com.exp.memoria.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.exp.memoria.ui.settings.Settings
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object PreferencesKeys {
        val API_KEY = stringPreferencesKey("api_key")
        val CHAT_MODEL = stringPreferencesKey("chat_model")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val TOP_P = floatPreferencesKey("top_p")
        val USE_LOCAL_STORAGE = booleanPreferencesKey("use_local_storage")
    }

    override val settingsFlow = dataStore.data.map {
        val apiKey = it[PreferencesKeys.API_KEY] ?: ""
        val chatModel = it[PreferencesKeys.CHAT_MODEL] ?: ""
        val temperature = it[PreferencesKeys.TEMPERATURE] ?: 0.8f
        val topP = it[PreferencesKeys.TOP_P] ?: 0.95f
        val useLocalStorage = it[PreferencesKeys.USE_LOCAL_STORAGE] ?: true
        Settings(apiKey, chatModel, temperature, topP, useLocalStorage)
    }

    override suspend fun updateApiKey(apiKey: String) {
        dataStore.edit { it[PreferencesKeys.API_KEY] = apiKey }
    }

    override suspend fun updateChatModel(chatModel: String) {
        dataStore.edit { it[PreferencesKeys.CHAT_MODEL] = chatModel }
    }

    override suspend fun updateTemperature(temperature: Float) {
        dataStore.edit { it[PreferencesKeys.TEMPERATURE] = temperature }
    }

    override suspend fun updateTopP(topP: Float) {
        dataStore.edit { it[PreferencesKeys.TOP_P] = topP }
    }

    override suspend fun updateUseLocalStorage(useLocalStorage: Boolean) {
        dataStore.edit { it[PreferencesKeys.USE_LOCAL_STORAGE] = useLocalStorage }
    }
}