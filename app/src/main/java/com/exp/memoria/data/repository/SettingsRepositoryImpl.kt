package com.exp.memoria.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.exp.memoria.ui.settings.Settings
import kotlinx.coroutines.flow.map
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
        val MAX_OUTPUT_TOKENS = intPreferencesKey("max_output_tokens")
        val STOP_SEQUENCES = stringPreferencesKey("stop_sequences")
        val RESPONSE_MIME_TYPE = stringPreferencesKey("response_mime_type")
        val RESPONSE_LOGPROBS = booleanPreferencesKey("response_logprobs")
        val FREQUENCY_PENALTY = floatPreferencesKey("frequency_penalty")
        val PRESENCE_PENALTY = floatPreferencesKey("presence_penalty")
        val CANDIDATE_COUNT = intPreferencesKey("candidate_count")
        val SEED = intPreferencesKey("seed")
        val USE_LOCAL_STORAGE = booleanPreferencesKey("use_local_storage")
        val HARASSMENT = floatPreferencesKey("harassment")
        val HATE_SPEECH = floatPreferencesKey("hate_speech")
        val SEXUALLY_EXPLICIT = floatPreferencesKey("sexually_explicit")
        val DANGEROUS_CONTENT = floatPreferencesKey("dangerous_content")
        val IS_STREAMING_ENABLED = booleanPreferencesKey("is_streaming_enabled")
    }

    override val settingsFlow = dataStore.data.map {
        val apiKey = it[PreferencesKeys.API_KEY] ?: ""
        val chatModel = it[PreferencesKeys.CHAT_MODEL] ?: ""
        val temperature = it[PreferencesKeys.TEMPERATURE] ?: 0.8f
        val topP = it[PreferencesKeys.TOP_P] ?: 0.95f
        val topK = it[PreferencesKeys.TOP_K]
        val maxOutputTokens = it[PreferencesKeys.MAX_OUTPUT_TOKENS]
        val stopSequences = it[PreferencesKeys.STOP_SEQUENCES] ?: ""
        val responseMimeType = it[PreferencesKeys.RESPONSE_MIME_TYPE] ?: ""
        val responseLogprobs = it[PreferencesKeys.RESPONSE_LOGPROBS] ?: false
        val frequencyPenalty = it[PreferencesKeys.FREQUENCY_PENALTY] ?: 0.0f
        val presencePenalty = it[PreferencesKeys.PRESENCE_PENALTY] ?: 0.0f
        val candidateCount = it[PreferencesKeys.CANDIDATE_COUNT] ?: 1
        val seed = it[PreferencesKeys.SEED]
        val useLocalStorage = it[PreferencesKeys.USE_LOCAL_STORAGE] ?: true
        val harassment = it[PreferencesKeys.HARASSMENT] ?: 0.0f
        val hateSpeech = it[PreferencesKeys.HATE_SPEECH] ?: 0.0f
        val sexuallyExplicit = it[PreferencesKeys.SEXUALLY_EXPLICIT] ?: 0.0f
        val dangerousContent = it[PreferencesKeys.DANGEROUS_CONTENT] ?: 0.0f
        val isStreamingEnabled = it[PreferencesKeys.IS_STREAMING_ENABLED] ?: false

        Settings(
            apiKey = apiKey, 
            chatModel = chatModel, 
            temperature = temperature, 
            topP = topP, 
            topK = topK, 
            maxOutputTokens = maxOutputTokens,
            stopSequences = stopSequences,
            responseMimeType = responseMimeType,
            responseLogprobs = responseLogprobs,
            frequencyPenalty = frequencyPenalty,
            presencePenalty = presencePenalty,
            candidateCount = candidateCount,
            seed = seed,
            useLocalStorage = useLocalStorage, 
            harassment = harassment, 
            hateSpeech = hateSpeech, 
            sexuallyExplicit = sexuallyExplicit, 
            dangerousContent = dangerousContent, 
            isStreamingEnabled = isStreamingEnabled
        )
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

    override suspend fun updateMaxOutputTokens(maxOutputTokens: Int?) {
        if (maxOutputTokens != null) {
            dataStore.edit { it[PreferencesKeys.MAX_OUTPUT_TOKENS] = maxOutputTokens }
        } else {
            dataStore.edit { it.remove(PreferencesKeys.MAX_OUTPUT_TOKENS) }
        }
    }

    override suspend fun updateStopSequences(stopSequences: String) {
        dataStore.edit { it[PreferencesKeys.STOP_SEQUENCES] = stopSequences }
    }

    override suspend fun updateResponseMimeType(responseMimeType: String) {
        dataStore.edit { it[PreferencesKeys.RESPONSE_MIME_TYPE] = responseMimeType }
    }

    override suspend fun updateResponseLogprobs(responseLogprobs: Boolean) {
        dataStore.edit { it[PreferencesKeys.RESPONSE_LOGPROBS] = responseLogprobs }
    }

    override suspend fun updateFrequencyPenalty(frequencyPenalty: Float) {
        dataStore.edit { it[PreferencesKeys.FREQUENCY_PENALTY] = frequencyPenalty }
    }

    override suspend fun updatePresencePenalty(presencePenalty: Float) {
        dataStore.edit { it[PreferencesKeys.PRESENCE_PENALTY] = presencePenalty }
    }

    override suspend fun updateCandidateCount(candidateCount: Int) {
        dataStore.edit { it[PreferencesKeys.CANDIDATE_COUNT] = candidateCount }
    }

    override suspend fun updateSeed(seed: Int?) {
        if (seed != null) {
            dataStore.edit { it[PreferencesKeys.SEED] = seed }
        } else {
            dataStore.edit { it.remove(PreferencesKeys.SEED) }
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

    override suspend fun updateIsStreamingEnabled(isStreamingEnabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.IS_STREAMING_ENABLED] = isStreamingEnabled }
    }
}