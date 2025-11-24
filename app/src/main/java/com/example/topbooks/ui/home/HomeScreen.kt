package com.example.topbooks.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "¡Bienvenido a TopBooks!", fontSize = 24.sp)
        Text(text = "Tu biblioteca personal", fontSize = 16.sp)

        Spacer(modifier = Modifier.height(32.dp))

        Text("Aquí aparecerán tus libros favoritos...")
    }
}