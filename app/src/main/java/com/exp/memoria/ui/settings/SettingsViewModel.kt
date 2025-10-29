package com.exp.memoria.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exp.memoria.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settingsState = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Settings()
    )

    fun onApiKeyChange(apiKey: String) {
        viewModelScope.launch {
            settingsRepository.updateApiKey(apiKey)
        }
    }

    fun onChatModelChange(chatModel: String) {
        viewModelScope.launch {
            settingsRepository.updateChatModel(chatModel)
        }
    }

    suspend fun onTemperatureChange(temperature: Float) {
        settingsRepository.updateTemperature(temperature)
    }

    suspend fun onTopPChange(topP: Float) {
        settingsRepository.updateTopP(topP)
    }

    suspend fun onTopKChange(topK: Int?) {
        settingsRepository.updateTopK(topK)
    }

    suspend fun onMaxOutputTokensChange(maxOutputTokens: Int?) {
        settingsRepository.updateMaxOutputTokens(maxOutputTokens)
    }

    suspend fun onStopSequencesChange(stopSequences: String) {
        settingsRepository.updateStopSequences(stopSequences)
    }

    suspend fun onResponseMimeTypeChange(responseMimeType: String) {
        settingsRepository.updateResponseMimeType(responseMimeType)
    }

    suspend fun onResponseLogprobsChange(responseLogprobs: Boolean) {
        settingsRepository.updateResponseLogprobs(responseLogprobs)
    }

    suspend fun onFrequencyPenaltyChange(frequencyPenalty: Float) {
        settingsRepository.updateFrequencyPenalty(frequencyPenalty)
    }

    suspend fun onPresencePenaltyChange(presencePenalty: Float) {
        settingsRepository.updatePresencePenalty(presencePenalty)
    }

    suspend fun onCandidateCountChange(candidateCount: Int) {
        settingsRepository.updateCandidateCount(candidateCount)
    }

    suspend fun onSeedChange(seed: Int?) {
        settingsRepository.updateSeed(seed)
    }

    fun onUseLocalStorageChange(useLocalStorage: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateUseLocalStorage(useLocalStorage)
        }
    }

    fun onIsStreamingEnabledChange(isStreamingEnabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateIsStreamingEnabled(isStreamingEnabled)
        }
    }

    fun onDisableSummaryAndEmbeddingChange(disableSummaryAndEmbedding: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDisableSummaryAndEmbedding(disableSummaryAndEmbedding)
        }
    }

    suspend fun onHarassmentChange(value: Float) {
        settingsRepository.updateHarassment(value)
    }

    suspend fun onHateSpeechChange(value: Float) {
        settingsRepository.updateHateSpeech(value)
    }

    suspend fun onSexuallyExplicitChange(value: Float) {
        settingsRepository.updateSexuallyExplicit(value)
    }

    suspend fun onDangerousContentChange(value: Float) {
        settingsRepository.updateDangerousContent(value)
    }

    suspend fun onOutputDimensionalityChange(outputDimensionality: Int?) {
        settingsRepository.updateOutputDimensionality(outputDimensionality)
    }
}
