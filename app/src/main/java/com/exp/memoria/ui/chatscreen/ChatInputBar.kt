package com.exp.memoria.ui.chatscreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * [聊天输入栏]
 *
 * 这是一个可组合函数，提供了文本输入框和发送按钮，用于用户输入和发送消息。
 *
 * @param onSendMessage 当用户点击发送按钮时触发的回调，参数为输入的文本内容。
 * @param isLoading 指示LLM是否正在响应，用于禁用发送按钮。
 * @param tokensCount 当前上下文的Token数量。
 * @param onImagePickerClick 当用户点击选择图片按钮时触发的回调。
 * @param onFilePickerClick 当用户点击选择文件按钮时触发的回调。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInputBar(
    onSendMessage: (String) -> Unit,
    isLoading: Boolean,
    tokensCount: Int?, // 修改参数类型为 Int?
    onImagePickerClick: () -> Unit,
    onFilePickerClick: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var isMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Bottom // 将所有子项与底部对齐
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedVisibility(visible = isMenuExpanded) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = {
                        onImagePickerClick()
                        isMenuExpanded = false
                    }) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "选择图片",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    IconButton(onClick = {
                        onFilePickerClick()
                        isMenuExpanded = false
                    }) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "选择文件",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            IconButton(onClick = { isMenuExpanded = !isMenuExpanded }) {
                Icon(
                    imageVector = if (isMenuExpanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = if (isMenuExpanded) "关闭菜单" else "展开菜单",
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        TextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("与 Memoria 对话...") },
            trailingIcon = {
                // 只有当输入框为空且 tokensCount 不为 null 时才显示Tokens计数
                if (text.isBlank() && tokensCount != null) {
                    Text(
                        text = "Tokens: $tokensCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp) // 调整内边距以美观
                    )
                }
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = {
                onSendMessage(text)
                text = ""
            },
            enabled = !isLoading && text.isNotBlank(), // 当 isLoading 为 true 或文本为空时禁用按钮
            modifier = Modifier.height(IntrinsicSize.Min) // 确保按钮与TextField高度大致匹配
        ) {
            Text("发送")
        }
    }
}
