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

    fun onTemperatureChange(temperature: Float) {
        viewModelScope.launch {
            settingsRepository.updateTemperature(temperature)
        }
    }

    fun onTopPChange(topP: Float) {
        viewModelScope.launch {
            settingsRepository.updateTopP(topP)
        }
    }

    fun onUseLocalStorageChange(useLocalStorage: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateUseLocalStorage(useLocalStorage)
        }
    }

    fun onHarassmentChange(value: Float) {
        viewModelScope.launch {
            settingsRepository.updateHarassment(value)
        }
    }

    fun onHateSpeechChange(value: Float) {
        viewModelScope.launch {
            settingsRepository.updateHateSpeech(value)
        }
    }

    fun onSexuallyExplicitChange(value: Float) {
        viewModelScope.launch {
            settingsRepository.updateSexuallyExplicit(value)
        }
    }

    fun onDangerousContentChange(value: Float) {
        viewModelScope.launch {
            settingsRepository.updateDangerousContent(value)
        }
    }
}