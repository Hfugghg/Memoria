package com.exp.memoria.ui.settings.settingsscreen

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.exp.memoria.ui.settings.SettingsViewModel
import com.exp.memoria.utils.DatabaseUtils

/**
 * ## 设置界面的主 Composable UI
 *
 * 这个 Composable 是设置功能的入口点，负责构建整个设置页面的结构。
 *
 * ### 主要职责:
 * 1.  **UI 骨架**: 使用 `Scaffold` 提供顶栏和内容区域的基本布局。
 * 2.  **状态管理**: 观察来自 `SettingsViewModel` 的 UI 状态 (`Settings`)，并将这些状态传递给子 Composable。
 * 3.  **用户交互**: 处理顶层的用户输入事件，例如文本框输入、开关切换，并将这些事件通知给 `SettingsViewModel`。
 * 4.  **组件组合**: 将各个独立的设置项（如 `GraphicalSchemaEditor`, `SafetySettingsSection`, `GenerationConfigSection` 等）组合在一起，形成完整的界面。
 * 5.  **对话框管理**: 管理和显示“API Key 缺失”和“模型选择”等对话框。
 *
 * @param viewModel `SettingsViewModel` 的实例，通过 Hilt 自动注入，用于提供数据和处理业务逻辑。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    // 从 ViewModel 中收集状态
    val settings by viewModel.settingsState.collectAsState()
    val systemInstruction by viewModel.systemInstruction.collectAsState() // Now it's SystemInstruction object
    val responseSchema by viewModel.responseSchema.collectAsState()
    val showModelSelectionDialog by viewModel.showModelSelectionDialog.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()
    val isLoadingModels by viewModel.isLoadingModels.collectAsState()
    val showApiKeyError by viewModel.showApiKeyError.collectAsState()
    val isGraphicalSchemaMode by viewModel.isGraphicalSchemaMode.collectAsState()
    val graphicalSchemaProperties by viewModel.graphicalSchemaProperties.collectAsState()
    val draftProperty by viewModel.draftProperty.collectAsState() // 获取草稿属性
    val context = LocalContext.current

    // State for managing system instruction dialogs
    var showAddInstructionDialog by remember { mutableStateOf(false) }
    var showEditInstructionDialog by remember { mutableStateOf(false) }
    var editingInstructionIndex by remember { mutableStateOf<Int?>(null) }
    var draftInstructionText by remember { mutableStateOf("") }

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
                    viewModel.onApiKeyChange(it) // 通知 ViewModel 保存
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
                    value = responseSchema, // 绑定到新的 ViewModel 状态
                    onValueChange = viewModel::onResponseSchemaChange,
                    label = { Text("Response Schema (JSON)") },
                    placeholder = { Text("例如: {\"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}} }") },
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
                    onCheckedChange = viewModel::onUseLocalStorageChange
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
                    onCheckedChange = viewModel::onIsStreamingEnabledChange
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
                    onCheckedChange = viewModel::onDisableSummaryAndEmbeddingChange
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("LLM 高级设置", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            // System Instruction Section
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text("系统指令", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                if (systemInstruction.parts.isEmpty()) {
                    Text("当前没有系统指令。点击下方按钮添加。", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp) // Limit height to avoid excessive scrolling
                    ) {
                        items(systemInstruction.parts.size) { index ->
                            val part = systemInstruction.parts[index]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = part.text,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(onClick = {
                                        editingInstructionIndex = index
                                        draftInstructionText = part.text
                                        showEditInstructionDialog = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "编辑指令")
                                    }
                                    IconButton(onClick = {
                                        viewModel.removeSystemInstructionPart(index)
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "删除指令")
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        draftInstructionText = "" // Clear draft for new instruction
                        showAddInstructionDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加指令")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("添加系统指令")
                }
            }

            // Add Instruction Dialog
            if (showAddInstructionDialog) {
                AlertDialog(
                    onDismissRequest = { showAddInstructionDialog = false },
                    title = { Text("添加系统指令") },
                    text = {
                        OutlinedTextField(
                            value = draftInstructionText,
                            onValueChange = { draftInstructionText = it },
                            label = { Text("指令内容") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (draftInstructionText.isNotBlank()) {
                                    viewModel.addSystemInstructionPart(draftInstructionText)
                                    showAddInstructionDialog = false
                                } else {
                                    Toast.makeText(context, "指令内容不能为空", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("添加")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddInstructionDialog = false }) {
                            Text("取消")
                        }
                    }
                )
            }

            // Edit Instruction Dialog
            if (showEditInstructionDialog) {
                AlertDialog(
                    onDismissRequest = { showEditInstructionDialog = false },
                    title = { Text("编辑系统指令") },
                    text = {
                        OutlinedTextField(
                            value = draftInstructionText,
                            onValueChange = { draftInstructionText = it },
                            label = { Text("指令内容") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                editingInstructionIndex?.let { index ->
                                    if (draftInstructionText.isNotBlank()) {
                                        viewModel.updateSystemInstructionPart(index, draftInstructionText)
                                        showEditInstructionDialog = false
                                        editingInstructionIndex = null
                                    } else {
                                        Toast.makeText(context, "指令内容不能为空", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Text("保存")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditInstructionDialog = false }) {
                            Text("取消")
                        }
                    }
                )
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
                onResponseLogprobsChange = viewModel::onResponseLogprobsChange,
                onOutputDimensionalityChange = viewModel::onOutputDimensionalityChange
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
                                    Column(
                                        modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.onChatModelChange(model.name.removePrefix("models/")) // 移除 "models/" 前缀
                                            viewModel.onDismissModelSelectionDialog()
                                            Log.d("SettingsScreen", "模型 ${model.displayName} 被选中。")
                                        }
                                        .padding(vertical = 8.dp)
                                    ) {
                                        Text(text = model.displayName, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "输入Token限制: ${model.inputTokenLimit}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "输出Token限制: ${model.outputTokenLimit}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
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
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
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

private fun exportDatabase(context: Context) {
    val success = DatabaseUtils.exportDatabase(context)
    val message = if (success) "数据库导出成功！" else "数据库导出失败。"
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}