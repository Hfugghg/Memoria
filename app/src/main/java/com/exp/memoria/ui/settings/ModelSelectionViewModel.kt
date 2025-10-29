package com.exp.memoria.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exp.memoria.data.remote.api.ModelDetail
import com.exp.memoria.data.repository.LlmRepository
import com.exp.memoria.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelSelectionViewModel @Inject constructor(
    private val llmRepository: LlmRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _showModelSelectionDialog = MutableStateFlow(false)
    val showModelSelectionDialog = _showModelSelectionDialog.asStateFlow()

    private val _availableModels = MutableStateFlow<List<ModelDetail>>(emptyList())
    val availableModels = _availableModels.asStateFlow()

    private val _isLoadingModels = MutableStateFlow(false)
    val isLoadingModels = _isLoadingModels.asStateFlow()

    private val _nextPageToken = MutableStateFlow<String?>(null)
    val nextPageToken = _nextPageToken.asStateFlow()

    private val _showApiKeyError = MutableStateFlow(false)
    val showApiKeyError = _showApiKeyError.asStateFlow()

    fun onShowModelSelectionDialog() {
        _showModelSelectionDialog.value = true
    }

    fun onDismissModelSelectionDialog() {
        _showModelSelectionDialog.value = false
    }

    fun fetchAvailableModels(initialLoad: Boolean = true) {
        viewModelScope.launch {
            val apiKey = settingsRepository.settingsFlow.first().apiKey
            if (apiKey.isBlank()) {
                _showApiKeyError.value = true
                return@launch
            }

            if (_isLoadingModels.value) return@launch

            _isLoadingModels.value = true
            try {
                val currentNextPageToken = if (initialLoad) null else _nextPageToken.value
                val (models, newNextPageToken) = llmRepository.getAvailableModels(
                    apiKey, currentNextPageToken
                )

                val filteredModels = models.filter { it.supportedGenerationMethods.contains("generateContent") }

                if (initialLoad) {
                    _availableModels.value = filteredModels
                } else {
                    _availableModels.update { it + filteredModels }
                }
                _nextPageToken.value = newNextPageToken
                Log.d("ModelSelectionViewModel", "Fetched ${filteredModels.size} models, next page token: $newNextPageToken")
            } catch (e: Exception) {
                Log.e("ModelSelectionViewModel", "Failed to fetch available models: ${e.message}", e)
            } finally {
                _isLoadingModels.value = false
            }
        }
    }

    fun onDismissApiKeyError() {
        _showApiKeyError.value = false
    }
}
