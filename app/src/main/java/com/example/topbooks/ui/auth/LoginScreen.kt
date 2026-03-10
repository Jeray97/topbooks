package com.example.topbooks.ui.auth

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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

    // Estados locales para controlar lo que el usuario escribe
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) } // Ojo/Toggle de la contraseña

    val scrollState = rememberScrollState()

    // Estado para el cuadro de diálogo de recuperar contraseña
    var showResetDialog by remember { mutableStateOf(false) }

    // --- DIÁLOGO DE RECUPERAR CONTRASEÑA ---
    if (showResetDialog) {
        var resetEmail by remember { mutableStateOf(email) } // Pre-rellena con el email que ya escribió
        var isSending by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.login_reset_title), fontFamily = GuardianCity, fontWeight = FontWeight.Normal) },
            text = {
                Column {
                    Text(stringResource(R.string.login_reset_desc), color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text(stringResource(R.string.login_field_email)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ColorArcMediumBrown, focusedLabelColor = ColorArcMediumBrown)
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
                    colors = ButtonDefaults.buttonColors(containerColor = ColorArcMediumBrown)
                ) {
                    if (isSending) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text(stringResource(R.string.login_action_send_link))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text(stringResource(R.string.login_action_cancel), color = Color.Gray) }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // --- ESTRUCTURA PRINCIPAL DE LA PANTALLA ---
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

        // 1. Imagen de fondo que ocupa toda la pantalla
        Image(
            painter = painterResource(id = R.drawable.login_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. Columna con los elementos superpuestos (Scrollable para pantallas pequeñas)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, top = 60.dp, end = 24.dp, bottom = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- HEADER (Bienvenida y Logo Textual) ---
            Text(
                text = stringResource(R.string.login_welcome_prefix),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 24.sp,
                    fontFamily = GuardianCity
                ),
                color = ColorTituloCategoriaDetalle,
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 40.sp,
                        fontFamily = GuardianCity
                    ),
                    color = ColorTituloCategoriaDetalle
                )
            }

            Spacer(modifier = Modifier.weight(0.3f))

            // --- FORMULARIO (Email y Password) ---
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.login_field_email)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.8f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.8f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.login_field_password)) },
                modifier = Modifier.fillMaxWidth(),
                // Alterna entre mostrar puntitos (VisualTransformation) o el texto plano
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                singleLine = true,
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = null, tint = ColorArcMediumBrown)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.8f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.8f)
                )
            )

            // --- BOTÓN RECUPERAR CONTRASEÑA ---
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Text(
                    text = stringResource(R.string.login_forgot_password),
                    color = Color.DarkGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clickable { showResetDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- BOTONES DE ACCIÓN (Login normal y Google) ---
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = ColorArcMediumBrown,
                    strokeWidth = 4.dp
                )
            } else {
                Button(
                    onClick = { onLoginClick(email, password) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = email.isNotEmpty() && password.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorArcMediumBrown)
                ) {
                    Text(stringResource(R.string.login_btn_submit))
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onGoogleClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = stringResource(R.string.login_google_desc),
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.login_btn_google), color = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- FOOTER (Ir a Registro) ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.login_no_account), color = Color.Black)
                Text(
                    text = stringResource(R.string.login_register_here),
                    color = ColorArcDarkBrown,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.clickable { onNavigateToRegister() }
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}