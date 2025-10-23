package com.exp.memoria.ui.chat

import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.* // 导入所有 Material3 组件
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.exp.memoria.ui.chat.chatviewmodel.ChatViewModel
import com.exp.memoria.ui.chatscreen.MessageBubble
import kotlinx.coroutines.launch
import java.util.UUID

private const val MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024 // 20MB

/**
 * 创建一个文件选择器的辅助函数，内置文件大小检查逻辑。
 */
@Composable
private fun rememberFilePickerLauncher(
    context: Context,
    onFileSizeError: () -> Unit,
    onFileSelected: (Uri) -> Unit
): ManagedActivityResultLauncher<String, Uri?> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileSize = context.contentResolver.openFileDescriptor(it, "r")?.use { pfd -> pfd.statSize }
            if (fileSize != null && fileSize > MAX_FILE_SIZE_BYTES) {
                onFileSizeError()
            } else {
                onFileSelected(it)
            }
        }
    }
}


/**
 * [聊天界面的Compose UI]
 *
 * 职责:
 * 1. 使用Jetpack Compose构建用户交互界面。
 * 2. 界面应包含一个消息列表（如LazyColumn）和一个位于底部的文本输入框及发送按钮。
 * 3. 观察(collect)来自 ChatViewModel 的UI状态(ChatState)。
 * 4. 当状态变化时，自动重组(Recompose)界面，例如显示新的消息或加载指示器。
 * 5. 将用户的输入和点击事件传递给 ChatViewModel 进行处理。
 * 6. 确保UI流畅，所有耗时操作都在ViewModel的协程中完成。
 *
 * 关联:
 * - 通过Hilt的 `@HiltViewModel` 机制获取 ChatViewModel 的实例。
 * - 使用 `ui/chatscreen` 中定义的可复用组件来构建界面。
 *
 * 未实现的功能职责:
 * - TopAppBar 中没有显示当前的对话标题。
 * - 没有处理消息发送失败的情况，例如在UI上给出提示。
 * - "Switch Conversation" 的功能不完整，目前只是导航到历史列表。
 */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    navController: NavController,
    conversationId: String?, // 接收 conversationId 参数
    viewModel: ChatViewModel = hiltViewModel(key = conversationId) // 将 conversationId 作为 key
) {
    val uiState by viewModel.uiState.collectAsState()
    val totalTokenCount by viewModel.totalTokenCount.collectAsState() // 收集 totalTokenCount
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var showFileSizeErrorDialog by remember { mutableStateOf(false) }

    val onFileSizeError = { showFileSizeErrorDialog = true }

    val imagePickerLauncher = rememberFilePickerLauncher(
        context = context,
        onFileSizeError = onFileSizeError,
        onFileSelected = viewModel::handleImageSelection
    )

    val filePickerLauncher = rememberFilePickerLauncher(
        context = context,
        onFileSizeError = onFileSizeError,
        onFileSelected = viewModel::handleFileSelection
    )

    if (showFileSizeErrorDialog) {
        AlertDialog(
            onDismissRequest = { showFileSizeErrorDialog = false },
            title = { Text("文件过大") },
            text = { Text("选择的文件大小不能超过20MB。") },
            confirmButton = {
                Button(onClick = { showFileSizeErrorDialog = false }) {
                    Text("确定")
                }
            }
        )
    }

    // 用于跟踪长按菜单显示状态的变量
    var showMenuForMessageId by remember { mutableStateOf<UUID?>(null) }
    // 用于跟踪正在编辑的消息ID
    var editingMessageId by remember { mutableStateOf<UUID?>(null) }
    // 用于存储正在编辑的文本
    var editingText by remember { mutableStateOf("") }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(uiState.messages.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.clickable {
                            navController.navigate("conversationHistory") {
                                popUpTo("conversationHistory")
                                launchSingleTop = true
                            }
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Memoria")
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Switch Conversation"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("settings/$conversationId") }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                onSendMessage = { viewModel.sendMessage(it) },
                isLoading = uiState.isLoading,
                tokensCount = totalTokenCount, // 传递 totalTokenCount
                onImagePickerClick = { imagePickerLauncher.launch("image/*") },
                onFilePickerClick = { filePickerLauncher.launch("*/*") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .pointerInput(Unit) {
                    // 点击空白区域时关闭菜单
                    detectTapGestures(onTap = { showMenuForMessageId = null })
                },
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = uiState.messages,
                key = { message -> message.id } // 使用唯一的ID作为key
            ) { message ->
                MessageBubble(
                    message = message,
                    isMenuVisible = showMenuForMessageId == message.id,
                    onLongPress = {
                        showMenuForMessageId = message.id
                        editingMessageId = null // 重置编辑状态
                    },
                    isEditing = editingMessageId == message.id,
                    editingText = if (editingMessageId == message.id) editingText else message.text,
                    onEditingTextChange = { editingText = it },
                    onEdit = {
                        editingMessageId = message.id
                        editingText = message.text
                        showMenuForMessageId = null
                    },
                    onResay = {
                        viewModel.regenerateResponse(message.id)
                        showMenuForMessageId = null
                    },
                    onConfirmEdit = {
                        editingMessageId?.let { id -> viewModel.updateMessage(id, editingText) }
                        editingMessageId = null
                    },
                    onCancelEdit = {
                        editingMessageId = null
                    }
                )
            }
            if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}

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
