package com.exp.memoria.ui.settings.settingsscreen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * ## 安全设置区域 Composable
 *
 * 这个 Composable 将所有与安全相关的设置项（例如骚扰、仇恨言论等）组合在一起，
 * 以一个卡片的形式显示在界面上，提供一个整洁、有组织的设置区域。
 *
 * ### 主要职责:
 * 1.  **分组和布局**: 使用 `Card` 将相关的安全设置项包裹起来，与其他设置项区分开。
 * 2.  **委托渲染**: 为每个安全类别（骚扰、仇恨言论等）调用 `SettingSlider` Composable，
 * 并将相应的状态和挂起函数传递给它。
 *
 * @param harassmentValue 骚扰内容滑块的当前值。
 * @param onHarassmentChange 当骚扰内容滑块值改变时触发的挂起函数。
 * @param hateSpeechValue 仇恨言论滑块的当前值。
 * @param onHateSpeechChange 当仇恨言论滑块值改变时触发的挂起函数。
 * @param sexuallyExplicitValue 色情内容滑块的当前值。
 * @param onSexuallyExplicitChange 当色情内容滑块值改变时触发的挂起函数。
 * @param dangerousContentValue 危险内容滑块的当前值。
 * @param onDangerousContentChange 当危险内容滑块值改变时触发的挂起函数。
 */
@Composable
fun SafetySettingsSection(
    harassmentValue: Float,
    onHarassmentChange: suspend (Float) -> Unit,
    hateSpeechValue: Float,
    onHateSpeechChange: suspend (Float) -> Unit,
    sexuallyExplicitValue: Float,
    onSexuallyExplicitChange: suspend (Float) -> Unit,
    dangerousContentValue: Float,
    onDangerousContentChange: suspend (Float) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("安全设置", style = MaterialTheme.typography.titleMedium)
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
 * ## 设置项滑块 Composable
 *
 * 这是一个通用的、私有的滑块组件，专门用于安全设置中的每个具体项目。
 * 它包含一个标签、一个描述当前屏蔽等级的文本和一个具有 4 个步骤的滑块，
 * 使用户可以直观地调整安全屏蔽阈值。
 *
 * ### 主要职责:
 * 1.  **值到文本的转换**: 将传入的浮点数值（0.0f 到 1.0f）转换为用户易于理解的文本描述
 * （如“未指定”、“不屏蔽”、“仅高风险即屏蔽”等）。
 * 2.  **UI 渲染**: 显示标签和转换后的文本，以及一个 `Slider` 组件。
 * 3.  **状态提升**: `Slider` 的值和 `onValueChange` 挂起函数由父 Composable 控制，
 * 使其成为一个无状态的、可重用的组件。
 *
 * @param label 滑块的标签，例如“骚扰内容”。
 * @param value 滑块的当前浮点数值 (0.0f 到 1.0f 之间)。
 * @param onValueChange 当滑块值发生改变时调用的挂起函数。
 */
@Composable
private fun SettingSlider(label: String, value: Float, onValueChange: suspend (Float) -> Unit) {
    val scope = rememberCoroutineScope()
    // 将 0.0f-1.0f 的浮点值映射到四个档位，并转换为用户可读的文本
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
            onValueChange = {
                scope.launch {
                    onValueChange(it)
                }
            },
            valueRange = 0f..1f,
            steps = 3 // 步数为3，意味着滑块有4个可选位置 (0, 1/3, 2/3, 1)
        )
    }
}
