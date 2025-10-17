package com.exp.memoria.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * 应用的暗色主题颜色方案。
 * 使用在 Color.kt 中定义的颜色。
 */
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

/**
 * 应用的亮色主题颜色方案。
 * 使用在 Color.kt 中定义的颜色。
 */
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* 其他可以覆盖的默认颜色
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

/**
 * [应用主题 Composable]
 *
 * 这是应用的主要主题函数，应该在 UI 的根部被调用，用于包裹所有界面内容。
 * 它负责根据系统设置和设备能力，选择并应用正确的颜色方案和排版样式。
 *
 * @param darkTheme 如果为 true，则应用暗色主题；默认为跟随系统设置。
 * @param dynamicColor 如果为 true，并且设备运行在 Android 12+ 上，则会使用动态颜色（从壁纸中提取颜色）。
 * @param content 需要应用此主题的可组合内容。
 */
@Composable
fun MemoriaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // 动态颜色仅在 Android 12+ 上可用
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
