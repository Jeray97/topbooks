package com.example.topbooks.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.topbooks.data.preferences.SettingsManager

private val DarkColorScheme = darkColorScheme(
    primary = DarkThemeColors.ArcDarkBrown,
    secondary = DarkThemeColors.ArcMediumBrown,
    tertiary = DarkThemeColors.JournalRomance,
    background = DarkThemeColors.Background,
    surface = DarkThemeColors.ComponentBackground,
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFFFFFFFF),
    onBackground = DarkThemeColors.TextPrimary,
    onSurface = DarkThemeColors.TextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = LightThemeColors.ArcDarkBrown,
    secondary = LightThemeColors.ArcMediumBrown,
    tertiary = LightThemeColors.JournalRomance,
    background = LightThemeColors.Background,
    surface = LightThemeColors.ComponentBackground,
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFFFFFFFF),
    onBackground = LightThemeColors.TextPrimary,
    onSurface = LightThemeColors.TextPrimary
)

@Composable
fun TopBooksTheme(
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val settingsManager = remember { SettingsManager(context) }

    val isDarkModeEnabledByUser by settingsManager.darkModeFlow.collectAsState(initial = isSystemInDarkTheme())

    val useDarkTheme = isDarkModeEnabledByUser

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
