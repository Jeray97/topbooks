package com.example.topbooks.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.topbooks.ui.auth.AuthViewModel
import com.example.topbooks.ui.auth.LoginScreen
import com.example.topbooks.ui.auth.RegisterScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.topbooks.ui.category.CategoryDetailScreen

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel = viewModel()
) {
    val navController = rememberNavController()

    // Si hay usuario, vamos directo a la pantalla principal (Main)
    val startDestination = if (authViewModel.currentUser != null) "main" else "login"

    NavHost(navController = navController, startDestination = startDestination) {

        // LOGIN
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }

        // REGISTRO
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // PANTALLA PRINCIPAL (CON BARRA DE NAVEGACIÓN)
        composable("main") {
            MainScreen(
                onLogout = {
                    authViewModel.signOut() // 1. Cerramos sesión en Firebase
                    navController.navigate("login") { // 2. Navegamos al login
                        popUpTo("main") { inclusive = true } // 3. Borramos el historial
                    }
                },

                onNavigateToCategory = { nombre, query ->
                    navController.navigate("category_detail/$nombre/$query")
                }
            )
        }

        //DETALLE DE CATEGORIA
        composable(
            route = "category_detail/{name}/{query}",
            arguments = listOf(
                navArgument("name") { type = NavType.StringType },
                navArgument("query") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            //Recuperamos datos de URL
            val categoryName = backStackEntry.arguments?.getString("name") ?: "Categoría"
            val query = backStackEntry.arguments?.getString("query") ?: ""

            //LLamamos de nuevo a la pantalla
            CategoryDetailScreen(
                categoryName = categoryName,
                query = query,
                onBackClick = { navController.popBackStack() } // Para que la flecha atrás funcione
            )
        }
    }
}