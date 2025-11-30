package com.example.topbooks.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.topbooks.ui.home.HomeScreen
import com.example.topbooks.ui.navigation.BottomNavItem
import com.example.topbooks.ui.theme.*

@Composable
fun MainScreen(
    onLogout: () -> Unit, // Este parámetro es obligatorio para el botón de salir
    onNavigateToCategory: (String, String) -> Unit
) {
    // 1. Configuración de la navegación interna (la de las pestañas)
    val bottomNavController = rememberNavController()

    // 2. Definición de las 5 pestañas
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Progress,
        BottomNavItem.Friends,
        BottomNavItem.Reviews,
        BottomNavItem.Profile
    )

    // 3. Estructura visual: Barra abajo + Contenido
    Scaffold(

        containerColor = ColorBackGroundGeneral,
        bottomBar = {
            NavigationBar(
                containerColor = ColorSectionBackground,
            ) {
                val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = item.icon),
                                contentDescription = stringResource(id = item.title),
                                tint = Color.Unspecified
                            )
                        },
                        label = { Text(stringResource(id = item.title)) },
                        selected = currentRoute == item.route,
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = ColorSectionBackground.copy(alpha = 0.2f), // Color de la "burbuja" al seleccionar
                            selectedTextColor = ColorBackGroundCategorySection,
                            unselectedTextColor = ColorTextPrimary
                        ),
                        onClick = {
                            bottomNavController.navigate(item.route) {
                                // Configuración para que la navegación sea fluida
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        // 4. Aquí se muestra el contenido según la pestaña elegida
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Pestaña INICIO
            composable(BottomNavItem.Home.route) {
                HomeScreen(onCategoryClick = onNavigateToCategory)
            }

            // Pestaña PROGRESO
            composable(BottomNavItem.Progress.route) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Pantalla de Progreso (Próximamente)")
                }
            }

            // Pestaña AMIGOS
            composable(BottomNavItem.Friends.route) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Lista de Amigos (Próximamente)")
                }
            }

            // Pestaña RESEÑAS
            composable(BottomNavItem.Reviews.route) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tus Reseñas (Próximamente)")
                }
            }

            // Pestaña PERFIL
            composable(BottomNavItem.Profile.route) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    // Aquí usamos el 'onLogout' que recibimos desde AppNavigation
                    Button(onClick = onLogout) {
                        Text("Cerrar Sesión")
                    }
                }
            }
        }
    }
}