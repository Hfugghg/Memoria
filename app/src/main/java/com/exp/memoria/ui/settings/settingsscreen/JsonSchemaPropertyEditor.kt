package com.exp.memoria.ui.settings.settingsscreen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.exp.memoria.ui.settings.JsonSchemaProperty
import com.exp.memoria.ui.settings.JsonSchemaPropertyType
import com.exp.memoria.ui.settings.StringFormat
import kotlinx.coroutines.launch

/**
 * ## JSON Schema 属性编辑器 Composable
 *
 * 这是一个独立的、可重用的 Composable，用于编辑单个 `JsonSchemaProperty` 对象的详细信息。
 * 它包含属性名、类型、描述以及特定于类型的其他设置（如字符串格式、数字范围等）。
 *
 * ### 主要职责:
 * 1.  **显示属性**: 在卡片中清晰地展示一个属性的所有可编辑字段。
 * 2.  **处理输入**: 提供 `OutlinedTextField`、`ExposedDropdownMenuBox` 等控件来接收用户输入。
 * 3.  **状态提升**: 不持有自己的状态，而是通过 `onPropertyChange` 和 `onDeleteProperty` 挂起函数将用户操作通知给父 Composable。
 * 4.  **条件 UI**: 根据属性的类型（`STRING`, `NUMBER` 等）动态显示或隐藏相关的设置字段。
 * 5.  **输入验证**: 对属性名进行简单的格式验证（只允许英文和数字）。
 *
 * @param property 需要编辑的 `JsonSchemaProperty` 对象。
 * @param onPropertyChange 当属性的任何字段发生变化时调用的挂起函数，它会返回一个更新后的 `JsonSchemaProperty` 副本。
 * @param onDeleteProperty 当删除按钮被点击时调用的挂起函数，传递当前属性的 ID。
 * @param showDeleteButton 一个布尔值，用于控制是否显示删除按钮。这使得该组件可以同时用于“草稿”编辑（不显示删除）和列表项编辑（显示删除）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JsonSchemaPropertyEditor(
    property: JsonSchemaProperty,
    onPropertyChange: suspend (JsonSchemaProperty) -> Unit,
    onDeleteProperty: suspend (Long) -> Unit,
    showDeleteButton: Boolean = true // 默认显示删除按钮
) {
    val scope = rememberCoroutineScope()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 顶部行：属性名和删除按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(property.name.ifBlank { "新属性" }, style = MaterialTheme.typography.titleSmall)
                if (showDeleteButton) {
                    IconButton(onClick = { scope.launch { onDeleteProperty(property.id) } }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除属性")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // 属性名输入框
            OutlinedTextField(
                value = property.name,
                onValueChange = { newValue ->
                    scope.launch {
                        // 验证：仅允许输入英文和数字
                        if (newValue.matches(Regex("^[a-zA-Z0-9]*$"))) {
                            onPropertyChange(property.copy(name = newValue))
                        } else if (newValue.isBlank()) {
                            onPropertyChange(property.copy(name = ""))
                        }
                    }
                },
                label = { Text("属性名 (英文)") },
                isError = property.name.isBlank(),
                supportingText = { if (property.name.isBlank()) Text("属性名不能为空") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 类型选择下拉菜单
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
                    JsonSchemaPropertyType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName) },
                            onClick = {
                                scope.launch { onPropertyChange(property.copy(type = type)) }
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

            // 描述输入框
            OutlinedTextField(
                value = property.description,
                onValueChange = { scope.launch { onPropertyChange(property.copy(description = it)) } },
                label = { Text("描述") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 根据类型显示特定属性的编辑器
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
                            StringFormat.entries.forEach { format ->
                                DropdownMenuItem(
                                    text = { Text(format.displayName) },
                                    onClick = {
                                        scope.launch { onPropertyChange(property.copy(stringFormat = format)) }
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
                            scope.launch {
                                val doubleValue = newValue.toDoubleOrNull()
                                onPropertyChange(property.copy(numberMinimum = doubleValue))
                            }
                        },
                        label = { Text("最小值") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = property.numberMaximum?.toString() ?: "",
                        onValueChange = { newValue ->
                            scope.launch {
                                val doubleValue = newValue.toDoubleOrNull()
                                onPropertyChange(property.copy(numberMaximum = doubleValue))
                            }
                        },
                        label = { Text("最大值") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                else -> {
                    // 对于 OBJECT 和 ARRAY 类型，当前没有额外的特定属性需要编辑
                }
            }
        }
    }
}
