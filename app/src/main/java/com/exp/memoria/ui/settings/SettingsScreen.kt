package com.exp.memoria.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settingsState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()) // 添加垂直滚动
                .padding(16.dp)
        ) {
            TextField(
                value = settings.apiKey,
                onValueChange = viewModel::onApiKeyChange,
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth()
            )
            TextField(
                value = settings.chatModel,
                onValueChange = viewModel::onChatModelChange,
                label = { Text("Chat Model") },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Temperature: ${settings.temperature}")
            Slider(
                value = settings.temperature,
                onValueChange = viewModel::onTemperatureChange,
                valueRange = 0f..1f,
                steps = 10
            )
            Text("Top P: ${settings.topP}")
            Slider(
                value = settings.topP,
                onValueChange = viewModel::onTopPChange,
                valueRange = 0f..1f,
                steps = 10
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Use Local Storage")
                Checkbox(
                    checked = settings.useLocalStorage,
                    onCheckedChange = viewModel::onUseLocalStorageChange
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("LLM 高级设置", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            // 其他LLM设置的占位符
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("系统指令", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                    Text("// 尚未实现", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                }
            }
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("工具", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                    Text("// 尚未实现", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                }
            }
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("工具配置", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                    Text("// 尚未实现", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                }
            }

            // 可交互的安全设置
            SafetySettingsSection()

            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("生成配置", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                    Text("// 尚未实现", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/**
 * 安全设置的可组合函数
 * 允许用户分别为四个不同的安全类别调整滑块
 */
@Composable
fun SafetySettingsSection() {
    // 使用 remember 为每个安全设置的滑块创建一个可变状态，用于模拟调整
    var harassmentValue by remember { mutableStateOf(0.0f) }
    var hateSpeechValue by remember { mutableStateOf(0.0f) }
    var sexuallyExplicitValue by remember { mutableStateOf(0.0f) }
    var dangerousContentValue by remember { mutableStateOf(0.0f) }

    // 安全设置卡片
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("安全设置", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            // 骚扰内容设置
            SettingSlider(label = "骚扰内容", value = harassmentValue, onValueChange = { harassmentValue = it })

            // 仇恨言论设置
            SettingSlider(label = "仇恨言论", value = hateSpeechValue, onValueChange = { hateSpeechValue = it })

            // 色情内容设置
            SettingSlider(label = "色情内容", value = sexuallyExplicitValue, onValueChange = { sexuallyExplicitValue = it })

            // 危险内容设置
            SettingSlider(label = "危险内容", value = dangerousContentValue, onValueChange = { dangerousContentValue = it })
        }
    }
}

/**
 * 一个通用的滑块组件，用于安全设置中的每个项目
 * @param label 滑块的标签
 * @param value 滑块的当前值
 * @param onValueChange 当滑块值改变时调用的回调
 */
@Composable
private fun SettingSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    // 将Float值转换为更易读的文本标签
    val levelText = when {
        value < 0.125f -> "未指定"
        value < 0.375f -> "不屏蔽"
        value < 0.625f -> "仅高风险即屏蔽"
        value < 0.875f -> "中风险及以上即屏蔽"
        else -> "低风险及以上即屏蔽"
    }
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text("$label: $levelText")
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            steps = 3
        )
    }
}
