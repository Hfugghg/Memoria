package com.exp.memoria.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exp.memoria.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * [设置页面的 ViewModel]
 *
 * 职责:
 * 1. 作为设置界面 (SettingsScreen) 和数据仓库 (SettingsRepository) 之间的桥梁。
 * 2. 持有并管理设置界面的 UI 状态 (Settings)，使用 StateFlow 将其暴露给 UI。
 * 3. 响应用户的操作（例如修改 API 密钥、调整温度等），并调用 SettingsRepository 中相应的方法来持久化这些更改。
 *
 * @property settingsState 一个 StateFlow，它从 SettingsRepository 收集设置数据，并将其暴露给 UI。
 *                         `stateIn` 操作符将冷流 (Flow) 转换为热流 (StateFlow)，
 *                         使其可以在多个订阅者之间共享，并在没有订阅者时在后台保持活动 5 秒。
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settingsState = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Settings()
    )

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
}
