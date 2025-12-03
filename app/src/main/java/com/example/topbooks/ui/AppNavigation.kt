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
import com.example.topbooks.ui.book.BookDetailScreen
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

        // PANTALLA PRINCIPAL
        composable("main") {
            MainScreen(
                onLogout = {
                    authViewModel.signOut()
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                },
                // Navegar a categoría
                onNavigateToCategory = { nombre, query ->
                    navController.navigate("category_detail/$nombre/$query")
                },
                // Navegar a detalle de libro DIRECTAMENTE
                onNavigateToBookDetail = { bookId ->
                    navController.navigate("book_detail/$bookId")
                }
            )
        }

        // DETALLE DE CATEGORIA
        composable(
            route = "category_detail/{name}/{query}",
            arguments = listOf(
                navArgument("name") { type = NavType.StringType },
                navArgument("query") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoryName = backStackEntry.arguments?.getString("name") ?: "Categoría"
            val query = backStackEntry.arguments?.getString("query") ?: ""

            CategoryDetailScreen(
                categoryName = categoryName,
                query = query,
                onBackClick = { navController.popBackStack() },
                // Pasamos la navegación al libro aquí también
                onBookClick = { bookId ->
                    navController.navigate("book_detail/$bookId")
                }
            )
        }

        // NUEVO: PANTALLA DE DETALLE DE LIBRO (Tarjeta Marrón)
        composable(
            route = "book_detail/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""

            BookDetailScreen(
                bookId = bookId,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}