package com.exp.memoria.ui.chat

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box

/**
 * [对话历史记录页面]
 *
 * 职责:
 * 1. 显示所有过去的对话列表。
 * 2. 提供创建新对话的入口。
 * 3. 允许用户选择一个历史对话并返回到聊天界面继续对话。
 * 4. 支持长按删除对话。
 * 5. 支持长按改名对话。
 *
 * 未实现的功能职责:
 * - **对话预览**: 列表项仅显示对话名称和时间，可以增加最后一条消息的预览以提供更多上下文。
 * - **搜索和过滤**: 当对话列表很长时，没有提供搜索或过滤功能。
 * - **更醒目的“新建”入口**: “新建对话”只是一个文本按钮，可以考虑使用更显眼的悬浮操作按钮(FAB)。
 * - **撤销删除**: 删除对话后，没有提供类似 Snackbar 的“撤销”选项，这是一个常见的提升用户体验的功能。
 * - **空状态引导**: 空列表的提示可以更友好，例如增加一个图标或更具引导性的文案。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class) // 启用实验性 Foundation API 注解
@Composable
fun ConversationHistoryScreen(
    navController: NavController,
    viewModel: ConversationHistoryViewModel = hiltViewModel()
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val coroutineScope = rememberCoroutineScope()

    // 删除对话状态
    var showDeleteDialog by remember { mutableStateOf(false) }
    var conversationToDeleteId by remember { mutableStateOf<String?>(null) }

    // 重命名对话状态
    var showRenameDialog by remember { mutableStateOf(false) }
    var conversationToRenameId by remember { mutableStateOf<String?>(null) }
    var newConversationName by remember { mutableStateOf("") }

    // 下拉菜单状态（长按上下文菜单）
    var showDropdownMenu by remember { mutableStateOf(false) }
    var selectedConversationIdForMenu by remember { mutableStateOf<String?>(null) }
    var selectedConversationNameForMenu by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("对话记录") },
                actions = {
                    TextButton(onClick = {
                        val newConversationId = UUID.randomUUID().toString()
                        Log.d("ConversationHistoryScreen", "生成新的 conversationId: $newConversationId")
                        coroutineScope.launch {
                            viewModel.createNewConversation(newConversationId)
                        }
                        navController.navigate("chat?conversationId=$newConversationId") {
                            popUpTo("conversationHistory") { inclusive = true }
                        }
                    }) {
                        Text("新建对话")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (conversations.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("还没有对话记录")
                    Text("点击“新建对话”开始吧！")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(conversations) { conversation ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        Log.d("ConversationHistoryScreen", "导航到现有对话: ${conversation.conversationId}")
                                        navController.navigate("chat?conversationId=${conversation.conversationId}") {
                                            popUpTo("conversationHistory") { inclusive = true }
                                        }
                                    },
                                    onLongClick = {
                                        selectedConversationIdForMenu = conversation.conversationId
                                        selectedConversationNameForMenu = conversation.name.ifBlank { "新对话" } // 当名称为空时，显示“新对话”
                                        showDropdownMenu = true
                                    }
                                )
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = "对话: ${conversation.name.ifBlank { "新对话" }}" // 显示名称，如果为空则显示“新对话”
                                ) // 不再拼接 conversationId，并且不进行截断
                                Text(text = "最后更新: ${dateFormatter.format(Date(conversation.lastTimestamp))}")
                            }

                            DropdownMenu(
                                expanded = showDropdownMenu && selectedConversationIdForMenu == conversation.conversationId,
                                onDismissRequest = { showDropdownMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("重命名") },
                                    onClick = {
                                        conversationToRenameId = conversation.conversationId
                                        newConversationName = selectedConversationNameForMenu // 预填充当前名称
                                        showRenameDialog = true
                                        showDropdownMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("删除") },
                                    onClick = {
                                        conversationToDeleteId = conversation.conversationId
                                        showDeleteDialog = true
                                        showDropdownMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 删除确认对话框
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("删除对话") },
                text = { Text("确定要删除此对话吗？此操作无法撤消。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            conversationToDeleteId?.let { id ->
                                coroutineScope.launch {
                                    viewModel.deleteConversation(id) // 调用 ViewModel 删除对话函数
                                }
                            }
                            showDeleteDialog = false
                            conversationToDeleteId = null
                        }
                    ) {
                        Text("删除")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            conversationToDeleteId = null
                        }
                    ) {
                        Text("取消")
                    }
                }
            )
        }

        // 重命名对话框
        if (showRenameDialog) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("重命名对话") },
                text = {
                    TextField(
                        value = newConversationName,
                        onValueChange = { newConversationName = it },
                        label = { Text("新对话名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            conversationToRenameId?.let { id ->
                                val nameToRename = newConversationName // 捕获当前值
                                Log.d("ConversationScreen", "在 isNotBlank 检查之前。新对话名称: '$nameToRename'")
                                if (nameToRename.isNotBlank()) {
                                    Log.d("ConversationScreen", "在 isNotBlank 块内。调用重命名: '$nameToRename'")
                                    coroutineScope.launch {
                                        viewModel.renameConversation(id, nameToRename) // 使用捕获的值
                                    }
                                } else {
                                    Log.d("ConversationScreen", "newName 为空。不调用重命名.")
                                }
                            }
                            showRenameDialog = false
                            conversationToRenameId = null
                            newConversationName = ""
                        }
                    ) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showRenameDialog = false
                            conversationToRenameId = null
                            newConversationName = ""
                        }
                    ) {
                        Text("取消")
                    }
                }
            )
        }
    }
}