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
import androidx.compose.ui.platform.LocalContext
import com.example.topbooks.data.preferences.SettingsManager

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
    // Aquí definirás tus colores oscuros en el futuro (ej: background = ColorDarkGray)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
    // Aquí puedes enlazar tus colores diurnos si decides usar MaterialTheme.colorScheme
)

@Composable
fun TopBooksTheme(
    // Dinamic color adapta los colores al fondo de pantalla del usuario (Android 12+).
    // Lo pongo en 'false' por defecto para que respeten colores marrones y beige de la marca.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // 1. Instanciamos el SettingsManager para leer las preferencias
    val settingsManager = remember { SettingsManager(context) }

    // 2. Leemos la preferencia del usuario en tiempo real.
    // Si no ha elegido nada, usamos la preferencia del sistema por defecto.
    val isDarkModeEnabledByUser by settingsManager.darkModeFlow.collectAsState(initial = isSystemInDarkTheme())

    // ------------------------------------------------------------------
    // INTERRUPTOR DE SEGURIDAD: MODO OSCURO EN CONSTRUCCIÓN
    // ------------------------------------------------------------------
    // Actualmente forzamos 'false' para que siempre se vea el modo diurno.
    // val useDarkTheme = isDarkModeEnabledByUser
    val useDarkTheme = false
    // ------------------------------------------------------------------

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