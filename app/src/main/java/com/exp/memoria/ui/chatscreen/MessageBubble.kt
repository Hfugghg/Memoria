package com.exp.memoria.ui.chatscreen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.exp.memoria.ui.chat.chatviewmodel.ChatMessage
import dev.jeziellago.compose.markdowntext.MarkdownText

/**
 * [消息气泡]
 *
 * 这是一个可组合函数，用于显示单条聊天消息。
 * 它会根据消息的来源（用户或 AI）显示不同的对齐方式和背景颜色。
 *
 * @param message 要显示的消息对象。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    isMenuVisible: Boolean,
    onLongPress: () -> Unit,
    isEditing: Boolean,
    editingText: String,
    onEditingTextChange: (String) -> Unit,
    onEdit: () -> Unit,
    onResay: () -> Unit,
    onConfirmEdit: () -> Unit,
    onCancelEdit: () -> Unit
) {
    val horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isFromUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = horizontalArrangement
        ) {
            Card(
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onLongPress = { onLongPress() })
                },
                colors = CardDefaults.cardColors(
                    containerColor = if (message.isFromUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                if (isEditing) {
                    // 编辑模式下仍然使用标准的 TextField
                    TextField(
                        value = editingText,
                        onValueChange = onEditingTextChange,
                        modifier = Modifier.padding(12.dp)
                    )
                } else {
                    // 使用 MarkdownText 替代标准的 Text Composable 来渲染 Markdown 内容
                    MarkdownText(
                        markdown = message.text,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
        // 显示操作菜单
        if (isMenuVisible || isEditing) {
            MessageActionMenu(
                message = message,
                isEditing = isEditing,
                onEdit = onEdit,
                onResay = onResay,
                onConfirmEdit = onConfirmEdit,
                onCancelEdit = onCancelEdit
            )
        }
    }
}

@Composable
fun MessageActionMenu(
    message: ChatMessage,
    isEditing: Boolean,
    onEdit: () -> Unit,
    onResay: () -> Unit,
    onConfirmEdit: () -> Unit,
    onCancelEdit: () -> Unit
) {
    Row(
        modifier = Modifier.padding(top = 4.dp)
    ) {
        if (isEditing) {
            IconButton(onClick = onConfirmEdit, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Check, contentDescription = "Confirm")
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onCancelEdit, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Cancel")
            }
        } else {
            // 用户消息和LLM消息都有编辑按钮
            IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            // 只有LLM的消息有重说按钮
            if (!message.isFromUser) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onResay, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Resay")
                }
            }
        }
    }
}