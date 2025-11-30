package com.example.topbooks.ui.navigation

import com.example.topbooks.R

// Definimos las 5 pestañas
sealed class BottomNavItem(val route: String, val title: Int, val icon: Int) {
    object Home : BottomNavItem("home_tab", R.string.home_bottom, R.drawable.home_icon)
    object Progress : BottomNavItem("progress_tab", R.string.progress_bottom, R.drawable.progreso)
    object Friends : BottomNavItem("friends_tab", R.string.social_bottom, R.drawable.social)
    object Reviews : BottomNavItem("reviews_tab", R.string.reviews_bottom, R.drawable.resenas)
    object Profile : BottomNavItem("profile_tab", R.string.profile_bottom, R.drawable.perfil)
}