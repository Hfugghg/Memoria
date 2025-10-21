package com.exp.memoria.ui.settings.settingsscreen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.exp.memoria.ui.settings.JsonSchemaProperty

/**
 * ## 图形化 Response Schema 编辑器 Composable
 *
 * 这个 Composable 提供了一个用户友好的界面，用于创建和管理 JSON Schema 的属性列表。
 * 它由一个“草稿”属性编辑器、一个“添加到列表”按钮和一个已添加属性的列表组成。
 *
 * ### 主要职责:
 * 1.  **布局容器**: 为属性编辑器和属性列表提供整体布局。
 * 2.  **草稿管理**: 显示一个用于输入新属性的 `JsonSchemaPropertyEditor`（草稿区）。
 * 3.  **属性添加**: 提供一个按钮，当草稿属性有效时，用户可以点击它来将草稿添加到列表中。
 * 4.  **属性列表**: 使用 `LazyColumn` 高效地显示所有已添加的属性，每个属性都由一个 `JsonSchemaPropertyEditor` 实例来渲染。
 * 5.  **空状态处理**: 在列表为空时显示提示信息。
 *
 * @param properties 当前已定义的 JSON Schema 属性列表。
 * @param draftProperty 用于创建新属性的草稿对象。
 * @param onDraftPropertyChange 当草稿属性发生变化时调用的回调函数。
 * @param onAddProperty 当用户点击“添加到列表”按钮时调用的回调函数。
 * @param onUpdateProperty 当列表中某个属性被修改时调用的回调函数。
 * @param onDeleteProperty 当用户删除列表中某个属性时调用的回调函数。
 */
@Composable
fun GraphicalSchemaEditor(
    properties: List<JsonSchemaProperty>,
    draftProperty: JsonSchemaProperty,
    onDraftPropertyChange: (JsonSchemaProperty) -> Unit,
    onAddProperty: () -> Unit,
    onUpdateProperty: (JsonSchemaProperty) -> Unit,
    onDeleteProperty: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 用于编辑新属性的“草稿”编辑器
        JsonSchemaPropertyEditor(
            property = draftProperty,
            onPropertyChange = onDraftPropertyChange,
            onDeleteProperty = { /* 草稿属性没有删除按钮 */ },
            showDeleteButton = false // 明确指示不显示删除按钮
        )
        Spacer(modifier = Modifier.height(8.dp))

        // “添加到列表”按钮
        Button(
            onClick = onAddProperty,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            // 只有当草稿属性的名称不为空时，按钮才可用
            enabled = draftProperty.name.isNotBlank()
        ) {
            Icon(Icons.Default.Add, contentDescription = "添加到列表")
            Text("添加到列表")
        }

        // 根据属性列表是否为空，显示提示信息或列表本身
        if (properties.isEmpty()) {
            Text(
                "点击“添加到列表”开始定义您的 Response Schema。",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            // 使用 LazyColumn 显示已添加的属性列表，以提高性能
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 400.dp) // 限制列表的最大高度，超出部分可滚动
            ) {
                items(properties, key = { it.id }) { property ->
                    JsonSchemaPropertyEditor(
                        property = property,
                        onPropertyChange = onUpdateProperty,
                        onDeleteProperty = onDeleteProperty,
                        showDeleteButton = true // 列表中的项目显示删除按钮
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}