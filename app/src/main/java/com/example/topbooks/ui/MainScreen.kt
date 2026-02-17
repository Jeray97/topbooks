package com.example.topbooks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.topbooks.ui.home.HomeScreen
import com.example.topbooks.ui.navigation.BottomNavItem
import com.example.topbooks.ui.theme.*
import com.example.topbooks.ui.profile.ProfileScreen
import com.example.topbooks.ui.friends.FriendsScreen
import com.example.topbooks.ui.reviews.ReviewsScreen

@Composable
fun MainScreen(
    onNavigateToConfig: () -> Unit,
    onNavigateToCategory: (String, String) -> Unit,
    onNavigateToBookDetail: (String) -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToAllCategories: () -> Unit
) {
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Progress,
        BottomNavItem.Friends,
        BottomNavItem.Reviews,
        BottomNavItem.Profile
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = ColorArcMediumBrown, // Color de la barra café
                tonalElevation = 0.dp // Quitamos elevación para que el color sea puro
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route

                    NavigationBarItem(
                        icon = {
                            // --- CÍRCULO BLANCO DE FONDO ---
                            Box(
                                modifier = Modifier
                                    .size(40.dp) // Tamaño del círculo
                                    .background(
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = item.icon),
                                    contentDescription = stringResource(id = item.title),
                                    // Ajustamos el tamaño del icono vectorial dentro del círculo
                                    modifier = Modifier.size(24.dp),
                                    // El icono se verá café sobre el fondo blanco
                                    tint = ColorArcMediumBrown
                                )
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(id = item.title),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        selected = isSelected,
                        onClick = {
                            bottomNavController.navigate(item.route) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            // Ponemos el indicador transparente para que no choque con nuestro círculo
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    onCategoryClick = onNavigateToCategory,
                    onBookClick = onNavigateToBookDetail,
                    onScanClick = onNavigateToScanner,
                    onSeeAllCategoriesClick = onNavigateToAllCategories
                )
            }

            composable(BottomNavItem.Progress.route) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Pantalla de Progreso (Próximamente)")
                }
            }

            composable(BottomNavItem.Friends.route) {
                FriendsScreen()
            }

            composable(BottomNavItem.Reviews.route) {
                ReviewsScreen(
                    onBackClick = { /* No hay back en la raíz */ },
                    onBookClick = onNavigateToBookDetail
                )
            }

            composable(BottomNavItem.Profile.route) {
                ProfileScreen(
                    onNavigateToSettings = onNavigateToConfig,
                    onNavigateToDetail = onNavigateToBookDetail
                )
            }
        }
    }
}