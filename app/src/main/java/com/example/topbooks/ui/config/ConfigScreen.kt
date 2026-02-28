package com.example.topbooks.ui.config

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.topbooks.data.preferences.SettingsManager
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.ui.theme.ColorArcDarkBrown

// Definimos unos colores grises sutiles para el diseño premium
val ColorPremiumDivider = Color(0xFFEEEEEE)
val ColorPremiumTextSecondary = Color(0xFF757575)

@Composable
fun ConfigScreen(
    onBackClick: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: ConfigViewModel = viewModel(factory = ConfigViewModel.Factory(SettingsManager(LocalContext.current)))
) {
    val context = LocalContext.current

    // Observamos las preferencias y estados del ViewModel
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val publicJournalDefaultEnabled by viewModel.publicJournalDefaultEnabled.collectAsStateWithLifecycle()

    // Estado para la eliminación de cuenta
    val isDeleting by viewModel.isDeletingAccount.collectAsStateWithLifecycle(initialValue = false)
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopBar(onBackClick = onBackClick) },
        containerColor = Color.White // Fondo Blanco Puro
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {

            // Título principal de la pantalla
            Text(
                text = "Configuración",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = ColorArcDarkBrown,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
            )

            // --- SECCIÓN: APARIENCIA Y USO ---
            ConfigSection(title = "Apariencia y Uso") {
                ConfigSwitchItem(
                    icon = Icons.Default.Palette,
                    title = "Modo Oscuro",
                    description = "Cambia el tema visual de la aplicación",
                    isChecked = darkModeEnabled,
                    onCheckedChange = { viewModel.toggleDarkMode(it) }
                )
                HorizontalDivider(color = ColorPremiumDivider, modifier = Modifier.padding(horizontal = 16.dp))
                ConfigSwitchItem(
                    icon = Icons.Default.Notifications,
                    title = "Notificaciones",
                    description = "Recibe avisos de likes y comentarios",
                    isChecked = notificationsEnabled,
                    onCheckedChange = { viewModel.toggleNotifications(it) }
                )
            }

            // --- SECCIÓN: PRIVACIDAD ---
            ConfigSection(title = "Privacidad") {
                ConfigSwitchItem(
                    icon = Icons.Default.Visibility,
                    title = "Diarios públicos por defecto",
                    description = "Tus nuevas lecturas serán visibles para todos",
                    isChecked = publicJournalDefaultEnabled,
                    onCheckedChange = { viewModel.togglePublicJournalDefault(it) }
                )
            }

            // --- SECCIÓN: CUENTA ---
            ConfigSection(title = "Cuenta") {
                ConfigActionItem(
                    icon = Icons.Default.LockReset,
                    title = "Cambiar Contraseña",
                    description = "Te enviaremos un correo para restablecerla",
                    onClick = {
                        viewModel.sendPasswordReset { message ->
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    }
                )
                HorizontalDivider(color = ColorPremiumDivider, modifier = Modifier.padding(horizontal = 16.dp))
                ConfigActionItem(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    title = "Cerrar Sesión",
                    description = "Desconecta tu cuenta de este dispositivo",
                    onClick = {
                        viewModel.signOut()
                        onSignOut()
                    }
                )
            }

            // --- SECCIÓN: INFORMACIÓN ---
            ConfigSection(title = "Información") {
                ConfigActionItem(
                    icon = Icons.Default.Info,
                    title = "Política de Privacidad",
                    description = "Conoce cómo tratamos tus datos",
                    onClick = { Toast.makeText(context, "Abrir enlace...", Toast.LENGTH_SHORT).show() }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Versión de la app
            Text(
                text = "TopBooks v1.0.0",
                style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 1.sp),
                color = Color.LightGray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- ZONA DE PELIGRO (Al final del to-do) ---
            Text(
                text = "Zona de Peligro",
                style = MaterialTheme.typography.titleSmall,
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                ConfigActionItem(
                    icon = Icons.Default.DeleteForever,
                    title = "Eliminar Cuenta",
                    description = "Borra permanentemente tus datos de TopBooks",
                    titleColor = Color.Red,
                    iconColor = Color.Red,
                    onClick = { showDeleteDialog = true }
                )
            }
        }

        // --- DIÁLOGO DE CONFIRMACIÓN DE BORRADO ---
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
                title = { Text("¿Eliminar cuenta?", fontWeight = FontWeight.Bold) },
                text = { Text("Esta acción no se puede deshacer. Se perderán todos tus diarios, listas y comentarios. ¿Estás seguro?") },
                containerColor = Color.White,
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteAccount { success, message ->
                                showDeleteDialog = false
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                if (success) {
                                    onSignOut()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        enabled = !isDeleting
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Sí, eliminar")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false },
                        enabled = !isDeleting
                    ) {
                        Text("Cancelar", color = ColorPremiumTextSecondary)
                    }
                }
            )
        }
    }
}

// --- COMPONENTES REUTILIZABLES CON DISEÑO PREMIUM ---

@Composable
fun ConfigSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = ColorArcDarkBrown,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun ConfigActionItem(
    icon: ImageVector,
    title: String,
    description: String,
    titleColor: Color = ColorArcDarkBrown,
    iconColor: Color = ColorArcDarkBrown,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(24.dp), tint = iconColor)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = titleColor, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = ColorPremiumTextSecondary)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.LightGray)
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
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(24.dp), tint = ColorArcDarkBrown)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = ColorArcDarkBrown, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = ColorPremiumTextSecondary)
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ColorArcDarkBrown,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE0E0E0),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}