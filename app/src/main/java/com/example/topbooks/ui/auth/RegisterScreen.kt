package com.example.topbooks.ui.auth

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.topbooks.R
import com.example.topbooks.utils.Resource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close

// --- 1. COMPONENTE CON LÓGICA (Stateful) ---
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel = viewModel(),
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(authState) {
        when (val state = authState) {
            is Resource.Success -> {
                viewModel.clearState()
                onRegisterSuccess()
            }
            is Resource.Error -> {
                Toast.makeText(context, state.exception.message, Toast.LENGTH_LONG).show()
                viewModel.clearState()
            }
            else -> {}
        }
    }

    RegisterContent(
        isLoading = authState is Resource.Loading,
        onRegisterClick = { name, email, pass -> viewModel.register(name, email, pass) },
        onNavigateToLogin = onNavigateToLogin
    )
}

// --- 2. COMPONENTE DE DISEÑO PURO (Stateless) ---
@Composable
fun RegisterContent(
    isLoading: Boolean,
    onRegisterClick: (String, String, String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }


    //Lógica para validación en tiempo real
    val hasUpperCase = remember(password) { password.any {it.isUpperCase() } }
    val hasNumber = remember(password) { password.any { it.isDigit() } }
    val hasSpecialChar = remember(password) { password.any { !it.isLetterOrDigit() } }
    val isLengthValid = remember(password) { password.length >= 6 }

    //Si cumple to-do, la contraseña es válida
    val isPasswordValid = hasUpperCase && hasNumber && hasSpecialChar && isLengthValid


    // Usamos un Box para superponer elementos
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 1. Imagen de Fondo
        Image(
            painter = painterResource(id = R.drawable.register_bg), // Asegúrate de que el nombre coincida
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. Contenido Principal
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()), // Hacer scroll si el contenido es muy largo en pantallas pequeñas
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Campo Nombre
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre completo") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo electrónico") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo Contraseña
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo Confirmar Contraseña
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirmar Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                singleLine = true,
                isError = password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword
            )

            // Solo los mostramos si ha empezado a escribir
            if (password.isNotEmpty() || confirmPassword.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("La contraseña debe contener:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    PasswordRequirementItem(text = "Mínimo 6 caracteres", isMet = isLengthValid)
                    PasswordRequirementItem(text = "Al menos 1 mayúscula", isMet = hasUpperCase)
                    PasswordRequirementItem(text = "Al menos 1 número", isMet = hasNumber)
                    PasswordRequirementItem(text = "Al menos 1 carácter especial (@#$%)", isMet = hasSpecialChar)
                }
            }


            if (password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword) {
                Text("Las contraseñas no coinciden", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { onRegisterClick(name, email, password) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && password == confirmPassword && isPasswordValid
                ) {
                    Text("Registrarse")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("¿Ya tienes cuenta? ", color = Color.Black)
                Text(
                    text = "Inicia sesión aquí",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }
        }
    }
}

//Auxiliar para comprobar requisitos password
@Composable
fun PasswordRequirementItem(text: String, isMet: Boolean) {
    val color = if (isMet) Color.Green else Color.Gray //Verde si cumple, gris si no
    val icon = if (isMet) Icons.Default.Check else Icons.Default.Close

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = text,
            color = color,
            fontSize = 12.sp
        )
    }
}
// --- 3. PREVISUALIZACIÓN ---
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RegisterScreenPreview() {
    MaterialTheme {
        RegisterContent(
            isLoading = false,
            onRegisterClick = { _, _, _ -> },
            onNavigateToLogin = {}
        )
    }
}