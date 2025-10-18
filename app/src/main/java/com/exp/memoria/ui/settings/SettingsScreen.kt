package com.exp.memoria.ui.settings

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * [设置界面的 Composable UI]
 *
 * 职责:
 * 1. 提供一个用户界面，允许用户查看和修改应用的各项设置。
 * 2. 观察来自 SettingsViewModel 的 UI 状态 (Settings)，并根据状态更新界面上的控件。
 * 3. 将用户的输入（例如文本框输入、滑块拖动、复选框点击）通知给 SettingsViewModel。
 *
 * @param viewModel SettingsViewModel 的实例，通过 Hilt 自动注入。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settingsState.collectAsState()
    val showModelSelectionDialog by viewModel.showModelSelectionDialog.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()
    val isLoadingModels by viewModel.isLoadingModels.collectAsState()
    val showApiKeyError by viewModel.showApiKeyError.collectAsState()
    val isGraphicalSchemaMode by viewModel.isGraphicalSchemaMode.collectAsState()
    val graphicalSchemaProperties by viewModel.graphicalSchemaProperties.collectAsState()
    val currentResponseSchemaString by viewModel.currentResponseSchemaString.collectAsState()
    val draftProperty by viewModel.draftProperty.collectAsState() // 获取草稿属性

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("设置") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()) // 添加垂直滚动
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = settings.apiKey,
                onValueChange = viewModel::onApiKeyChange,
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth()
            )

            // 对话模型选择
            OutlinedTextField(
                value = settings.chatModel,
                onValueChange = viewModel::onChatModelChange, // 恢复手动输入功能
                label = { Text("对话模型") },
                readOnly = false, // 允许手动输入
                trailingIcon = { // 将点击事件绑定到图标按钮上
                    IconButton(onClick = {
                        Log.d("SettingsScreen", "下拉箭头图标被点击。API Key是否为空: ${settings.apiKey.isBlank()}")
                        // 点击时尝试获取模型列表并显示弹窗
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
                modifier = Modifier.fillMaxWidth() // 移除 clickable 修饰符
            )

            Text("Temperature: ${settings.temperature}")
            Slider(
                value = settings.temperature,
                onValueChange = viewModel::onTemperatureChange,
                valueRange = 0f..1f,
                steps = 10
            )
            Text("Top P: ${settings.topP}")
            Slider(
                value = settings.topP,
                onValueChange = viewModel::onTopPChange,
                valueRange = 0f..1f,
                steps = 10
            )

            // Response Schema 设置
            Spacer(modifier = Modifier.height(16.dp))
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

            if (isGraphicalSchemaMode) {
                GraphicalSchemaEditor(
                    properties = graphicalSchemaProperties,
                    draftProperty = draftProperty, // 传递草稿属性
                    onDraftPropertyChange = viewModel::onDraftPropertyChange, // 传递草稿属性更新回调
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


            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("本地存储")
                Checkbox(
                    checked = settings.useLocalStorage,
                    onCheckedChange = viewModel::onUseLocalStorageChange
                ) // 修正 Checkbox 的语法错误
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("LLM 高级设置", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            // 其他LLM设置的占位符
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

            // 可交互的安全设置
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

            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("生成配置", style = MaterialTheme.typography.titleMedium)
                    Text("// 尚未实现", style = MaterialTheme.typography.bodySmall)
                }
            }
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
                                if (model.supportedGenerationMethods.contains("generateContent")) { // 仅显示支持 generateContent 的模型
                                    Column(modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.onChatModelChange(model.name.removePrefix("models/")) // 移除 "models/"
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
                            // 加载更多模型
                            item {
                                LaunchedEffect(listState.canScrollForward) {
                                    // 仅当列表不能再向前滚动（即滚动到底部）、当前不在加载中且存在 nextPageToken 时才加载更多
                                    if (!listState.canScrollForward && !isLoadingModels && viewModel.nextPageToken.value != null) {
                                        Log.d("SettingsScreen", "滑动到底部，加载更多模型...")
                                        viewModel.fetchAvailableModels(initialLoad = false)
                                    }
                                }
                                if (isLoadingModels) {
                                    // 修复: 使用 Column 包裹来正确居中
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

/**
 * [图形化 Response Schema 编辑器 Composable]
 */
@Composable
fun GraphicalSchemaEditor(
    properties: List<JsonSchemaProperty>,
    draftProperty: JsonSchemaProperty, // 新增草稿属性
    onDraftPropertyChange: (JsonSchemaProperty) -> Unit, // 新增草稿属性更新回调
    onAddProperty: () -> Unit,
    onUpdateProperty: (JsonSchemaProperty) -> Unit,
    onDeleteProperty: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 用于编辑草稿属性的编辑器
        JsonSchemaPropertyEditor(
            property = draftProperty,
            onPropertyChange = onDraftPropertyChange,
            onDeleteProperty = { /* 草稿属性没有删除按钮 */ },
            showDeleteButton = false // 隐藏删除按钮
        )
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onAddProperty,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            enabled = draftProperty.name.isNotBlank() // 只有当草稿属性名不为空时才可点击
        ) {
            Icon(Icons.Default.Add, contentDescription = "添加到列表")
            Text("添加到列表")
        }

        if (properties.isEmpty()) {
            Text(
                "点击“添加到列表”开始定义您的 Response Schema。",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 400.dp) // 允许高度根据内容展开，但限制最大高度
            ) {
                items(properties, key = { it.id }) { property ->
                    JsonSchemaPropertyEditor(
                        property = property,
                        onPropertyChange = onUpdateProperty,
                        onDeleteProperty = onDeleteProperty,
                        showDeleteButton = true // 显示删除按钮
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * [JSON Schema 属性编辑器 Composable]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JsonSchemaPropertyEditor(
    property: JsonSchemaProperty,
    onPropertyChange: (JsonSchemaProperty) -> Unit,
    onDeleteProperty: (Long) -> Unit,
    showDeleteButton: Boolean = true // 新增参数，控制是否显示删除按钮
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(property.name.ifBlank { "新属性" }, style = MaterialTheme.typography.titleSmall)
                if (showDeleteButton) { // 根据参数决定是否显示删除按钮
                    IconButton(onClick = { onDeleteProperty(property.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除属性")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = property.name,
                onValueChange = { newValue ->
                    // 仅允许英文和数字
                    if (newValue.matches(Regex("^[a-zA-Z0-9]*$"))) {
                        onPropertyChange(property.copy(name = newValue))
                    }
                },
                label = { Text("属性名 (英文)") },
                isError = property.name.isBlank(),
                supportingText = { if (property.name.isBlank()) Text("属性名不能为空") },
                // 修复: 移除重复的 Modifier.Modifier
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Type Dropdown
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = property.type.displayName,
                    onValueChange = { /* 只读 */ },
                    readOnly = true,
                    label = { Text("类型") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    JsonSchemaPropertyType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName) },
                            onClick = {
                                onPropertyChange(property.copy(type = type))
                                expanded = false
                            }
                        )
                    }
                }
            }
            Text(
                text = property.type.description,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Description TextField
            OutlinedTextField(
                value = property.description,
                onValueChange = { onPropertyChange(property.copy(description = it)) },
                label = { Text("描述") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Type-specific properties
            when (property.type) {
                JsonSchemaPropertyType.STRING -> {
                    var formatExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = formatExpanded,
                        onExpandedChange = { formatExpanded = !formatExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = property.stringFormat.displayName,
                            onValueChange = { /* 只读 */ },
                            readOnly = true,
                            label = { Text("字符串格式") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = formatExpanded,
                            onDismissRequest = { formatExpanded = false }
                        ) {
                            StringFormat.values().forEach { format ->
                                DropdownMenuItem(
                                    text = { Text(format.displayName) },
                                    onClick = {
                                        onPropertyChange(property.copy(stringFormat = format))
                                        formatExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Text(
                        text = property.stringFormat.description,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                JsonSchemaPropertyType.NUMBER -> {
                    OutlinedTextField(
                        value = property.numberMinimum?.toString() ?: "",
                        onValueChange = { newValue ->
                            val doubleValue = newValue.toDoubleOrNull()
                            onPropertyChange(property.copy(numberMinimum = doubleValue))
                        },
                        label = { Text("最小值") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = property.numberMaximum?.toString() ?: "",
                        onValueChange = { newValue ->
                            val doubleValue = newValue.toDoubleOrNull()
                            onPropertyChange(property.copy(numberMaximum = doubleValue))
                        },
                        label = { Text("最大值") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                else -> {
                    // 对于 OBJECT 和 ARRAY 简化处理，暂时没有特定属性
                }
            }
        }
    }
}


/**
 * [安全设置区域 Composable]
 *
 * 这是一个可组合函数，它将所有与安全相关的设置项（例如骚扰、仇恨言论等）组合在一起，
 * 以一个卡片的形式显示在界面上。
 *
 * @param harassmentValue 骚扰内容滑块的当前值。
 * @param onHarassmentChange 当骚扰内容滑块值改变时触发的回调。
 * @param hateSpeechValue 仇恨言论滑块的当前值。
 * @param onHateSpeechChange 当仇恨言论滑块值改变时触发的回调。
 * @param sexuallyExplicitValue 色情内容滑块的当前值。
 * @param onSexuallyExplicitChange 当色情内容滑块值改变时触发的回调。
 * @param dangerousContentValue 危险内容滑块的当前值。
 * @param onDangerousContentChange 当危险内容滑块值改变时触发的回调。
 */
@Composable
fun SafetySettingsSection(
    harassmentValue: Float,
    onHarassmentChange: (Float) -> Unit,
    hateSpeechValue: Float,
    onHateSpeechChange: (Float) -> Unit,
    sexuallyExplicitValue: Float,
    onSexuallyExplicitChange: (Float) -> Unit,
    dangerousContentValue: Float,
    onDangerousContentChange: (Float) -> Unit
) {
    // 安全设置卡片
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("安全设置", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            // 骚扰内容设置
            SettingSlider(label = "骚扰内容", value = harassmentValue, onValueChange = onHarassmentChange)

            // 仇恨言论设置
            SettingSlider(label = "仇恨言论", value = hateSpeechValue, onValueChange = onHateSpeechChange)

            // 色情内容设置
            SettingSlider(label = "色情内容", value = sexuallyExplicitValue, onValueChange = onSexuallyExplicitChange)

            // 危险内容设置
            SettingSlider(label = "危险内容", value = dangerousContentValue, onValueChange = onDangerousContentChange)
        }
    }
}

/**
 * [设置项滑块 Composable]
 *
 * 这是一个通用的滑块组件，用于安全设置中的每个具体项目。
 * 它包含一个标签、一个描述当前等级的文本和一个具有 4 个步骤的滑块。
 *
 * @param label 滑块的标签，例如“骚扰内容”。
 * @param value 滑块的当前浮点数值 (0.0f 到 1.0f 之间)。
 * @param onValueChange 当滑块值改变时调用的回调函数。
 */
@Composable
private fun SettingSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    // 将Float值转换为更易读的文本标签
    val levelText = when {
        value < 0.125f -> "未指定"
        value < 0.375f -> "不屏蔽"
        value < 0.625f -> "仅高风险即屏蔽"
        value < 0.875f -> "中风险及以上即屏蔽"
        else -> "低风险及以上即屏蔽"
    }
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text("$label: $levelText")
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            steps = 3
        )
    }
}