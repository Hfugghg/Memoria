package com.exp.memoria

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.exp.memoria.ui.chat.ChatScreen
import com.exp.memoria.ui.chat.ConversationHistoryScreen
import com.exp.memoria.ui.settings.settingsscreen.SettingsScreen
import com.exp.memoria.ui.theme.MemoriaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MemoriaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MemoriaApp()
                }
            }
        }
    }
}

@Composable
fun MemoriaApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "conversationHistory") { // 将 startDestination 改为 "conversationHistory"
        composable(
            route = "chat?conversationId={conversationId}", // 定义带参数的路由
            arguments = listOf(navArgument("conversationId") { // 定义参数
                type = NavType.StringType
                nullable = true // 允许 conversationId 为空，以便首次创建对话时使用默认值
            })
        ) { backStackEntry ->
            // 从 NavBackStackEntry 中获取参数
            val conversationId = backStackEntry.arguments?.getString("conversationId")
            ChatScreen(navController = navController, conversationId = conversationId)
        }
        composable(
            route = "settings/{conversationId}",
            arguments = listOf(navArgument("conversationId") {
                type = NavType.StringType
            })
        ) {
            SettingsScreen()
        }
        composable("conversationHistory") {
            ConversationHistoryScreen(navController = navController)
        }
    }
}
