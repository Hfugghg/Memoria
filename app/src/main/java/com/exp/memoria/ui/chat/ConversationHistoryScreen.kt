package com.exp.memoria.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

/**
 * [对话历史记录页面]
 *
 * 职责:
 * 1. 显示所有过去的对话列表。
 * 2. 提供创建新对话的入口。
 * 3. 允许用户选择一个历史对话并返回到聊天界面继续对话。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationHistoryScreen(
    navController: NavController,
    viewModel: ConversationHistoryViewModel = hiltViewModel()
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("对话记录") },
                actions = {
                    TextButton(onClick = {
                        // 创建新对话
                        val newConversationId = UUID.randomUUID().toString()
                        navController.navigate("chat?conversationId=$newConversationId") {
                            // 从后退栈中弹出此屏幕，以避免循环
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
                // 如果没有对话，显示一个提示信息
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
                        Column(modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate("chat?conversationId=${conversation.conversationId}") {
                                    // 从后退栈中弹出此屏幕，以避免循环
                                    popUpTo("conversationHistory") { inclusive = true }
                                }
                            }
                            .padding(16.dp)
                        ) {
                            // 为了可读性，只显示部分ID
                            Text(text = "对话: ...${conversation.conversationId.takeLast(6)}")
                            Text(text = "最后更新: ${dateFormatter.format(Date(conversation.lastTimestamp))}")
                        }
                    }
                }
            }
        }
    }
}
