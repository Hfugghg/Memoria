package com.exp.memoria

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.exp.memoria.ui.chat.ChatScreen
import com.exp.memoria.ui.chat.ConversationHistoryScreen
import com.exp.memoria.ui.settings.SettingsScreen
import com.exp.memoria.ui.theme.MemoriaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MemoriaTheme {
                MemoriaApp()
            }
        }
    }
}

@Composable
fun MemoriaApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "chat") {
        composable("chat") {
            ChatScreen(navController = navController)
        }
        composable("settings") {
            SettingsScreen()
        }
        composable("conversationHistory") {
            ConversationHistoryScreen(navController = navController)
        }
    }
}
