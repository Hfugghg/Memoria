package com.exp.memoria.ui.settings.settingsscreen

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.exp.memoria.ui.settings.Settings
import com.exp.memoria.ui.settings.SettingsViewModel

/**
 * ## 设置界面的主 Composable UI
 *
 * 这个 Composable 是设置功能的入口点，负责构建整个设置页面的结构。
 *
 * ### 主要职责:
 * 1.  **UI 骨架**: 使用 `Scaffold` 提供顶栏和内容区域的基本布局。
 * 2.  **状态管理**: 观察来自 `SettingsViewModel` 的 UI 状态 (`Settings`)，并将这些状态传递给子 Composable。
 * 3.  **用户交互**: 处理顶层的用户输入事件，例如文本框输入、开关切换，并将这些事件通知给 `SettingsViewModel`。
 * 4.  **组件组合**: 将各个独立的设置项（如 `GraphicalSchemaEditor`, `SafetySettingsSection` 等）组合在一起，形成完整的界面。
 * 5.  **对话框管理**: 管理和显示“API Key 缺失”和“模型选择”等对话框。
 *
 * @param viewModel `SettingsViewModel` 的实例，通过 Hilt 自动注入，用于提供数据和处理业务逻辑。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    // 从 ViewModel 中收集状态
    val settings by viewModel.settingsState.collectAsState()
    val showModelSelectionDialog by viewModel.showModelSelectionDialog.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()
    val isLoadingModels by viewModel.isLoadingModels.collectAsState()
    val showApiKeyError by viewModel.showApiKeyError.collectAsState()
    val isGraphicalSchemaMode by viewModel.isGraphicalSchemaMode.collectAsState()
    val graphicalSchemaProperties by viewModel.graphicalSchemaProperties.collectAsState()
    val currentResponseSchemaString by viewModel.currentResponseSchemaString.collectAsState()
    val draftProperty by viewModel.draftProperty.collectAsState() // 获取草稿属性

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
                value = settings.apiKey,
                onValueChange = viewModel::onApiKeyChange,
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth()
            )

            // 对话模型选择输入框
            OutlinedTextField(
                value = settings.chatModel,
                onValueChange = viewModel::onChatModelChange, // 允许用户手动输入模型名称
                label = { Text("对话模型") },
                readOnly = false, // 允许手动输入
                trailingIcon = { // 右侧的下拉箭头图标
                    IconButton(onClick = {
                        Log.d("SettingsScreen", "下拉箭头图标被点击。API Key是否为空: ${settings.apiKey.isBlank()}")
                        // 仅当 API Key 不为空时，才尝试获取模型列表并显示弹窗
                        if (settings.apiKey.isNotBlank()) {
                            viewModel.fetchAvailableModels(initialLoad = true)
                            viewModel.onShowModelSelectionDialog()
                            Log.d("SettingsScreen", "尝试获取模型并显示弹窗。")
                        } else {
                            viewModel.onShowApiKeyError()
                            Log.d("SettingsScreen", "API Key 为空，显示错误弹窗。")
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
                    onCheckedChange = { viewModel.onToggleGraphicalSchemaMode() }
                )
            }

            // 根据模式显示不同的编辑器
            if (isGraphicalSchemaMode) {
                GraphicalSchemaEditor(
                    properties = graphicalSchemaProperties,
                    draftProperty = draftProperty,
                    onDraftPropertyChange = viewModel::onDraftPropertyChange,
                    onAddProperty = viewModel::addGraphicalSchemaProperty,
                    onUpdateProperty = viewModel::updateGraphicalSchemaProperty,
                    onDeleteProperty = viewModel::removeGraphicalSchemaProperty
                )
            } else {
                OutlinedTextField(
                    value = settings.responseSchema,
                    onValueChange = viewModel::onResponseSchemaChange,
                    label = { Text("Response Schema (JSON)") },
                    placeholder = { Text("例如: {\"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}} }") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 显示当前生效的 Response Schema JSON 字符串
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = currentResponseSchemaString,
                onValueChange = { /* 只读 */ },
                label = { Text("实际生效的 Response Schema JSON") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
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
                    onCheckedChange = viewModel::onUseLocalStorageChange
                )
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
                    onCheckedChange = viewModel::onIsStreamingEnabledChange
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("LLM 高级设置", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            // 其他高级设置的占位符
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("系统指令", style = MaterialTheme.typography.titleMedium)
                    Text("// 尚未实现", style = MaterialTheme.typography.bodySmall)
                }
            }
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
                onHarassmentChange = viewModel::onHarassmentChange,
                hateSpeechValue = settings.hateSpeech,
                onHateSpeechChange = viewModel::onHateSpeechChange,
                sexuallyExplicitValue = settings.sexuallyExplicit,
                onSexuallyExplicitChange = viewModel::onSexuallyExplicitChange,
                dangerousContentValue = settings.dangerousContent,
                onDangerousContentChange = viewModel::onDangerousContentChange
            )

            // 生成配置
            GenerationConfigSection(
                settings = settings,
                onTemperatureChange = viewModel::onTemperatureChange,
                onTopPChange = viewModel::onTopPChange,
                onTopKChange = viewModel::onTopKChange,
                onMaxOutputTokensChange = viewModel::onMaxOutputTokensChange,
                onStopSequencesChange = viewModel::onStopSequencesChange,
                onFrequencyPenaltyChange = viewModel::onFrequencyPenaltyChange,
                onPresencePenaltyChange = viewModel::onPresencePenaltyChange,
                onCandidateCountChange = viewModel::onCandidateCountChange,
                onSeedChange = viewModel::onSeedChange,
                onResponseMimeTypeChange = viewModel::onResponseMimeTypeChange,
                onResponseLogprobsChange = viewModel::onResponseLogprobsChange
            )
        }
    }

    // API Key 错误弹窗
    if (showApiKeyError) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissApiKeyError,
            title = { Text("API Key 缺失") },
            text = { Text("请先设置您的 API Key 才能获取可用模型列表。") },
            confirmButton = {
                TextButton(onClick = viewModel::onDismissApiKeyError) {
                    Text("确定")
                }
            }
        )
    }

    // 模型选择弹窗
    if (showModelSelectionDialog) {
        Log.d("SettingsScreen", "模型选择对话框正在显示。")
        AlertDialog(
            onDismissRequest = viewModel::onDismissModelSelectionDialog,
            title = { Text("选择对话模型") },
            text = {
                Column {
                    // 根据加载状态和模型列表显示不同内容
                    if (isLoadingModels && availableModels.isEmpty()) {
                        Log.d("SettingsScreen", "模型正在加载且列表为空。")
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else if (availableModels.isEmpty()) {
                        Log.d("SettingsScreen", "模型加载完成但列表为空。")
                        Text("没有找到可用模型。请检查 API Key 和网络连接。")
                    } else {
                        Log.d("SettingsScreen", "模型列表已加载，数量: ${availableModels.size}")
                        val listState = rememberLazyListState()
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(availableModels) { model ->
                                // 仅显示支持 generateContent 方法的模型
                                if (model.supportedGenerationMethods.contains("generateContent")) {
                                    Column(modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.onChatModelChange(model.name.removePrefix("models/")) // 移除 "models/" 前缀
                                            viewModel.onDismissModelSelectionDialog()
                                            Log.d("SettingsScreen", "模型 ${model.displayName} 被选中。")
                                        }
                                        .padding(vertical = 8.dp)
                                    ) {
                                        Text(text = model.displayName, style = MaterialTheme.typography.titleMedium)
                                        Text(text = "输入Token限制: ${model.inputTokenLimit}", style = MaterialTheme.typography.bodySmall)
                                        Text(text = "输出Token限制: ${model.outputTokenLimit}", style = MaterialTheme.typography.bodySmall)
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }
                            // 加载更多的项目
                            item {
                                LaunchedEffect(listState.canScrollForward) {
                                    // 当滚动到底部、不在加载中且有下一页时，加载更多模型
                                    if (!listState.canScrollForward && !isLoadingModels && viewModel.nextPageToken.value != null) {
                                        Log.d("SettingsScreen", "滑动到底部，加载更多模型...")
                                        viewModel.fetchAvailableModels(initialLoad = false)
                                    }
                                }
                                if (isLoadingModels) {
                                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Log.d("SettingsScreen", "正在加载更多模型指示器。")
                                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::onDismissModelSelectionDialog) {
                    Text("关闭")
                }
            }
        )
    }
}

@Composable
fun GenerationConfigSection(
    settings: Settings,
    onTemperatureChange: (Float) -> Unit,
    onTopPChange: (Float) -> Unit,
    onTopKChange: (Int?) -> Unit,
    onMaxOutputTokensChange: (Int?) -> Unit,
    onStopSequencesChange: (String) -> Unit,
    onFrequencyPenaltyChange: (Float) -> Unit,
    onPresencePenaltyChange: (Float) -> Unit,
    onCandidateCountChange: (Int) -> Unit,
    onSeedChange: (Int?) -> Unit,
    onResponseMimeTypeChange: (String) -> Unit,
    onResponseLogprobsChange: (Boolean) -> Unit
) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("生成配置", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            // Temperature 输入框
            OutlinedTextField(
                value = settings.temperature.toString(),
                onValueChange = { newValue ->
                    val floatValue = newValue.toFloatOrNull()
                    if (floatValue != null) {
                        onTemperatureChange(floatValue.coerceIn(0f, 2f))
                    } else if (newValue.isBlank()) {
                        onTemperatureChange(0.0f)
                    }
                },
                label = { Text("Temperature (0.0 - 2.0)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Top P 输入框
            OutlinedTextField(
                value = settings.topP.toString(),
                onValueChange = { newValue ->
                    val floatValue = newValue.toFloatOrNull()
                    if (floatValue != null) {
                        onTopPChange(floatValue.coerceIn(0f, 1f))
                    } else if (newValue.isBlank()) {
                        onTopPChange(0.0f)
                    }
                },
                label = { Text("Top P (0.0 - 1.0)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Top K 输入框
            OutlinedTextField(
                value = settings.topK?.toString() ?: "",
                onValueChange = { newValue ->
                    if (newValue.isNotBlank()) {
                        val intValue = newValue.toIntOrNull()
                        if (intValue != null) {
                            onTopKChange(intValue.coerceAtLeast(1))
                        }
                    } else {
                        onTopKChange(null) // 允许清空
                    }
                },
                label = { Text("Top K (>= 1)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 最大输出 Token 输入框
            OutlinedTextField(
                value = settings.maxOutputTokens?.toString() ?: "",
                onValueChange = { newValue ->
                    if (newValue.isNotBlank()) {
                        val intValue = newValue.toIntOrNull()
                        if (intValue != null) {
                            onMaxOutputTokensChange(intValue.coerceAtLeast(1))
                        }
                    } else {
                        onMaxOutputTokensChange(null) // 允许清空
                    }
                },
                label = { Text("最大输出 Token (>= 1)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Stop Sequences 输入框
            OutlinedTextField(
                value = settings.stopSequences,
                onValueChange = onStopSequencesChange,
                label = { Text("停止序列") },
                placeholder = { Text("例如 stop,end，用逗号分隔") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Response Mime Type 输入框
            OutlinedTextField(
                value = settings.responseMimeType,
                onValueChange = onResponseMimeTypeChange,
                label = { Text("Response MIME Type") },
                placeholder = { Text("例如 application/json") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Frequency Penalty 输入框
            OutlinedTextField(
                value = settings.frequencyPenalty.toString(),
                onValueChange = { newValue ->
                    val floatValue = newValue.toFloatOrNull()
                    if (floatValue != null) {
                        onFrequencyPenaltyChange(floatValue.coerceIn(-2f, Math.nextDown(2.0f)))
                    } else if (newValue.isBlank()) {
                        onFrequencyPenaltyChange(0.0f)
                    }
                },
                label = { Text("频率惩罚 [-2.0, 2.0)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Presence Penalty 输入框
            OutlinedTextField(
                value = settings.presencePenalty.toString(),
                onValueChange = { newValue ->
                    val floatValue = newValue.toFloatOrNull()
                    if (floatValue != null) {
                        onPresencePenaltyChange(floatValue.coerceIn(-2f, Math.nextDown(2.0f)))
                    } else if (newValue.isBlank()) {
                        onPresencePenaltyChange(0.0f)
                    }
                },
                label = { Text("存在惩罚 [-2.0, 2.0)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Candidate Count 输入框
            OutlinedTextField(
                value = settings.candidateCount.toString(),
                onValueChange = { newValue ->
                    val intValue = newValue.toIntOrNull()
                    if (intValue != null) {
                        onCandidateCountChange(intValue.coerceIn(1, 8))
                    }
                },
                label = { Text("候选数量 (1 - 8)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Seed 输入框
            OutlinedTextField(
                value = settings.seed?.toString() ?: "",
                onValueChange = { newValue ->
                    if (newValue.isNotBlank()) {
                        val intValue = newValue.toIntOrNull()
                        if (intValue != null) {
                            onSeedChange(intValue)
                        }
                    } else {
                        onSeedChange(null) // 允许清空
                    }
                },
                label = { Text("随机种子 (整数)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Response Logprobs 开关
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("返回对数概率 (Logprobs)")
                Switch(
                    checked = settings.responseLogprobs,
                    onCheckedChange = onResponseLogprobsChange
                )
            }
        }
    }
}