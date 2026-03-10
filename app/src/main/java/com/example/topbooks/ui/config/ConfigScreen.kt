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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.topbooks.R
import com.example.topbooks.data.preferences.SettingsManager
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.ui.theme.ColorArcDarkBrown

val ColorPremiumDivider = Color(0xFFEEEEEE)
val ColorPremiumTextSecondary = Color(0xFF757575)

/**
 * PANTALLA DE CONFIGURACIÓN / AJUSTES (Stateful Composable)
 * * Permite al usuario gestionar sus preferencias locales (modo oscuro, notificaciones),
 * estado de la cuenta (verificación de email, cambio de contraseña, cerrar sesión)
 * y opciones de privacidad.
 *
 * @param onBackClick Acción para volver a la pantalla anterior.
 * @param onSignOut Acción a ejecutar cuando el usuario cierra sesión o elimina su cuenta (navega al Login).
 * @param viewModel ViewModel inyectado que gestiona la lógica y se conecta con [SettingsManager] y repositorios.
 */
@Composable
fun ConfigScreen(
    onBackClick: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: ConfigViewModel = viewModel(factory = ConfigViewModel.Factory(SettingsManager(LocalContext.current)))
) {
    val context = LocalContext.current

    // --- OBSERVACIÓN REACTIVA DE ESTADOS ---
    // Usamos collectAsStateWithLifecycle() para que la UI solo escuche cambios cuando la pantalla es visible,
    // ahorrando batería y recursos del sistema frente al collectAsState() clásico.
    val isEmailVerified by viewModel.isEmailVerified.collectAsStateWithLifecycle()
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val publicJournalDefaultEnabled by viewModel.publicJournalDefaultEnabled.collectAsStateWithLifecycle()

    val isDeleting by viewModel.isDeletingAccount.collectAsStateWithLifecycle(initialValue = false)
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Al entrar a la pantalla, comprobamos silenciosamente si el usuario ha verificado su email
    LaunchedEffect(Unit) {
        viewModel.refreshVerificationStatus()
    }

    Scaffold(
        topBar = { TopBar(onBackClick = onBackClick) },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()) // Permite desplazar el menú si la pantalla es pequeña
        ) {

            // --- TÍTULO PRINCIPAL ---
            Text(
                text = stringResource(R.string.conf_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = ColorArcDarkBrown,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
            )

            // --- AVISO DE VERIFICACIÓN DE EMAIL ---
            // Solo se muestra si Firebase indica que el correo aún no está verificado
            if (!isEmailVerified) {
                VerificationWarningCard(
                    onResendClick = {
                        viewModel.resendVerificationEmail { message ->
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }

            // --- SECCIÓN: APARIENCIA Y USO ---
            ConfigSection(title = stringResource(R.string.conf_use_and_appearance)) {
                ConfigSwitchItem(
                    icon = Icons.Default.Palette,
                    title = stringResource(R.string.conf_dark_mode),
                    description = stringResource(R.string.conf_dark_mode_desc),
                    isChecked = darkModeEnabled,
                    onCheckedChange = { viewModel.toggleDarkMode(it) }
                )
                HorizontalDivider(color = ColorPremiumDivider, modifier = Modifier.padding(horizontal = 16.dp))
                ConfigSwitchItem(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.conf_notifications),
                    description = stringResource(R.string.conf_notifications_desc),
                    isChecked = notificationsEnabled,
                    onCheckedChange = { viewModel.toggleNotifications(it) }
                )
            }

            // --- SECCIÓN: PRIVACIDAD ---
            ConfigSection(title = stringResource(R.string.conf_privacy)) {
                ConfigSwitchItem(
                    icon = Icons.Default.Visibility,
                    title = stringResource(R.string.conf_journalmode),
                    description = stringResource(R.string.conf_journalmode_desc),
                    isChecked = publicJournalDefaultEnabled,
                    onCheckedChange = { viewModel.togglePublicJournalDefault(it) }
                )
            }

            // --- SECCIÓN: CUENTA ---
            ConfigSection(title = stringResource(R.string.conf_account)) {
                ConfigActionItem(
                    icon = Icons.Default.LockReset,
                    title = stringResource(R.string.conf_change_password),
                    description = stringResource(R.string.conf_change_password_desc),
                    onClick = {
                        viewModel.sendPasswordReset { message ->
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    }
                )
                HorizontalDivider(color = ColorPremiumDivider, modifier = Modifier.padding(horizontal = 16.dp))
                ConfigActionItem(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    title = stringResource(R.string.conf_logout),
                    description = stringResource(R.string.conf_logout_desc),
                    onClick = {
                        viewModel.signOut()
                        onSignOut() // Navegamos al login
                    }
                )
            }

            // --- SECCIÓN: INFORMACIÓN ---
            ConfigSection(title = stringResource(R.string.conf_information)) {
                ConfigActionItem(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.conf_privacy_policy),
                    description = stringResource(R.string.conf_privacy_policy_desc),
                    onClick = { Toast.makeText(context, context.getString(R.string.conf_privacy_policy_onClick), Toast.LENGTH_SHORT).show() }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Etiqueta de versión de la app
            Text(
                text = stringResource(R.string.conf_topbooks_version),
                style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 1.sp),
                color = Color.LightGray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- ZONA PELIGROSA: ELIMINAR CUENTA ---
            Text(
                text = stringResource(R.string.conf_danger_zone),
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
                    title = stringResource(R.string.conf_delete_account),
                    description = stringResource(R.string.conf_delete_account_desc),
                    titleColor = Color.Red,
                    iconColor = Color.Red,
                    onClick = { showDeleteDialog = true }
                )
            }
        }

        // --- DIÁLOGO DE ELIMINACIÓN DE CUENTA CON REAUTENTICACIÓN ---
        // Firebase exige re-autenticar al usuario para operaciones destructivas por seguridad.
        if (showDeleteDialog) {
            var passwordConfirm by remember { mutableStateOf("") }
            val isGoogleUser = remember { viewModel.isGoogleUser() }

            AlertDialog(
                onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
                title = { Text(stringResource(R.string.conf_delete_account_dialog_title), fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(stringResource(R.string.conf_delete_account_dialog_body))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Lógica condicional: Si es usuario de Google no le pedimos contraseña,
                        // pero si es usuario de correo tradicional, le exigimos la contraseña actual.
                        if (isGoogleUser) {
                            Text(stringResource(R.string.conf_delete_google_desc), color = Color.Gray, fontSize = 13.sp)
                        } else {
                            Text(stringResource(R.string.conf_delete_password_desc), color = Color.Gray, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = passwordConfirm,
                                onValueChange = { passwordConfirm = it },
                                label = { Text(stringResource(R.string.conf_delete_password_label)) },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Red,
                                    focusedLabelColor = Color.Red
                                )
                            )
                        }
                    }
                },
                containerColor = Color.White,
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.reauthenticateAndDelete(passwordConfirm) { success, messageResId ->
                                showDeleteDialog = false
                                Toast.makeText(context, context.getString(messageResId), Toast.LENGTH_LONG).show()
                                if (success) {
                                    onSignOut() // Solo cerramos la sesión en el dispositivo si el borrado en la nube triunfó
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        // Bloqueo de seguridad: El botón solo funciona si se ha escrito la contraseña o es usuario de Google
                        enabled = (!isGoogleUser && passwordConfirm.isNotEmpty() && !isDeleting) || (isGoogleUser && !isDeleting)
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.conf_delete_account_dialog_yes))
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false },
                        enabled = !isDeleting
                    ) {
                        Text(stringResource(R.string.conf_delete_account_dialog_no), color = ColorPremiumTextSecondary)
                    }
                }
            )
        }
    }
}

