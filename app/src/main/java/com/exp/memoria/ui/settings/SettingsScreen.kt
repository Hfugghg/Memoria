package com.exp.memoria.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settingsState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
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
        }
    }
}