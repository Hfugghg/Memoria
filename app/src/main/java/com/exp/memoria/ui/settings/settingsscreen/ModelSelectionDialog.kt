package com.exp.memoria.ui.settings.settingsscreen

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.exp.memoria.data.remote.api.ModelDetail

@Composable
fun ModelSelectionDialog(
    showModelSelectionDialog: Boolean,
    onDismissModelSelectionDialog: () -> Unit,
    isLoadingModels: Boolean,
    availableModels: List<ModelDetail>,
    onChatModelChange: (String) -> Unit,
    fetchAvailableModels: (Boolean) -> Unit,
    nextPageToken: State<String?>
) {
    if (showModelSelectionDialog) {
        Log.d("ModelSelectionDialog", "模型选择对话框正在显示。")
        AlertDialog(
            onDismissRequest = onDismissModelSelectionDialog,
            title = { Text("选择对话模型") },
            text = {
                Column {
                    // 根据加载状态和模型列表显示不同内容
                    if (isLoadingModels && availableModels.isEmpty()) {
                        Log.d("ModelSelectionDialog", "模型正在加载且列表为空。")
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else if (availableModels.isEmpty()) {
                        Log.d("ModelSelectionDialog", "模型加载完成但列表为空。")
                        Text("没有找到可用模型。请检查 API Key 和网络连接。")
                    } else {
                        Log.d("ModelSelectionDialog", "模型列表已加载，数量: ${availableModels.size}")
                        val listState = rememberLazyListState()
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(availableModels) { model ->
                                // 仅显示支持 generateContent 方法的模型
                                if (model.supportedGenerationMethods.contains("generateContent")) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onChatModelChange(model.name.removePrefix("models/")) // 移除 "models/" 前缀，并由 settingsViewModel 处理
                                                onDismissModelSelectionDialog()
                                                Log.d("ModelSelectionDialog", "模型 ${model.displayName} 被选中。")
                                            }
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Text(text = model.displayName, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "输入Token限制: ${model.inputTokenLimit}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "输出Token限制: ${model.outputTokenLimit}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }
                            // 加载更多的项目
                            item {
                                LaunchedEffect(listState.canScrollForward) {
                                    // 当滚动到底部、不在加载中且有下一页时，加载更多模型
                                    if (!listState.canScrollForward && !isLoadingModels && nextPageToken.value != null) {
                                        Log.d("ModelSelectionDialog", "滑动到底部，加载更多模型...")
                                        fetchAvailableModels(false)
                                    }
                                }
                                if (isLoadingModels) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Log.d("ModelSelectionDialog", "正在加载更多模型指示器。")
                                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissModelSelectionDialog) {
                    Text("关闭")
                }
            }
        )
    }
}
