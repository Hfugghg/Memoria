package com.exp.memoria.ui.chatscreen

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * [聊天输入栏]
 *
 * 这是一个可组合函数，提供了文本输入框和发送按钮，用于用户输入和发送消息。
 *
 * @param onSendMessage 当用户点击发送按钮时触发的回调，参数为输入的文本内容和附件列表。
 * @param isLoading 指示LLM是否正在响应，用于禁用发送按钮。
 * @param tokensCount 当前上下文的Token数量。
 * @param onImagePickerClick 当用户点击选择图片按钮时触发的回调。
 * @param onFilePickerClick 当用户点击选择文件按钮时触发的回调。
 * @param attachments 附件列表。
 * @param onRemoveAttachment 当用户点击移除附件按钮时触发的回调。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInputBar(
    onSendMessage: (String, List<Uri>) -> Unit,
    isLoading: Boolean,
    tokensCount: Int?, // 修改参数类型为 Int?
    onImagePickerClick: () -> Unit,
    onFilePickerClick: () -> Unit,
    attachments: List<Uri>,
    onRemoveAttachment: (Uri) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var isMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(visible = attachments.isNotEmpty()) {
            AttachmentInputPreview(
                attachments = attachments,
                onRemoveAttachment = onRemoveAttachment
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp), // 增加了整体的水平内边距，让布局呼吸感更好
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

                // 缩小了附件按钮的尺寸，使其更紧凑
                IconButton(
                    onClick = { isMenuExpanded = !isMenuExpanded },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isMenuExpanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = if (isMenuExpanded) "关闭菜单" else "展开菜单",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp)) // 保留一个微小的间距

            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("请输入文本…", maxLines = 1, overflow = TextOverflow.Ellipsis) }, // 确保 placeholder 不会换行
                trailingIcon = {
                    // 只有当输入框为空且 tokensCount 不为 null 时才显示Tokens计数
                    if (text.isBlank() && tokensCount != null) {
                        val displayTokens = if (tokensCount > 999) {
                            String.format("%.1fk", tokensCount / 1000f).replace(".0", "")
                        } else {
                            tokensCount.toString()
                        }
                        Text(
                            text = "Tokens: $displayTokens",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp) // 调整内边距以美观
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            // 减小了发送按钮的水平内边距，并移除了不必要的 modifier
            Button(
                onClick = {
                    onSendMessage(text, attachments)
                    text = ""
                },
                enabled = !isLoading && (text.isNotBlank() || attachments.isNotEmpty()),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("发送")
            }
        }
    }
}

@Composable
fun AttachmentInputPreview(attachments: List<Uri>, onRemoveAttachment: (Uri) -> Unit) {
    val context = LocalContext.current

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(attachments) { uri ->
            val mimeType = context.contentResolver.getType(uri)
            val fileName = getFileName(context.contentResolver, uri)

            Box {
                Card(
                    modifier = Modifier
                        .size(64.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (mimeType?.startsWith("image/") == true) {
                            AsyncImage(
                                model = uri,
                                contentDescription = fileName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // 对于非图片文件，显示通用文件图标和文件名
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = "File") // 通用文件图标
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = fileName ?: "File",
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                IconButton(
                    onClick = { onRemoveAttachment(uri) },
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove attachment",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// 辅助函数：从 Uri 获取文件名
private fun getFileName(contentResolver: ContentResolver, uri: Uri): String? {
    var name: String? = null
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                name = cursor.getString(nameIndex)
            }
        }
    }
    return name
}
