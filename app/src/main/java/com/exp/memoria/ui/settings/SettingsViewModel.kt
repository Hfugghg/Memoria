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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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
 * `stateIn` 操作符将冷流 (Flow) 转换为热流 (StateFlow)，
 * 使其可以在多个订阅者之间共享，并在没有订阅者时在后台保持活动 5 秒。
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

    // 控制是否显示图形化 Response Schema 编辑器
    private val _isGraphicalSchemaMode = MutableStateFlow(false)
    val isGraphicalSchemaMode = _isGraphicalSchemaMode.asStateFlow()

    // 存储图形化编辑的属性列表
    private val _graphicalSchemaProperties = MutableStateFlow<List<JsonSchemaProperty>>(emptyList())
    val graphicalSchemaProperties = _graphicalSchemaProperties.asStateFlow()

    // 用于编辑新的或现有的属性的草稿
    private val _draftProperty = MutableStateFlow(JsonSchemaProperty(id = System.currentTimeMillis()))
    val draftProperty = _draftProperty.asStateFlow()

    // 用于存储当前生效的 responseSchema JSON 字符串，无论是来自直接输入还是图形化生成
    private val _currentResponseSchemaString = MutableStateFlow("")
    val currentResponseSchemaString = _currentResponseSchemaString.asStateFlow()

    init {
        // 当 settingsState 或 graphicalSchemaProperties 变化时，更新 _currentResponseSchemaString
        viewModelScope.launch {
            combine(settingsState, graphicalSchemaProperties, _isGraphicalSchemaMode, _draftProperty) { settings, graphicalProperties, isGraphicalMode, draftProp ->
                if (isGraphicalMode) {
                    val combinedProperties = if (draftProp.name.isNotBlank()) {
                        // If draft property has a valid name, include it in the conversion
                        // 确保不重复添加已存在于列表中的属性 (虽然可能性小，但保险起见)
                        val exists = graphicalProperties.any { it.id == draftProp.id }
                        if (exists) graphicalProperties else graphicalProperties + draftProp
                    } else {
                        graphicalProperties
                    }
                    convertGraphicalSchemaToJson(combinedProperties)
                } else {
                    settings.responseSchema
                }
            }.collect {
                _currentResponseSchemaString.value = it
            }
        }

        // 初始化 graphicalSchemaProperties
        viewModelScope.launch {
            settingsState.map { it.graphicalResponseSchema }.collect {
                _graphicalSchemaProperties.value = it
            }
        }

        // 监听 _isGraphicalSchemaMode 变化，如果切换模式，则重置 _draftProperty
        viewModelScope.launch {
            _isGraphicalSchemaMode.collect { isGraphicalMode ->
                if (isGraphicalMode) {
                    _draftProperty.value = JsonSchemaProperty(id = System.currentTimeMillis())
                }
            }
        }
    }

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
     * 当 Top-K 值改变时调用。
     * @param topK 新的 Top-K 值。
     */
    fun onTopKChange(topK: Int?) {
        viewModelScope.launch {
            settingsRepository.updateTopK(topK)
        }
    }

    /**
     * 当最大输出 Token 值改变时调用。
     * @param maxOutputTokens 新的最大输出 Token 值。
     */
    fun onMaxOutputTokensChange(maxOutputTokens: Int?) {
        viewModelScope.launch {
            settingsRepository.updateMaxOutputTokens(maxOutputTokens)
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
     * 当“流式输出”开关状态改变时调用。
     * @param isStreamingEnabled 新的布尔值。
     */
    fun onIsStreamingEnabledChange(isStreamingEnabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateIsStreamingEnabled(isStreamingEnabled)
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
     * 当 responseSchema 输入框内容改变时调用。
     * @param responseSchema 新的 responseSchema 字符串。
     */
    fun onResponseSchemaChange(responseSchema: String) {
        viewModelScope.launch {
            settingsRepository.updateResponseSchema(responseSchema)
        }
    }

    /**
     * 切换图形化 Response Schema 编辑模式。
     */
    fun onToggleGraphicalSchemaMode() {
        // 在更新状态之前，将当前模式的数据保存
        viewModelScope.launch {
            if (_isGraphicalSchemaMode.value) {
                // 从图形化模式切换到 JSON 模式，将当前图形化数据生成的 JSON 存入 responseSchema
                settingsRepository.updateResponseSchema(_currentResponseSchemaString.value)
            } else {
                // 从 JSON 模式切换到图形化模式，将当前 JSON 字符串存入 graphicalResponseSchema (假设 SettingsRepository 有解析逻辑)
                // 注意: 这里应该实现 JSON 字符串到 JsonSchemaProperty 列表的解析逻辑，但为了简单，先保存当前图形化列表
                settingsRepository.updateGraphicalResponseSchema(_graphicalSchemaProperties.value)
            }
        }
        _isGraphicalSchemaMode.update { !it }
        // 模式切换后，重置草稿属性
        _draftProperty.value = JsonSchemaProperty(id = System.currentTimeMillis())
    }

    /**
     * 添加一个新的 JSON Schema 属性。
     * 从 _draftProperty 获取属性，添加到列表中，并重置 _draftProperty。
     */
    // SettingsViewModel.kt - addGraphicalSchemaProperty (已修复)
    fun addGraphicalSchemaProperty() {
        val propertyToAdd = _draftProperty.value
        // 确保属性名不为空且不在现有列表中（基于名称或 ID 检查）
        if (propertyToAdd.name.isNotBlank() && !_graphicalSchemaProperties.value.any { it.name == propertyToAdd.name }) {
            _graphicalSchemaProperties.update { currentList ->
                // 1. 计算新列表
                val newList = currentList + propertyToAdd.copy(id = System.currentTimeMillis())

                // 2. 在更新 StateFlow 之前，将新列表保存到仓库
                viewModelScope.launch {
                    settingsRepository.updateGraphicalResponseSchema(newList)
                }

                // 3. 返回新列表以更新 StateFlow 的值
                newList // update 块的返回值用于更新 StateFlow
            }
            _draftProperty.value = JsonSchemaProperty(id = System.currentTimeMillis()) // 重置草稿
        } else if (propertyToAdd.name.isNotBlank()) {
            Log.w("SettingsViewModel", "试图添加一个属性名已存在的属性: ${propertyToAdd.name}")
        } else {
            Log.w("SettingsViewModel", "试图添加一个属性名为空的属性。")
        }
    }

    /**
     * 更新一个现有的 JSON Schema 属性。
     * @param updatedProperty 需要更新的属性对象。
     */
    fun updateGraphicalSchemaProperty(updatedProperty: JsonSchemaProperty) {
        _graphicalSchemaProperties.update { currentProperties ->
            currentProperties.map {
                if (it.id == updatedProperty.id) updatedProperty else it
            }.also {
                viewModelScope.launch {
                    settingsRepository.updateGraphicalResponseSchema(it)
                }
            }
        }
    }

    /**
     * 更新草稿属性。
     * @param updatedProperty 需要更新的草稿属性对象。
     */
    fun onDraftPropertyChange(updatedProperty: JsonSchemaProperty) {
        _draftProperty.value = updatedProperty
    }

    /**
     * 删除一个 JSON Schema 属性。
     * @param propertyId 需要删除的属性的唯一ID。
     */
    fun removeGraphicalSchemaProperty(propertyId: Long) {
        _graphicalSchemaProperties.update { currentProperties ->
            currentProperties.filter { it.id != propertyId }.also {
                viewModelScope.launch {
                    settingsRepository.updateGraphicalResponseSchema(it)
                }
            }
        }
    }

    /**
     * 将图形化编辑的属性列表转换为 JSON Schema 字符串。
     * @param properties 待转换的属性列表。
     * @return 对应的 JSON Schema 字符串。
     * * 修复了 SerializationException: Serializer for class 'Any' is not found. 的问题
     */
    private fun convertGraphicalSchemaToJson(properties: List<JsonSchemaProperty>): String {
        if (properties.isEmpty()) {
            return ""
        }

        // 步骤 1: 构建每个属性的 JsonObject
        val propertiesMap = properties.associate { prop ->
            prop.name to buildJsonObject {
                // 确保属性名不为空
                if (prop.name.isBlank()) return@associate "" to buildJsonObject{} // 跳过空属性名

                put("type", prop.type.name.lowercase())
                if (prop.description.isNotBlank()) {
                    put("description", prop.description)
                }
                when (prop.type) {
                    JsonSchemaPropertyType.STRING -> {
                        if (prop.stringFormat != StringFormat.NONE) {
                            put("format", prop.stringFormat.name.lowercase())
                        }
                    }
                    JsonSchemaPropertyType.NUMBER -> {
                        // 使用 put(key, Double) 的重载
                        prop.numberMinimum?.let { put("minimum", it) }
                        prop.numberMaximum?.let { put("maximum", it) }
                    }
                    else -> {
                        // 对于 OBJECT 和 ARRAY 简化处理，暂时不添加嵌套属性
                    }
                }
            }
        }.filterKeys { it.isNotBlank() } // 过滤掉空属性名的键值对

        // 步骤 2: 构建根 JSON Schema 对象
        val rootSchema = buildJsonObject {
            put("type", "object")
            // 步骤 3: 嵌套 properties 字段
            put("properties", buildJsonObject {
                propertiesMap.forEach { (name, jsonObject) ->
                    // put(key, JsonElement) 的重载，将 JsonObject 放入父 JsonObject
                    put(name, jsonObject)
                }
            })
        }

        // 步骤 4: 序列化为字符串
        return Json.encodeToString(rootSchema)
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
