package com.exp.memoria.ui.chat

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
// import androidx.lifecycle.SavedStateHandle // 不再需要直接导入

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
 * - 使用 `ui/components` 中定义的可复用组件来构建界面。
 *
 * 未实现的功能职责:
 * - TopAppBar 中没有显示当前的对话标题。
 * - 没有处理消息发送失败的情况，例如在UI上给出提示。
 * - "Switch Conversation" 的功能不完整，目前只是导航到历史列表。
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    conversationId: String?, // 接收 conversationId 参数
    viewModel: ChatViewModel = hiltViewModel(key = conversationId) // 将 conversationId 作为 key
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

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
                        modifier = Modifier.clickable { navController.navigate("conversationHistory") },
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
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        },
        bottomBar = { ChatInputBar(onSendMessage = { viewModel.sendMessage(it) }) }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = uiState.messages,
                key = { message -> message.id } // 使用唯一的ID作为key
            ) { message ->
                MessageBubble(message = message)
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
 * [消息气泡]
 *
 * 这是一个可组合函数，用于显示单条聊天消息。
 * 它会根据消息的来源（用户或 AI）显示不同的对齐方式和背景颜色。
 *
 * @param message 要显示的消息对象。
 */
@Composable
fun MessageBubble(message: ChatMessage) {
    val horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (message.isFromUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

/**
 * [聊天输入栏]
 *
 * 这是一个可组合函数，提供了文本输入框和发送按钮，用于用户输入和发送消息。
 *
 * @param onSendMessage 当用户点击发送按钮时触发的回调，参数为输入的文本内容。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInputBar(onSendMessage: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("与 Memoria 对话...") }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = {
            onSendMessage(text)
            text = ""
        }) {
            Text("发送")
        }
    }
}
