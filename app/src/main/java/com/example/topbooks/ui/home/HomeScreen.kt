package com.example.topbooks.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.topbooks.ui.auth.AuthViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(
    viewModel: AuthViewModel = viewModel(),
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "¡Bienvenido a TopBooks!", fontSize = 24.sp)

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = {
            viewModel.signOut() // Cerramos sesión en Firebase
            onLogout()          // Navegamos al Login
        }) {
            Text("Cerrar Sesión")
        }
    }
}