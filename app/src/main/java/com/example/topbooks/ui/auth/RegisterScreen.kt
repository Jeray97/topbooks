package com.example.topbooks.ui.auth

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.topbooks.R
import com.example.topbooks.ui.theme.*

/**
 * PANTALLA PRINCIPAL DE REGISTRO (Stateful Composable)
 * * Gestiona la conexión con el [AuthViewModel] y los eventos de un solo uso (Side Effects)
 * como mostrar un Toast cuando hay un error o cuando el registro es exitoso.
 *
 * @param viewModel Proveedor de la lógica de autenticación.
 * @param onRegisterSuccess Callback que navega al flujo de bienvenida (Tutorial/Onboarding) tras registrarse.
 * @param onNavigateToLogin Callback para volver a la pantalla de Login si el usuario ya tiene cuenta.
 */
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel = viewModel(),
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // 1. OBSERVADOR DE ERRORES: Muestra un Toast si Firebase devuelve fallo (ej. Email duplicado)
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, context.getString(it), Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    // 2. RENDERIZADO VISUAL
    RegisterContent(
        isLoading = uiState.isAuthenticating,
        onRegisterClick = { name, email, pass ->
            viewModel.register(name, email, pass) {
                Toast.makeText(context, context.getString(R.string.register_toast_success), Toast.LENGTH_LONG).show()
                onRegisterSuccess()
            }
        },
        onNavigateToLogin = onNavigateToLogin
    )
}

/**
 * INTERFAZ VISUAL DEL REGISTRO (Stateless Composable)
 * * Contiene la lógica visual de validación de contraseñas y el formulario puro.
 */
@Composable
fun RegisterContent(
    isLoading: Boolean,
    onRegisterClick: (String, String, String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    // --- ESTADOS LOCALES DEL FORMULARIO ---
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Estados para controlar la visibilidad (ojo tachado/abierto) de ambas contraseñas
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // --- REGLAS DE VALIDACIÓN EN TIEMPO REAL ---
    // Estas variables se recalculan automáticamente cada vez que 'password' cambia
    val hasUpperCase = remember(password) { password.any { it.isUpperCase() } }
    val hasNumber = remember(password) { password.any { it.isDigit() } }
    val hasSpecialChar = remember(password) { password.any { !it.isLetterOrDigit() } }
    val isLengthValid = remember(password) { password.length >= 6 }
    val isPasswordValid = hasUpperCase && hasNumber && hasSpecialChar && isLengthValid

    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

        // 1. Imagen de fondo
        Image(
            painter = painterResource(id = R.drawable.register_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. Contenedor principal con Scroll
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, top = 60.dp, end = 24.dp, bottom = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(0.3f))

            // --- HEADER (Título) ---
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ColorSurfaceTextRegister,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.register_title),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 30.sp,
                        fontFamily = CenturyGotic
                    ),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.weight(0.35f))

            // --- FORMULARIO ---

            // Campo Nombre
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.register_field_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.8f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.8f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.register_field_email)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.8f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.8f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo Contraseña
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.register_field_password)) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = null, tint = ColorArcMediumBrown)
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.8f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.8f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo Confirmar Contraseña
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text(stringResource(R.string.register_field_confirm_password)) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(imageVector = image, contentDescription = null, tint = ColorArcMediumBrown)
                    }
                },
                singleLine = true,
                // Feedback visual: Se pone en rojo si las contraseñas no coinciden
                isError = password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.8f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.8f)
                )
            )

            // --- PANEL DE REQUISITOS DE CONTRASEÑA ---
            // Solo se muestra si el usuario ha empezado a escribir la contraseña
            if (password.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = stringResource(R.string.register_pwd_req_title),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorArcDarkBrown,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        PasswordReqItem(text = stringResource(R.string.register_pwd_req_length), isMet = isLengthValid)
                        PasswordReqItem(text = stringResource(R.string.register_pwd_req_uppercase), isMet = hasUpperCase)
                        PasswordReqItem(text = stringResource(R.string.register_pwd_req_number), isMet = hasNumber)
                        PasswordReqItem(text = stringResource(R.string.register_pwd_req_special), isMet = hasSpecialChar)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- BOTÓN DE REGISTRO ---
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = ColorArcMediumBrown,
                    strokeWidth = 4.dp
                )
            } else {
                Button(
                    onClick = { onRegisterClick(name, email, password) },
                    modifier = Modifier.fillMaxWidth(),
                    // Bloqueo inteligente: El botón solo se activa si TODOS los requisitos se cumplen
                    enabled = name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && password == confirmPassword && isPasswordValid,
                    colors = ButtonDefaults.buttonColors(containerColor = ColorArcMediumBrown)
                ) {
                    Text(stringResource(R.string.register_btn_submit))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- FOOTER (Ir a Login) ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.register_already_have_account), color = Color.Black)
                Text(
                    text = stringResource(R.string.register_login_here),
                    color = ColorArcDarkBrown,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/**
 * Micro-componente visual para mostrar una regla de contraseña específica.
 * * Cambia de un aspa gris a un tick verde/color de éxito cuando la regla se cumple.
 *
 * @param text Texto de la regla (Ej: "Al menos 6 caracteres").
 * @param isMet Booleano que indica si la regla se está cumpliendo actualmente.
 */
@Composable
fun PasswordReqItem(text: String, isMet: Boolean) {
    val color = if (isMet) ColorConditionOk else Color.DarkGray
    val icon = if (isMet) Icons.Default.Check else Icons.Default.Close

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = color, fontSize = 12.sp)
    }
}