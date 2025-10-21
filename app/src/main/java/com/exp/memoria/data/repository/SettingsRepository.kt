package com.exp.memoria.data.repository

import com.exp.memoria.ui.settings.Settings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settingsFlow: Flow<Settings>
    suspend fun updateApiKey(apiKey: String)
    suspend fun updateChatModel(chatModel: String)
    suspend fun updateTemperature(temperature: Float)
    suspend fun updateTopP(topP: Float)
    suspend fun updateTopK(topK: Int?)
    suspend fun updateMaxOutputTokens(maxOutputTokens: Int?)
    suspend fun updateStopSequences(stopSequences: String)
    suspend fun updateResponseMimeType(responseMimeType: String)
    suspend fun updateResponseLogprobs(responseLogprobs: Boolean)
    suspend fun updateFrequencyPenalty(frequencyPenalty: Float)
    suspend fun updatePresencePenalty(presencePenalty: Float)
    suspend fun updateCandidateCount(candidateCount: Int)
    suspend fun updateSeed(seed: Int?)
    suspend fun updateUseLocalStorage(useLocalStorage: Boolean)
    suspend fun updateHarassment(value: Float)
    suspend fun updateHateSpeech(value: Float)
    suspend fun updateSexuallyExplicit(value: Float)
    suspend fun updateDangerousContent(value: Float)
    suspend fun updateIsStreamingEnabled(isStreamingEnabled: Boolean)
}
