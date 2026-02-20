package com.example.topbooks.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.ui.theme.*

@Composable
fun UserListScreen(
    type: String, // "friends", "reviews", "read"
    userId: String,
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    onUserClick: (String) -> Unit
) {
    val title = when(type) {
        "friends" -> "Amigos"
        "reviews" -> "Reseñas"
        "read" -> "Libros Leídos"
        else -> "Lista"
    }

    Scaffold(
        containerColor = ColorBackGroundGeneral,
        topBar = { TopBar(onBackClick = onBackClick) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                text = title,
                fontFamily = CenturyGotic,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTituloTopBooks
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Aquí implementaremos la lógica de carga de datos según el tipo
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Próximamente: Lista de $title para el usuario $userId",
                    color = ColorArcDarkBrown,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}