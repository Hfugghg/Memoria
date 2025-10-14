package com.exp.memoria

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.exp.memoria.ui.theme.MemoriaTheme
import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * [应用入口]
 *
 * 职责:
 * 1. 作为整个应用的入口点和全局上下文提供者。
 * 2. 初始化Hilt依赖注入框架。所有应用级别的依赖项将从这里开始注入。
 * 3. (可选) 在未来可以用于初始化应用级的单例，如日志库、监控工具等。
 *
 * 关联:
 * - @HiltAndroidApp 注解是Hilt的起点，使得所有被@AndroidEntryPoint注解的Activity, Fragment, ViewModel等都能被注入依赖。
 * - 必须在 AndroidManifest.xml 的 <application> 标签中通过 android:name=".MemoriaApplication" 进行注册。
 */

@HiltAndroidApp
class MemoriaApplication : Application() {
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MemoriaTheme {
        Greeting("Android")
    }
}