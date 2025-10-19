package com.exp.memoria.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.exp.memoria.ui.settings.Settings
import com.exp.memoria.ui.settings.JsonSchemaProperty // Import JsonSchemaProperty
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object PreferencesKeys {
        val API_KEY = stringPreferencesKey("api_key")
        val CHAT_MODEL = stringPreferencesKey("chat_model")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val TOP_P = floatPreferencesKey("top_p")
        val TOP_K = intPreferencesKey("top_k")
        val USE_LOCAL_STORAGE = booleanPreferencesKey("use_local_storage")
        val HARASSMENT = floatPreferencesKey("harassment")
        val HATE_SPEECH = floatPreferencesKey("hate_speech")
        val SEXUALLY_EXPLICIT = floatPreferencesKey("sexually_explicit")
        val DANGEROUS_CONTENT = floatPreferencesKey("dangerous_content")
        val RESPONSE_SCHEMA = stringPreferencesKey("response_schema")
        val GRAPHICAL_RESPONSE_SCHEMA = stringPreferencesKey("graphical_response_schema")
        val IS_STREAMING_ENABLED = booleanPreferencesKey("is_streaming_enabled") // 添加此键
    }

    override val settingsFlow = dataStore.data.map {
        val apiKey = it[PreferencesKeys.API_KEY] ?: ""
        val chatModel = it[PreferencesKeys.CHAT_MODEL] ?: ""
        val temperature = it[PreferencesKeys.TEMPERATURE] ?: 0.8f
        val topP = it[PreferencesKeys.TOP_P] ?: 0.95f
        val topK = it[PreferencesKeys.TOP_K]
        val useLocalStorage = it[PreferencesKeys.USE_LOCAL_STORAGE] ?: true
        val harassment = it[PreferencesKeys.HARASSMENT] ?: 0.0f
        val hateSpeech = it[PreferencesKeys.HATE_SPEECH] ?: 0.0f
        val sexuallyExplicit = it[PreferencesKeys.SEXUALLY_EXPLICIT] ?: 0.0f
        val dangerousContent = it[PreferencesKeys.DANGEROUS_CONTENT] ?: 0.0f
        val responseSchema = it[PreferencesKeys.RESPONSE_SCHEMA] ?: ""
        val graphicalResponseSchemaString = it[PreferencesKeys.GRAPHICAL_RESPONSE_SCHEMA]
        val graphicalResponseSchema = if (!graphicalResponseSchemaString.isNullOrBlank()) {
            try {
                Json.decodeFromString<List<JsonSchemaProperty>>(graphicalResponseSchemaString)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        val isStreamingEnabled = it[PreferencesKeys.IS_STREAMING_ENABLED] ?: false // 读取此值

        Settings(apiKey, chatModel, temperature, topP, topK, useLocalStorage, harassment, hateSpeech, sexuallyExplicit, dangerousContent, responseSchema, graphicalResponseSchema, isStreamingEnabled = isStreamingEnabled)
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

    override suspend fun updateTopK(topK: Int?) {
        if (topK != null) {
            dataStore.edit { it[PreferencesKeys.TOP_K] = topK }
        } else {
            dataStore.edit { it.remove(PreferencesKeys.TOP_K) }
        }
    }

    override suspend fun updateUseLocalStorage(useLocalStorage: Boolean) {
        dataStore.edit { it[PreferencesKeys.USE_LOCAL_STORAGE] = useLocalStorage }
    }

    override suspend fun updateHarassment(value: Float) {
        dataStore.edit { it[PreferencesKeys.HARASSMENT] = value }
    }

    override suspend fun updateHateSpeech(value: Float) {
        dataStore.edit { it[PreferencesKeys.HATE_SPEECH] = value }
    }

    override suspend fun updateSexuallyExplicit(value: Float) {
        dataStore.edit { it[PreferencesKeys.SEXUALLY_EXPLICIT] = value }
    }

    override suspend fun updateDangerousContent(value: Float) {
        dataStore.edit { it[PreferencesKeys.DANGEROUS_CONTENT] = value }
    }

    override suspend fun updateResponseSchema(responseSchema: String) {
        dataStore.edit { it[PreferencesKeys.RESPONSE_SCHEMA] = responseSchema }
    }

    override suspend fun updateGraphicalResponseSchema(schema: List<JsonSchemaProperty>) {
        dataStore.edit { it[PreferencesKeys.GRAPHICAL_RESPONSE_SCHEMA] = Json.encodeToString(schema) }
    }

    override suspend fun updateIsStreamingEnabled(isStreamingEnabled: Boolean) { // 实现此方法
        dataStore.edit { it[PreferencesKeys.IS_STREAMING_ENABLED] = isStreamingEnabled }
    }
}
