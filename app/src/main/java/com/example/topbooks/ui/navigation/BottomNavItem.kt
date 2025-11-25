package com.example.topbooks.ui.navigation

import com.example.topbooks.R

// Definimos las 5 pestañas
sealed class BottomNavItem(val route: String, val title: String, val icon: Int) {
    object Home : BottomNavItem("home_tab", "Inicio", R.drawable.home_icon)
    object Progress : BottomNavItem("progress_tab", "Progreso", R.drawable.progreso)
    object Friends : BottomNavItem("friends_tab", "Social", R.drawable.social)
    object Reviews : BottomNavItem("reviews_tab", "Reseñas", R.drawable.resenas)
    object Profile : BottomNavItem("profile_tab", "Perfil", R.drawable.perfil)
}