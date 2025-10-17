package com.exp.memoria.ui.settings

import android.util.Log // 导入 Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exp.memoria.data.remote.api.ModelDetail // 导入 ModelDetail
import com.exp.memoria.data.repository.LlmRepository // 导入 LlmRepository
import com.exp.memoria.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * [设置页面的 ViewModel]
 *
 * 职责:
 * 1. 作为设置界面 (SettingsScreen) 和数据仓库 (SettingsRepository) 之间的桥梁。
 * 2. 持有并管理设置界面的 UI 状态 (Settings)，使用 StateFlow 将其暴露给 UI。
 * 3. 响应用户的操作（例如修改 API 密钥、调整温度等），并调用 SettingsRepository 中相应的方法来持久化这些更改。
 * 4. 负责从 LlmRepository 获取可用模型列表，并管理模型选择对话框的状态和分页加载。
 *
 * @property settingsState 一个 StateFlow，它从 SettingsRepository 收集设置数据，并将其暴露给 UI。
 *                         `stateIn` 操作符将冷流 (Flow) 转换为热流 (StateFlow)，
 *                         使其可以在多个订阅者之间共享，并在没有订阅者时在后台保持活动 5 秒。
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val llmRepository: LlmRepository // 注入 LlmRepository
) : ViewModel() {

    val settingsState = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Settings()
    )

    // 控制 API Key 缺失错误弹窗的显示状态
    private val _showApiKeyError = MutableStateFlow(false)
    val showApiKeyError = _showApiKeyError.asStateFlow()

    // 控制模型选择对话框的显示状态
    private val _showModelSelectionDialog = MutableStateFlow(false)
    val showModelSelectionDialog = _showModelSelectionDialog.asStateFlow()

    // 存储可用的模型列表
    private val _availableModels = MutableStateFlow<List<ModelDetail>>(emptyList())
    val availableModels = _availableModels.asStateFlow()

    // 存储模型加载状态
    private val _isLoadingModels = MutableStateFlow(false)
    val isLoadingModels = _isLoadingModels.asStateFlow()

    // 存储下一页的模型列表 token
    private val _nextPageToken = MutableStateFlow<String?>(null)
    val nextPageToken = _nextPageToken.asStateFlow()

    /**
     * 当 API 密钥输入框内容改变时调用。
     * @param apiKey 新的 API 密钥。
     */
    fun onApiKeyChange(apiKey: String) {
        viewModelScope.launch {
            settingsRepository.updateApiKey(apiKey)
        }
    }

    /**
     * 当聊天模型改变时调用。
     * @param chatModel 新的聊天模型名称。
     */
    fun onChatModelChange(chatModel: String) {
        viewModelScope.launch {
            settingsRepository.updateChatModel(chatModel)
        }
    }

    /**
     * 当温度滑块值改变时调用。
     * @param temperature 新的温度值。
     */
    fun onTemperatureChange(temperature: Float) {
        viewModelScope.launch {
            settingsRepository.updateTemperature(temperature)
        }
    }

    /**
     * 当 Top-P 滑块值改变时调用。
     * @param topP 新的 Top-P 值。
     */
    fun onTopPChange(topP: Float) {
        viewModelScope.launch {
            settingsRepository.updateTopP(topP)
        }
    }

    /**
     * 当“使用本地存储”开关状态改变时调用。
     * @param useLocalStorage 新的布尔值。
     */
    fun onUseLocalStorageChange(useLocalStorage: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateUseLocalStorage(useLocalStorage)
        }
    }

    /**
     * 当“骚扰”安全等级滑块值改变时调用。
     * @param value 新的阈值。
     */
    fun onHarassmentChange(value: Float) {
        viewModelScope.launch {
            settingsRepository.updateHarassment(value)
        }
    }

    /**
     * 当“仇恨言论”安全等级滑块值改变时调用。
     * @param value 新的阈值。
     */
    fun onHateSpeechChange(value: Float) {
        viewModelScope.launch {
            settingsRepository.updateHateSpeech(value)
        }
    }

    /**
     * 当“色情内容”安全等级滑块值改变时调用。
     * @param value 新的阈值。
     */
    fun onSexuallyExplicitChange(value: Float) {
        viewModelScope.launch {
            settingsRepository.updateSexuallyExplicit(value)
        }
    }

    /**
     * 当“危险内容”安全等级滑块值改变时调用。
     * @param value 新的阈值。
     */
    fun onDangerousContentChange(value: Float) {
        viewModelScope.launch {
            settingsRepository.updateDangerousContent(value)
        }
    }

    /**
     * 显示 API Key 错误弹窗。
     */
    fun onShowApiKeyError() {
        _showApiKeyError.value = true
    }

    /**
     * 关闭 API Key 错误弹窗。
     */
    fun onDismissApiKeyError() {
        _showApiKeyError.value = false
    }

    /**
     * 显示模型选择对话框。
     */
    fun onShowModelSelectionDialog() {
        _showModelSelectionDialog.value = true
    }

    /**
     * 关闭模型选择对话框。
     */
    fun onDismissModelSelectionDialog() {
        _showModelSelectionDialog.value = false
    }

    /**
     * 获取可用的 LLM 模型列表。
     * @param initialLoad 如果为 true，则清空现有列表并从第一页开始加载；否则，加载下一页。
     */
    fun fetchAvailableModels(initialLoad: Boolean = true) {
        viewModelScope.launch {
            // 如果 API Key 为空，则显示错误并退出
            if (settingsState.value.apiKey.isBlank()) {
                onShowApiKeyError()
                return@launch
            }

            // 如果正在加载中，则直接返回，避免重复请求
            if (_isLoadingModels.value) return@launch

            _isLoadingModels.value = true
            try {
                val currentNextPageToken = if (initialLoad) null else _nextPageToken.value
                val (models, newNextPageToken) = llmRepository.getAvailableModels(
                    settingsState.value.apiKey, currentNextPageToken
                )

                // 过滤只支持 generateContent 的模型
                val filteredModels = models.filter { it.supportedGenerationMethods.contains("generateContent") }

                if (initialLoad) {
                    _availableModels.value = filteredModels
                } else {
                    // 追加到现有列表
                    _availableModels.update { it + filteredModels }
                }
                _nextPageToken.value = newNextPageToken
                Log.d("SettingsViewModel", "获取到模型数量: ${filteredModels.size}, 下一页token: $newNextPageToken")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "获取可用模型失败: ${e.message}", e)
                // 可以在这里添加更详细的错误处理，例如显示 SnackBar
            } finally {
                _isLoadingModels.value = false
            }
        }
    }
}
