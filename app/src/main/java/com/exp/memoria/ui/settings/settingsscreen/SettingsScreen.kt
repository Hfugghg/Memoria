package com.exp.memoria.ui.settings.settingsscreen

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.exp.memoria.ui.settings.ModelSelectionViewModel
import com.exp.memoria.ui.settings.ResponseSchemaViewModel
import com.exp.memoria.ui.settings.SettingsViewModel
import com.exp.memoria.ui.settings.SystemInstructionViewModel
import com.exp.memoria.utils.DatabaseUtils
import kotlinx.coroutines.launch

/**
 * 设置界面的主 Composable UI。
 *
 * 这是设置功能的入口点，负责构建整个设置页面的结构。它通过组合各种子 Composable
 *（如 [GraphicalSchemaEditor]、[SafetySettingsSection]、[GenerationConfigSection] 等）来形成完整的界面。
 *
 * ### 主要职责:
 * - **UI 骨架**: 使用 `Scaffold` 提供顶栏和内容区域的基本布局。
 * - **状态管理**: 观察并响应来自各个 ViewModel 的 UI 状态，并将这些状态传递给子 Composable。
 * - **用户交互**: 处理顶层的用户输入事件（如文本框输入、开关切换），并将这些事件分派给相应的 ViewModel。
 * - **对话框管理**: 管理和显示“API Key 缺失”和“模型选择”等对话框。
 *
 * @param settingsViewModel 提供通用设置数据和业务逻辑。
 * @param modelSelectionViewModel 处理模型选择相关的业务逻辑。
 * @param responseSchemaViewModel 处理响应模式相关的业务逻辑。
 * @param systemInstructionViewModel 处理系统指令相关的业务逻辑。
 *
 * @see SettingsViewModel
 * @see ModelSelectionViewModel
 * @see ResponseSchemaViewModel
 * @see SystemInstructionViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    modelSelectionViewModel: ModelSelectionViewModel = hiltViewModel(),
    responseSchemaViewModel: ResponseSchemaViewModel = hiltViewModel(),
    systemInstructionViewModel: SystemInstructionViewModel = hiltViewModel()
) {
    // 从 ViewModel 中收集状态
    val settings by settingsViewModel.settingsState.collectAsState()
    val systemInstruction by systemInstructionViewModel.systemInstruction.collectAsState() // Now it's SystemInstruction object
    val responseSchema by responseSchemaViewModel.responseSchema.collectAsState()
    val showModelSelectionDialog by modelSelectionViewModel.showModelSelectionDialog.collectAsState()
    val availableModels by modelSelectionViewModel.availableModels.collectAsState()
    val isLoadingModels by modelSelectionViewModel.isLoadingModels.collectAsState()
    val showApiKeyError by modelSelectionViewModel.showApiKeyError.collectAsState()
    val isGraphicalSchemaMode by responseSchemaViewModel.isGraphicalSchemaMode.collectAsState()
    val graphicalSchemaProperties by responseSchemaViewModel.graphicalSchemaProperties.collectAsState()
    val draftProperty by responseSchemaViewModel.draftProperty.collectAsState() // 获取草稿属性
    val context = LocalContext.current

    val scope = rememberCoroutineScope() // 添加协程作用域

    // API Key 输入框的本地状态
    var apiKeyInput by remember { mutableStateOf(settings.apiKey) }
    var apiKeyVisible by rememberSaveable { mutableStateOf(false) }

    // 当 ViewModel 中的 apiKey 变化时，同步更新本地状态
    LaunchedEffect(settings.apiKey) {
        if (apiKeyInput != settings.apiKey) {
            apiKeyInput = settings.apiKey
        }
    }

    // 页面整体脚手架
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("设置") })
        }
    ) { padding ->
        // 主内容区域
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()) // 使整个页面可滚动
                .padding(16.dp)
        ) {
            // API Key 输入框
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = {
                    apiKeyInput = it // 立即更新本地状态，解决粘贴问题
                    settingsViewModel.onApiKeyChange(it) // 通知 ViewModel 保存
                },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (apiKeyVisible)
                        Icons.Filled.Visibility
                    else
                        Icons.Filled.VisibilityOff
                    val description = if (apiKeyVisible) "隐藏密钥" else "显示密钥"

                    IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                        Icon(imageVector = image, contentDescription = description)
                    }
                }
            )

            // 对话模型选择输入框
            OutlinedTextField(
                value = settings.chatModel,
                onValueChange = settingsViewModel::onChatModelChange, // 允许用户手动输入模型名称，并由 settingsViewModel 处理
                label = { Text("对话模型") },
                readOnly = false, // 允许手动输入
                trailingIcon = {
                    IconButton(onClick = {
                        Log.d("SettingsScreen", "下拉箭头图标被点击。API Key是否为空: ${settings.apiKey.isBlank()}")
                        // 尝试获取模型。如果 API Key 为空，ViewModel 会处理错误状态。
                        modelSelectionViewModel.fetchAvailableModels(initialLoad = true)
                        // 仅当 API Key 不为空时，才显示模型选择弹窗
                        if (settings.apiKey.isNotBlank()) {
                            modelSelectionViewModel.onShowModelSelectionDialog()
                            Log.d("SettingsScreen", "API Key存在，尝试显示模型选择弹窗。")
                        } else {
                            Log.d("SettingsScreen", "API Key 为空，ViewModel将处理错误状态。")
                        }
                    }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "选择模型")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Response Schema 设置区域
            Text("Response Schema 配置", style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("图形化编辑模式")
                Switch(
                    checked = isGraphicalSchemaMode,
                    onCheckedChange = { responseSchemaViewModel.onToggleGraphicalSchemaMode() }
                )
            }

            // 根据模式显示不同的编辑器
            if (isGraphicalSchemaMode) {
                GraphicalSchemaEditor(
                    properties = graphicalSchemaProperties,
                    draftProperty = draftProperty,
                    onDraftPropertyChange = responseSchemaViewModel::onDraftPropertyChange,
                    onAddProperty = responseSchemaViewModel::addGraphicalSchemaProperty,
                    onUpdateProperty = responseSchemaViewModel::updateGraphicalSchemaProperty,
                    onDeleteProperty = responseSchemaViewModel::removeGraphicalSchemaProperty
                )
            } else {
                OutlinedTextField(
                    value = responseSchema, // 绑定到新的 ViewModel 状态
                    onValueChange = responseSchemaViewModel::onResponseSchemaChange,
                    label = { Text("Response Schema (JSON)") }, // 标签文本
                    placeholder = { Text("例如: {\"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}} }") }, // 占位符文本，JSON 字符串中的双引号已转义
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5
                )
            }

            Text(
                text = "提示: 如果 Response Schema 为空，response_mime_type 将为 text/plain；否则为 application/json。",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
            )


            // 本地存储设置
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("本地存储")
                Checkbox(
                    checked = settings.useLocalStorage,
                    onCheckedChange = settingsViewModel::onUseLocalStorageChange
                )
            }

            // 导出数据库按钮
            Button(
                onClick = {
                    exportDatabase(context)
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("导出数据库")
            }

            // 流式输出开关
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("流式输出")
                Switch(
                    checked = settings.isStreamingEnabled,
                    onCheckedChange = settingsViewModel::onIsStreamingEnabledChange
                )
            }

            // 调试模式开关
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("禁用摘要/嵌入", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "调试时可开启，以节省配额",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = settings.disableSummaryAndEmbedding,
                    onCheckedChange = settingsViewModel::onDisableSummaryAndEmbeddingChange
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("LLM 高级设置", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            // 系统指令部分
            SystemInstructionSection(
                systemInstruction = systemInstruction,
                systemInstructionViewModel = systemInstructionViewModel
            )

            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("工具", style = MaterialTheme.typography.titleMedium)
                    Text("// 尚未实现", style = MaterialTheme.typography.bodySmall)
                }
            }
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("工具配置", style = MaterialTheme.typography.titleMedium)
                    Text("// 尚未实现", style = MaterialTheme.typography.bodySmall)
                }
            }

            // 安全设置区域
            SafetySettingsSection(
                harassmentValue = settings.harassment,
                onHarassmentChange = { value -> scope.launch { settingsViewModel.onHarassmentChange(value) } },
                hateSpeechValue = settings.hateSpeech,
                onHateSpeechChange = { value -> scope.launch { settingsViewModel.onHateSpeechChange(value) } },
                sexuallyExplicitValue = settings.sexuallyExplicit,
                onSexuallyExplicitChange = { value -> scope.launch { settingsViewModel.onSexuallyExplicitChange(value) } },
                dangerousContentValue = settings.dangerousContent,
                onDangerousContentChange = { value -> scope.launch { settingsViewModel.onDangerousContentChange(value) } }
            )

            // 生成配置
            GenerationConfigSection(
                settings = settings,
                onTemperatureChange = { value -> scope.launch { settingsViewModel.onTemperatureChange(value) } },
                onTopPChange = { value -> scope.launch { settingsViewModel.onTopPChange(value) } },
                onTopKChange = { value -> scope.launch { settingsViewModel.onTopKChange(value) } },
                onMaxOutputTokensChange = { value -> scope.launch { settingsViewModel.onMaxOutputTokensChange(value) } },
                onStopSequencesChange = { value -> scope.launch { settingsViewModel.onStopSequencesChange(value) } },
                onFrequencyPenaltyChange = { value -> scope.launch { settingsViewModel.onFrequencyPenaltyChange(value) } },
                onPresencePenaltyChange = { value -> scope.launch { settingsViewModel.onPresencePenaltyChange(value) } },
                onCandidateCountChange = { value -> scope.launch { settingsViewModel.onCandidateCountChange(value) } },
                onSeedChange = { value -> scope.launch { settingsViewModel.onSeedChange(value) } },
                onResponseMimeTypeChange = { value -> scope.launch { settingsViewModel.onResponseMimeTypeChange(value) } },
                onResponseLogprobsChange = { value -> scope.launch { settingsViewModel.onResponseLogprobsChange(value) } },
                onOutputDimensionalityChange = { value ->
                    scope.launch {
                        settingsViewModel.onOutputDimensionalityChange(
                            value
                        )
                    }
                }
            )
        }
    }

    // API Key 错误弹窗
    if (showApiKeyError) {
        AlertDialog(
            onDismissRequest = modelSelectionViewModel::onDismissApiKeyError,
            title = { Text("API Key 缺失") },
            text = { Text("请先设置您的 API Key 才能获取可用模型列表。") },
            confirmButton = {
                TextButton(onClick = modelSelectionViewModel::onDismissApiKeyError) {
                    Text("确定")
                }
            }
        )
    }

    // 模型选择弹窗
    ModelSelectionDialog(
        showModelSelectionDialog = showModelSelectionDialog,
        onDismissModelSelectionDialog = modelSelectionViewModel::onDismissModelSelectionDialog,
        isLoadingModels = isLoadingModels,
        availableModels = availableModels,
        onChatModelChange = settingsViewModel::onChatModelChange,
        fetchAvailableModels = modelSelectionViewModel::fetchAvailableModels,
        nextPageToken = modelSelectionViewModel.nextPageToken.collectAsState()
    )
}

private fun exportDatabase(context: Context) {
    val success = DatabaseUtils.exportDatabase(context)
    val message = if (success) "数据库导出成功！" else "数据库导出失败。"
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