// =========================================================================================
// --- COMPONENTES SECUNDARIOS (STATELESS) ---
// =========================================================================================

/**
 * Tarjeta de advertencia visual que insta al usuario a verificar su correo electrónico.
 */
@Composable
fun VerificationWarningCard(onResendClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFFFF3E0)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFFE65100))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.conf_email_not_verified_title), style = MaterialTheme.typography.titleMedium, color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                Text(text = stringResource(R.string.conf_email_not_verified_desc), style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                TextButton(onClick = onResendClick, contentPadding = PaddingValues(0.dp)) {
                    Text(stringResource(R.string.conf_email_resend_button), fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                }
            }
        }
    }
}

/**
 * Contenedor visual estandarizado para agrupar ajustes bajo un mismo título temático (ej: Privacidad).
 */
@Composable
fun ConfigSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ColorArcDarkBrown, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White), elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)) {
            Column(content = content)
        }
    }
}

/**
 * Fila interactiva para acciones directas (ej: "Cambiar contraseña", "Cerrar sesión").
 * Muestra una flecha hacia la derecha (Chevron) indicando interactividad.
 */
@Composable
fun ConfigActionItem(icon: ImageVector, title: String, description: String, titleColor: Color = ColorArcDarkBrown, iconColor: Color = ColorArcDarkBrown, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(24.dp), tint = iconColor)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = titleColor, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = ColorPremiumTextSecondary)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.LightGray)
    }
}

/**
 * Fila interactiva para ajustes de tipo encendido/apagado (Booleanos).
 * Incorpora un interruptor (Switch) en la parte derecha sincronizado con el estado local de DataStore.
 */
@Composable
fun ConfigSwitchItem(icon: ImageVector, title: String, description: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!isChecked) }.padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(24.dp), tint = ColorArcDarkBrown)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = ColorArcDarkBrown, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = ColorPremiumTextSecondary)
        }
        Switch(checked = isChecked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ColorArcDarkBrown, uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFE0E0E0), uncheckedBorderColor = Color.Transparent))
    }
}