package com.example.topbooks.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

// Definimos las 5 pestañas
sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavItem("home_tab", "Inicio", Icons.Default.Home)
    object Progress : BottomNavItem("progress_tab", "Progreso", Icons.Default.List) // Icono de lista para el progreso
    object Friends : BottomNavItem("friends_tab", "Amigos", Icons.Default.Face) // Icono de cara para amigos
    object Reviews : BottomNavItem("reviews_tab", "Reseñas", Icons.Default.Edit) // Icono de editar para reseñas
    object Profile : BottomNavItem("profile_tab", "Perfil", Icons.Default.Person)
}