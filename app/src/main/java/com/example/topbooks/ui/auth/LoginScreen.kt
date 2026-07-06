@file:Suppress("DEPRECATION")

package com.example.topbooks.ui.auth

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.topbooks.R
import com.example.topbooks.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

/**
 * PANTALLA PRINCIPAL DE LOGIN (Stateful Composable)
 * * Gestiona la conexión con el [AuthViewModel], observa los estados y maneja eventos
 * complejos del sistema como los Toasts de error y el lanzador de Google Sign-In.
 *
 * @param viewModel ViewModel de autenticación que provee la lógica de negocio.
 * @param onLoginSuccess Callback que se ejecuta cuando el usuario inicia sesión correctamente para navegar a la Home.
 * @param onNavigateToRegister Callback para navegar a la pantalla de crear una cuenta nueva.
 */
@Suppress("DEPRECATION")
@Composable
fun LoginScreen(
    viewModel: AuthViewModel = viewModel(),
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current

    // Observamos el estado emitido por el ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // 1. GESTIÓN DE ERRORES: Si hay un error, mostramos un Toast y lo limpiamos
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, context.getString(it), Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    // 2. CONFIGURACIÓN DE GOOGLE SIGN-IN
    // Preparamos el cliente pidiendo el Email y usando el Web Client ID de tu proyecto Firebase
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    // Lanzador (Launcher) que abre la ventana emergente de Google para elegir cuenta
    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { token ->
                    viewModel.loginWithGoogle(token, onLoginSuccess)
                }
            } catch (e: ApiException) {
                Toast.makeText(context, context.getString(R.string.login_error_google, e.statusCode), Toast.LENGTH_LONG).show()
            }
        }
    }

    // 3. RENDERIZAMOS LA INTERFAZ PURA
    LoginContent(
        isLoading = uiState.isAuthenticating,
        onLoginClick = { email, pass -> viewModel.login(email, pass, onLoginSuccess) },
        onGoogleClick = { googleLauncher.launch(googleSignInClient.signInIntent) },
        onNavigateToRegister = onNavigateToRegister,
        viewModel = viewModel
    )
}

/**
 * INTERFAZ VISUAL DEL LOGIN (Stateless Composable)
 * * Contiene únicamente los elementos de UI (Cajas de texto, botones, fondos).
 * No contiene lógica de negocio directa, sino que delega las acciones mediante callbacks.
 */
@Composable
fun LoginContent(
    isLoading: Boolean,
    onLoginClick: (String, String) -> Unit,
    onGoogleClick: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel
) {
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        var resetEmail by remember { mutableStateOf(email) }
        var isSending by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.login_reset_title), fontFamily = GuardianCity, fontWeight = FontWeight.Normal, color = LoginColors.Primary) },
            text = {
                Column {
                    Text(stringResource(R.string.login_reset_desc), color = LoginColors.OnSurfaceVariant, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text(stringResource(R.string.login_field_email)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LoginColors.SecondaryContainer,
                            focusedLabelColor = LoginColors.SecondaryContainer,
                            unfocusedBorderColor = LoginColors.OutlineVariant
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSending = true
                        viewModel.resetPassword(resetEmail) { success, msg ->
                            isSending = false
                            showResetDialog = false
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = resetEmail.isNotBlank() && !isSending,
                    colors = ButtonDefaults.buttonColors(containerColor = LoginColors.Primary),
                    shape = RoundedCornerShape(50)
                ) {
                    if (isSending) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = LoginColors.OnPrimary, strokeWidth = 2.dp)
                    else Text(stringResource(R.string.login_action_send_link), color = LoginColors.OnPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text(stringResource(R.string.login_action_cancel), color = LoginColors.OnSurfaceVariant) }
            },
            containerColor = LoginColors.Surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LoginColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            color = LoginColors.Surface,
            shadowElevation = 4.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, LoginColors.OutlineVariant.copy(alpha = 0.3f))
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    LoginColors.PrimaryContainer,
                                    LoginColors.Primary,
                                    LoginColors.PrimaryContainer
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 40.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(LoginColors.SurfaceContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = LoginColors.Primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.app_name),
                        fontFamily = GuardianCity,
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp,
                        lineHeight = 52.sp,
                        color = LoginColors.Primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.login_welcome_prefix),
                        fontSize = 16.sp,
                        color = LoginColors.OnSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    Text(
                        text = stringResource(R.string.login_field_email),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LoginColors.OnSurface,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("scholar@example.com", color = LoginColors.Outline) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = LoginColors.OutlineVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LoginColors.SecondaryContainer,
                            unfocusedBorderColor = LoginColors.OutlineVariant,
                            focusedContainerColor = LoginColors.Surface,
                            unfocusedContainerColor = LoginColors.Surface
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.login_field_password),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LoginColors.OnSurface
                        )
                        Text(
                            text = stringResource(R.string.login_forgot_password),
                            fontSize = 14.sp,
                            color = LoginColors.Primary,
                            modifier = Modifier.clickable { showResetDialog = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("••••••••", color = LoginColors.Outline) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = LoginColors.OutlineVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        singleLine = true,
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = null, tint = LoginColors.OutlineVariant, modifier = Modifier.size(20.dp))
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LoginColors.SecondaryContainer,
                            unfocusedBorderColor = LoginColors.OutlineVariant,
                            focusedContainerColor = LoginColors.Surface,
                            unfocusedContainerColor = LoginColors.Surface
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            color = LoginColors.Primary,
                            strokeWidth = 4.dp
                        )
                    } else {
                        Button(
                            onClick = { onLoginClick(email, password) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = email.isNotEmpty() && password.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LoginColors.Primary,
                                contentColor = LoginColors.OnPrimary
                            ),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(stringResource(R.string.login_btn_submit), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = LoginColors.OutlineVariant)
                            Text(
                                text = stringResource(R.string.login_google_desc),
                                modifier = Modifier.padding(horizontal = 8.dp),
                                fontSize = 12.sp,
                                color = LoginColors.OnSurfaceVariant
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = LoginColors.OutlineVariant)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedButton(
                            onClick = onGoogleClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = LoginColors.Surface),
                            shape = RoundedCornerShape(50),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LoginColors.OutlineVariant)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_google),
                                contentDescription = stringResource(R.string.login_google_desc),
                                tint = Color.Unspecified,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.login_btn_google), color = LoginColors.OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.login_no_account), color = LoginColors.OnSurfaceVariant, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.login_register_here),
                            color = LoginColors.Primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { onNavigateToRegister() }
                        )
                    }
                }
            }
        }
    }
}