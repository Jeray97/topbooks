package com.example.topbooks.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.topbooks.data.preferences.SettingsManager

private val AthenaeumNoirDarkColorScheme = darkColorScheme(
    primary = AthenaeumNoirColors.Primary,
    onPrimary = AthenaeumNoirColors.OnPrimary,
    primaryContainer = AthenaeumNoirColors.PrimaryContainer,
    onPrimaryContainer = AthenaeumNoirColors.OnPrimaryContainer,
    inversePrimary = AthenaeumNoirColors.InversePrimary,
    secondary = AthenaeumNoirColors.Secondary,
    onSecondary = AthenaeumNoirColors.OnSecondary,
    secondaryContainer = AthenaeumNoirColors.SecondaryContainer,
    onSecondaryContainer = AthenaeumNoirColors.OnSecondaryContainer,
    tertiary = AthenaeumNoirColors.Tertiary,
    onTertiary = AthenaeumNoirColors.OnTertiary,
    tertiaryContainer = AthenaeumNoirColors.TertiaryContainer,
    onTertiaryContainer = AthenaeumNoirColors.OnTertiaryContainer,
    background = AthenaeumNoirColors.Background,
    onBackground = AthenaeumNoirColors.OnBackground,
    surface = AthenaeumNoirColors.Surface,
    onSurface = AthenaeumNoirColors.OnSurface,
    surfaceVariant = AthenaeumNoirColors.SurfaceVariant,
    onSurfaceVariant = AthenaeumNoirColors.OnSurfaceVariant,
    surfaceTint = AthenaeumNoirColors.SurfaceTint,
    inverseSurface = AthenaeumNoirColors.InverseSurface,
    inverseOnSurface = AthenaeumNoirColors.InverseOnSurface,
    error = AthenaeumNoirColors.Error,
    onError = AthenaeumNoirColors.OnError,
    errorContainer = AthenaeumNoirColors.ErrorContainer,
    onErrorContainer = AthenaeumNoirColors.OnErrorContainer,
    outline = AthenaeumNoirColors.Outline,
    outlineVariant = AthenaeumNoirColors.OutlineVariant,
    surfaceBright = AthenaeumNoirColors.SurfaceBright,
    surfaceDim = AthenaeumNoirColors.SurfaceDim,
    surfaceContainerLowest = AthenaeumNoirColors.SurfaceContainerLowest,
    surfaceContainerLow = AthenaeumNoirColors.SurfaceContainerLow,
    surfaceContainer = AthenaeumNoirColors.SurfaceContainer,
    surfaceContainerHigh = AthenaeumNoirColors.SurfaceContainerHigh,
    surfaceContainerHighest = AthenaeumNoirColors.SurfaceContainerHighest
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
        useDarkTheme -> AthenaeumNoirDarkColorScheme
        else -> LightColorScheme
    }

    val appColors = if (useDarkTheme) DarkAppColors else LightAppColors

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
