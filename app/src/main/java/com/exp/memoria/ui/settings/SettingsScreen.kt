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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * [设置界面的 Composable UI]
 *
 * 职责:
 * 1. 提供一个用户界面，允许用户查看和修改应用的各项设置。
 * 2. 观察来自 SettingsViewModel 的 UI 状态 (Settings)，并根据状态更新界面上的控件。
 * 3. 将用户的输入（例如文本框输入、滑块拖动、复选框点击）通知给 SettingsViewModel。
 *
 * @param viewModel SettingsViewModel 的实例，通过 Hilt 自动注入。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settingsState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("设置") })
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
            SafetySettingsSection(
                harassmentValue = settings.harassment,
                onHarassmentChange = viewModel::onHarassmentChange,
                hateSpeechValue = settings.hateSpeech,
                onHateSpeechChange = viewModel::onHateSpeechChange,
                sexuallyExplicitValue = settings.sexuallyExplicit,
                onSexuallyExplicitChange = viewModel::onSexuallyExplicitChange,
                dangerousContentValue = settings.dangerousContent,
                onDangerousContentChange = viewModel::onDangerousContentChange
            )

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
 * [安全设置区域 Composable]
 *
 * 这是一个可组合函数，它将所有与安全相关的设置项（例如骚扰、仇恨言论等）组合在一起，
 * 以一个卡片的形式显示在界面上。
 *
 * @param harassmentValue 骚扰内容滑块的当前值。
 * @param onHarassmentChange 当骚扰内容滑块值改变时触发的回调。
 * @param hateSpeechValue 仇恨言论滑块的当前值。
 * @param onHateSpeechChange 当仇恨言论滑块值改变时触发的回调。
 * @param sexuallyExplicitValue 色情内容滑块的当前值。
 * @param onSexuallyExplicitChange 当色情内容滑块值改变时触发的回调。
 * @param dangerousContentValue 危险内容滑块的当前值。
 * @param onDangerousContentChange 当危险内容滑块值改变时触发的回调。
 */
@Composable
fun SafetySettingsSection(
    harassmentValue: Float,
    onHarassmentChange: (Float) -> Unit,
    hateSpeechValue: Float,
    onHateSpeechChange: (Float) -> Unit,
    sexuallyExplicitValue: Float,
    onSexuallyExplicitChange: (Float) -> Unit,
    dangerousContentValue: Float,
    onDangerousContentChange: (Float) -> Unit
) {
    // 安全设置卡片
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("安全设置", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            // 骚扰内容设置
            SettingSlider(label = "骚扰内容", value = harassmentValue, onValueChange = onHarassmentChange)

            // 仇恨言论设置
            SettingSlider(label = "仇恨言论", value = hateSpeechValue, onValueChange = onHateSpeechChange)

            // 色情内容设置
            SettingSlider(label = "色情内容", value = sexuallyExplicitValue, onValueChange = onSexuallyExplicitChange)

            // 危险内容设置
            SettingSlider(label = "危险内容", value = dangerousContentValue, onValueChange = onDangerousContentChange)
        }
    }
}

/**
 * [设置项滑块 Composable]
 *
 * 这是一个通用的滑块组件，用于安全设置中的每个具体项目。
 * 它包含一个标签、一个描述当前等级的文本和一个具有 4 个步骤的滑块。
 *
 * @param label 滑块的标签，例如“骚扰内容”。
 * @param value 滑块的当前浮点数值 (0.0f 到 1.0f 之间)。
 * @param onValueChange 当滑块值改变时调用的回调函数。
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
