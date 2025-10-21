package com.exp.memoria.ui.settings

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exp.memoria.data.remote.api.ModelDetail
import com.exp.memoria.data.repository.LlmRepository
import com.exp.memoria.data.repository.MemoryRepository
import com.exp.memoria.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
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
 * `stateIn` 操作符将冷流 (Flow) 转换为热流 (StateFlow)，
 * 使其可以在多个订阅者之间共享，并在没有订阅者时在后台保持活动 5 秒。
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val llmRepository: LlmRepository, // 注入 LlmRepository
    private val memoryRepository: MemoryRepository, // 注入 MemoryRepository
    private val savedStateHandle: SavedStateHandle // 注入 SavedStateHandle
) : ViewModel() {

    val settingsState = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Settings()
    )

    // 从 SavedStateHandle 获取 conversationId
    private val _conversationId = MutableStateFlow<String?>(null)

    // 特定于对话的状态
    private val _systemInstruction = MutableStateFlow(SystemInstruction(parts = emptyList())) // Change type
    val systemInstruction = _systemInstruction.asStateFlow()

    private val _responseSchema = MutableStateFlow("")
    val responseSchema = _responseSchema.asStateFlow()

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

    init {
        // 从 savedStateHandle 获取 conversationId 并加载相关设置
        viewModelScope.launch {
            _conversationId.value = savedStateHandle.get<String>("conversationId")
            _conversationId.value?.let { loadConversationSettings(it) }
        }

        // 监听 _isGraphicalSchemaMode 变化，如果切换到图形模式，则重置 _draftProperty
        viewModelScope.launch {
            _isGraphicalSchemaMode.collect { isGraphicalMode ->
                if (isGraphicalMode) {
                    _draftProperty.value = JsonSchemaProperty(id = System.currentTimeMillis())
                }
            }
        }
    }

    /**
     * 根据 conversationId 加载特定于对话的设置。
     * @param conversationId 对话的唯一ID。
     */
    private fun loadConversationSettings(conversationId: String) {
        viewModelScope.launch {
            val header = memoryRepository.getConversationHeaderById(conversationId)
            if (header != null) {
                // Parse systemInstruction string to SystemInstruction object
                _systemInstruction.value = parseSystemInstructionJson(header.systemInstruction)
                _responseSchema.value = header.responseSchema ?: ""
                _graphicalSchemaProperties.value = parseJsonToGraphicalSchema(header.responseSchema)
            }
        }
    }

    /**
     * 辅助函数：将 JSON 字符串解析为 SystemInstruction 对象。
     * @param jsonString 待解析的 JSON 字符串。
     * @return 解析后的 SystemInstruction 对象，如果解析失败则返回包含空列表的 SystemInstruction。
     */
    private fun parseSystemInstructionJson(jsonString: String?): SystemInstruction {
        if (jsonString.isNullOrBlank()) return SystemInstruction(parts = emptyList())
        return try {
            Json.decodeFromString<SystemInstruction>(jsonString)
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "解析 System Instruction JSON 失败: ${e.message}")
            SystemInstruction(parts = emptyList())
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
     * 添加一个新的系统指令部分。
     * @param text 新指令部分的文本内容。
     */
    fun addSystemInstructionPart(text: String) {
        if (text.isBlank()) return
        val newPart = Part(text = text)
        val updatedParts = _systemInstruction.value.parts + newPart
        _systemInstruction.value = SystemInstruction(parts = updatedParts)
        updateSystemInstructionInRepository()
    }

    /**
     * 更新一个现有的系统指令部分。
     * @param index 要更新的指令部分的索引。
     * @param newText 新的文本内容。
     */
    fun updateSystemInstructionPart(index: Int, newText: String) {
        if (index < 0 || index >= _systemInstruction.value.parts.size) return
        if (newText.isBlank()) return // 不允许更新为空白
        val updatedParts = _systemInstruction.value.parts.toMutableList().apply {
            this[index] = this[index].copy(text = newText)
        }
        _systemInstruction.value = SystemInstruction(parts = updatedParts)
        updateSystemInstructionInRepository()
    }

    /**
     * 删除一个系统指令部分。
     * @param index 要删除的指令部分的索引。
     */
    fun removeSystemInstructionPart(index: Int) {
        if (index < 0 || index >= _systemInstruction.value.parts.size) return
        val updatedParts = _systemInstruction.value.parts.toMutableList().apply {
            removeAt(index)
        }
        _systemInstruction.value = SystemInstruction(parts = updatedParts)
        updateSystemInstructionInRepository()
    }

    /**
     * 辅助函数：将当前的系统指令保存到仓库。
     */
    private fun updateSystemInstructionInRepository() {
        viewModelScope.launch {
            savedStateHandle.get<String>("conversationId")?.let { conversationId ->
                val jsonString = Json.encodeToString(_systemInstruction.value)
                memoryRepository.updateSystemInstruction(conversationId, jsonString)
            }
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
     * 当停止序列输入框内容改变时调用。
     * @param stopSequences 新的停止序列字符串。
     */
    fun onStopSequencesChange(stopSequences: String) {
        viewModelScope.launch {
            settingsRepository.updateStopSequences(stopSequences)
        }
    }

    /**
     * 当 Response MIME Type 输入框内容改变时调用。
     * @param responseMimeType 新的 Response MIME Type。
     */
    fun onResponseMimeTypeChange(responseMimeType: String) {
        viewModelScope.launch {
            settingsRepository.updateResponseMimeType(responseMimeType)
        }
    }

    /**
     * 当 Response Logprobs 开关状态改变时调用。
     * @param responseLogprobs 新的布尔值。
     */
    fun onResponseLogprobsChange(responseLogprobs: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateResponseLogprobs(responseLogprobs)
        }
    }

    /**
     * 当频率惩罚值改变时调用。
     * @param frequencyPenalty 新的频率惩罚值。
     */
    fun onFrequencyPenaltyChange(frequencyPenalty: Float) {
        viewModelScope.launch {
            settingsRepository.updateFrequencyPenalty(frequencyPenalty)
        }
    }

    /**
     * 当存在惩罚值改变时调用。
     * @param presencePenalty 新的存在惩罚值。
     */
    fun onPresencePenaltyChange(presencePenalty: Float) {
        viewModelScope.launch {
            settingsRepository.updatePresencePenalty(presencePenalty)
        }
    }

    /**
     * 当候选数量值改变时调用。
     * @param candidateCount 新的候选数量值。
     */
    fun onCandidateCountChange(candidateCount: Int) {
        viewModelScope.launch {
            settingsRepository.updateCandidateCount(candidateCount)
        }
    }

    /**
     * 当随机种子值改变时调用。
     * @param seed 新的随机种子值。
     */
    fun onSeedChange(seed: Int?) {
        viewModelScope.launch {
            settingsRepository.updateSeed(seed)
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
        // 当用户在文本框中直接编辑时调用
        _responseSchema.value = responseSchema
        // 尝试将文本内容解析到图形化状态，以保持同步
        _graphicalSchemaProperties.value = parseJsonToGraphicalSchema(responseSchema)
        viewModelScope.launch {
            savedStateHandle.get<String>("conversationId")?.let { conversationId ->
                memoryRepository.updateResponseSchema(conversationId, responseSchema)
            }
        }
    }

    /**
     * 切换图形化 Response Schema 编辑模式。
     */
    fun onToggleGraphicalSchemaMode() {
        // 仅切换UI状态，数据的转换和保存在各自的编辑操作中完成
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
            val newList = _graphicalSchemaProperties.value + propertyToAdd.copy(id = System.currentTimeMillis())
            _graphicalSchemaProperties.value = newList
            // 根据更新后的图形化列表，转换并保存 Schema
            updateResponseSchemaFromGraphical()
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
        val newList = _graphicalSchemaProperties.value.map { if (it.id == updatedProperty.id) updatedProperty else it }
        _graphicalSchemaProperties.value = newList
        updateResponseSchemaFromGraphical()
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
        val newList = _graphicalSchemaProperties.value.filter { it.id != propertyId }
        _graphicalSchemaProperties.value = newList
        updateResponseSchemaFromGraphical()
    }

    /**
     * 辅助函数：从图形化属性列表生成JSON字符串并持久化。
     */
    private fun updateResponseSchemaFromGraphical() {
        val newSchema = convertGraphicalSchemaToJson(_graphicalSchemaProperties.value)
        _responseSchema.value = newSchema
        viewModelScope.launch {
            savedStateHandle.get<String>("conversationId")?.let { conversationId ->
                memoryRepository.updateResponseSchema(conversationId, newSchema)
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
                if (prop.name.isBlank()) return@associate "" to buildJsonObject {} // 跳过空属性名

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
        return Json { prettyPrint = true }.encodeToString(rootSchema)
    }

    /**
     * 辅助函数：将 JSON Schema 字符串解析为图形化编辑的属性列表。
     * @param jsonString 待解析的 JSON 字符串。
     * @return 解析后的属性列表，如果解析失败则返回空列表。
     */
    private fun parseJsonToGraphicalSchema(jsonString: String?): List<JsonSchemaProperty> {
        if (jsonString.isNullOrBlank()) return emptyList()
        return try {
            val root = Json.decodeFromString<JsonObject>(jsonString)
            // 确保根是 "object" 类型，并且有 "properties" 字段
            if (root["type"]?.jsonPrimitive?.content != "object" || !root.contains("properties")) {
                return emptyList()
            }
            val properties = root["properties"]?.jsonObject ?: return emptyList()

            properties.map { (name, element) ->
                val propObj = element.jsonObject
                val typeStr = propObj["type"]?.jsonPrimitive?.content?.uppercase()
                val type = JsonSchemaPropertyType.entries.find { it.name == typeStr } ?: JsonSchemaPropertyType.STRING
                JsonSchemaProperty(
                    id = System.currentTimeMillis() + name.hashCode(), // 生成一个临时的唯一ID
                    name = name,
                    type = type,
                    description = propObj["description"]?.jsonPrimitive?.content ?: "",
                    stringFormat = StringFormat.entries.find { it.name.equals(propObj["format"]?.jsonPrimitive?.content, ignoreCase = true) } ?: StringFormat.NONE,
                    numberMinimum = propObj["minimum"]?.jsonPrimitive?.doubleOrNull,
                    numberMaximum = propObj["maximum"]?.jsonPrimitive?.doubleOrNull
                )
            }
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "解析 Response Schema JSON 失败: ${e.message}")
            emptyList()
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