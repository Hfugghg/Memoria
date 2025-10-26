package com.exp.memoria.ui.settings.settingsscreen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.exp.memoria.ui.settings.Settings

/**
 * ## 生成配置 Composable
 *
 * 这个 Composable 负责展示和处理所有与大型语言模型（LLM）生成内容相关的配置项。
 * 它将 `Settings` 数据类中的相关字段与 UI 输入框（如 `OutlinedTextField` 和 `Switch`）进行绑定，
 * 并通过回调函数将用户的修改通知给 `SettingsViewModel`。
 *
 * ### 主要功能:
 * - **Temperature**: 控制生成文本的随机性。值越高，结果越多样化。
 * - **Top-P**: 控制核心采样的范围，仅考虑累积概率达到 p 的词元。
 * - **Top-K**: 在采样时仅考虑概率最高的 k 个词元。
 * - **Max Output Tokens**: 限制单次生成内容的最大长度。
 * - **Stop Sequences**: 定义一个或多个字符串，当模型生成这些字符串时会停止输出。
 * - **Frequency Penalty**: 对高频词元施加惩罚，降低重复性。
 * - **Presence Penalty**: 对已出现过的词元施加惩罚，鼓励引入新概念。
 * - **Candidate Count**: 指定生成多少个候选回复。
 * - **Seed**: 随机种子，用于确保结果的可复现性。
 * - **Response MIME Type**: 指定期望的响应格式，如 `application/json`。
 * - **Response Logprobs**: 控制是否返回每个词元的对数概率。
 * - **Output Dimensionality**: 指定嵌入输出的维度。
 *
 * @param settings 当前的设置状态，包含所有生成配置的值。
 * @param onTemperatureChange 当 Temperature 值变化时调用的回调。
 * @param onTopPChange 当 Top-P 值变化时调用的回调。
 * @param onTopKChange 当 Top-K 值变化时调用的回调。
 * @param onMaxOutputTokensChange 当最大输出 Token 数变化时调用的回调。
 * @param onStopSequencesChange 当停止序列变化时调用的回调。
 * @param onFrequencyPenaltyChange 当频率惩罚值变化时调用的回调。
 * @param onPresencePenaltyChange 当存在惩罚值变化时调用的回调。
 * @param onCandidateCountChange 当候选数量变化时调用的回调。
 * @param onSeedChange 当随机种子变化时调用的回调。
 * @param onResponseMimeTypeChange 当 Response MIME Type 变化时调用的回调。
 * @param onResponseLogprobsChange 当是否返回对数概率的开关变化时调用的回调。
 * @param onOutputDimensionalityChange 当嵌入输出维度变化时调用的回调。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerationConfigSection(
    settings: Settings,
    onTemperatureChange: (Float) -> Unit,
    onTopPChange: (Float) -> Unit,
    onTopKChange: (Int?) -> Unit,
    onMaxOutputTokensChange: (Int?) -> Unit,
    onStopSequencesChange: (String) -> Unit,
    onFrequencyPenaltyChange: (Float) -> Unit,
    onPresencePenaltyChange: (Float) -> Unit,
    onCandidateCountChange: (Int) -> Unit,
    onSeedChange: (Int?) -> Unit,
    onResponseMimeTypeChange: (String) -> Unit,
    onResponseLogprobsChange: (Boolean) -> Unit,
    onOutputDimensionalityChange: (Int?) -> Unit
) {
    val outputDimensionalityOptions = listOf("" to "默认", "768" to "768", "1536" to "1536", "3072" to "3072")
    val expanded = remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("生成配置", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            // Temperature 输入框
            OutlinedTextField(
                value = settings.temperature.toString(),
                onValueChange = { newValue ->
                    val floatValue = newValue.toFloatOrNull()
                    if (floatValue != null) {
                        onTemperatureChange(floatValue.coerceIn(0f, 2f))
                    } else if (newValue.isBlank()) {
                        onTemperatureChange(0.0f)
                    }
                },
                label = { Text("Temperature (0.0 - 2.0)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Top P 输入框
            OutlinedTextField(
                value = settings.topP.toString(),
                onValueChange = { newValue ->
                    val floatValue = newValue.toFloatOrNull()
                    if (floatValue != null) {
                        onTopPChange(floatValue.coerceIn(0f, 1f))
                    } else if (newValue.isBlank()) {
                        onTopPChange(0.0f)
                    }
                },
                label = { Text("Top P (0.0 - 1.0)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Top K 输入框
            OutlinedTextField(
                value = settings.topK?.toString() ?: "",
                onValueChange = { newValue ->
                    if (newValue.isNotBlank()) {
                        val intValue = newValue.toIntOrNull()
                        if (intValue != null) {
                            onTopKChange(intValue.coerceAtLeast(1))
                        }
                    } else {
                        onTopKChange(null) // 允许清空
                    }
                },
                label = { Text("Top K (>= 1)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 最大输出 Token 输入框
            OutlinedTextField(
                value = settings.maxOutputTokens?.toString() ?: "",
                onValueChange = { newValue ->
                    if (newValue.isNotBlank()) {
                        val intValue = newValue.toIntOrNull()
                        if (intValue != null) {
                            onMaxOutputTokensChange(intValue.coerceAtLeast(1))
                        }
                    } else {
                        onMaxOutputTokensChange(null) // 允许清空
                    }
                },
                label = { Text("最大输出 Token (>= 1)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Stop Sequences 输入框
            OutlinedTextField(
                value = settings.stopSequences,
                onValueChange = onStopSequencesChange,
                label = { Text("停止序列") },
                placeholder = { Text("例如 stop,end，用逗号分隔") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Response Mime Type 输入框
            OutlinedTextField(
                value = settings.responseMimeType,
                onValueChange = onResponseMimeTypeChange,
                label = { Text("Response MIME Type") },
                placeholder = { Text("例如 application/json") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 输出维度下拉列表
            ExposedDropdownMenuBox(
                expanded = expanded.value,
                onExpandedChange = { expanded.value = !expanded.value }
            ) {
                val selectedLabel = outputDimensionalityOptions.find { it.first == (settings.outputDimensionality?.toString() ?: "") }?.second ?: ""
                OutlinedTextField(
                    value = selectedLabel,
                    onValueChange = {},
                    label = { Text("嵌入输出维度") },
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded.value)
                    },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded.value,
                    onDismissRequest = { expanded.value = false }
                ) {
                    outputDimensionalityOptions.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onOutputDimensionalityChange(value.toIntOrNull())
                                expanded.value = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Frequency Penalty 输入框
            OutlinedTextField(
                value = settings.frequencyPenalty.toString(),
                onValueChange = { newValue ->
                    val floatValue = newValue.toFloatOrNull()
                    if (floatValue != null) {
                        onFrequencyPenaltyChange(floatValue.coerceIn(-2f, Math.nextDown(2.0f)))
                    } else if (newValue.isBlank()) {
                        onFrequencyPenaltyChange(0.0f)
                    }
                },
                label = { Text("频率惩罚 [-2.0, 2.0)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Presence Penalty 输入框
            OutlinedTextField(
                value = settings.presencePenalty.toString(),
                onValueChange = { newValue ->
                    val floatValue = newValue.toFloatOrNull()
                    if (floatValue != null) {
                        onPresencePenaltyChange(floatValue.coerceIn(-2f, Math.nextDown(2.0f)))
                    } else if (newValue.isBlank()) {
                        onPresencePenaltyChange(0.0f)
                    }
                },
                label = { Text("存在惩罚 [-2.0, 2.0)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Candidate Count 输入框
            OutlinedTextField(
                value = settings.candidateCount.toString(),
                onValueChange = { newValue ->
                    val intValue = newValue.toIntOrNull()
                    if (intValue != null) {
                        onCandidateCountChange(intValue.coerceIn(1, 8))
                    }
                },
                label = { Text("候选数量 (1 - 8)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Seed 输入框
            OutlinedTextField(
                value = settings.seed?.toString() ?: "",
                onValueChange = { newValue ->
                    if (newValue.isNotBlank()) {
                        val intValue = newValue.toIntOrNull()
                        if (intValue != null) {
                            onSeedChange(intValue)
                        }
                    } else {
                        onSeedChange(null) // 允许清空
                    }
                },
                label = { Text("随机种子 (整数)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Response Logprobs 开关
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("返回对数概率 (Logprobs)")
                Switch(
                    checked = settings.responseLogprobs,
                    onCheckedChange = onResponseLogprobsChange
                )
            }
        }
    }
}
