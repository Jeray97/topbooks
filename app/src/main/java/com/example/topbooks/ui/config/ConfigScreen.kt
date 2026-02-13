package com.example.topbooks.ui.config

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.ui.theme.ColorArcDarkBrown
import com.example.topbooks.ui.theme.ColorArcMediumBrown
import com.example.topbooks.ui.theme.ColorHeaderBeige

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    viewModel: ConfigViewModel,
    onLogoutSuccess: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToLanguage: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopBar(onBackClick = onBackClick) }
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

            ConfigSwitchItem(
                icon = Icons.Default.Notifications,
                title = "Notificaciones",
                description = "Recibir notificaciones sobre novedades",
                isChecked = notificationsEnabled,
                onCheckedChange = { viewModel.toggleNotifications(it) }
            )

            ConfigSwitchItem(
                icon = Icons.Default.Info,
                title = "Modo Oscuro",
                description = "Activar o desactivar el tema oscuro",
                isChecked = darkModeEnabled,
                onCheckedChange = { viewModel.toggleDarkMode(it) }
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

            // Espaciador flexible para empujar el botón al fondo
            Spacer(modifier = Modifier.weight(1f))

            // Botón de Cerrar Sesión
            Button(
                onClick = {
                    viewModel.signOut()
                    onLogoutSuccess()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Red
                ),
                elevation = null
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cerrar Sesión",
                    fontWeight = FontWeight.Bold
                )
            }
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
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
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