package com.example.topbooks.ui.config

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.topbooks.ui.components.TopBar
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.topbooks.ui.theme.ColorArcDarkBrown
import com.example.topbooks.ui.theme.ColorArcMediumBrown
import com.example.topbooks.ui.theme.ColorHeaderBeige


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    onNavigateToAbout: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToLanguage: () -> Unit = {},
    onToggleNotifications: (Boolean) -> Unit = {},
    onToggleTheme: (Boolean) -> Unit = {}
) {
    // Estas son las ÚNICAS variables de estado que necesitas para toda la pantalla
    var notificationsEnabled by remember { mutableStateOf(true) }
    var darkModeEnabled by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopBar(onBackClick = {}) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Preferencias Generales",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            ConfigItem(
                icon = Icons.Default.Star,
                title = "Idioma",
                description = "Cambiar el idioma de la aplicación",
                onClick = onNavigateToLanguage
            )

            // Pasamos el valor (isChecked) y la acción (onCheckedChange)
            ConfigSwitchItem(
                icon = Icons.Default.Notifications,
                title = "Notificaciones",
                description = "Recibir notificaciones sobre novedades",
                isChecked = notificationsEnabled,
                onCheckedChange = {
                    notificationsEnabled = it
                    onToggleNotifications(it)
                }
            )

            ConfigSwitchItem(
                icon = Icons.Default.Info,
                title = "Modo Oscuro",
                description = "Activar o desactivar el tema oscuro",
                isChecked = darkModeEnabled,
                onCheckedChange = {
                    darkModeEnabled = it
                    onToggleTheme(it)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Información y Legal",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            HorizontalDivider()

            ConfigItem(
                icon = Icons.Default.Lock,
                title = "Política de Privacidad",
                description = "Ver nuestra política de privacidad",
                onClick = onNavigateToPrivacy
            )
            ConfigItem(
                icon = Icons.Default.Info,
                title = "Acerca de TopBooks",
                description = "Información sobre la aplicación y la versión",
                onClick = onNavigateToAbout
            )
        }
    }
}

@Composable
fun ConfigItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    // ELIMINADAS: Aquí no debe haber variables de estado
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(28.dp), tint = ColorArcDarkBrown)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
    }
}

@Composable
fun ConfigSwitchItem(
    icon: ImageVector,
    title: String,
    description: String,
    isChecked: Boolean, // Recibimos el valor desde el padre
    onCheckedChange: (Boolean) -> Unit // Avisamos al padre del cambio
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(28.dp), tint = ColorArcDarkBrown)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ColorArcDarkBrown,
                checkedTrackColor = ColorArcMediumBrown.copy(alpha = 0.5f),
                uncheckedTrackColor = ColorHeaderBeige
            )
        )
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewConfigScreen() {
    ConfigScreen()
}