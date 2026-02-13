package com.example.topbooks.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.topbooks.data.preferences.SettingsManager
import com.example.topbooks.ui.auth.AuthViewModel
import com.example.topbooks.ui.auth.LoginScreen
import com.example.topbooks.ui.auth.RegisterScreen
import com.example.topbooks.ui.book.BookDetailScreen
import com.example.topbooks.ui.category.CategoriesScreen
import com.example.topbooks.ui.category.CategoryDetailScreen
import com.example.topbooks.ui.config.ConfigScreen
import com.example.topbooks.ui.config.ConfigViewModel
import com.example.topbooks.ui.scanner.QRScannerScreen

@Composable
fun AppNavigation(
    settingsManager: SettingsManager,
    authViewModel: AuthViewModel = viewModel()
) {
    val navController = rememberNavController()
    val startDestination = if (authViewModel.currentUser != null) "main" else "login"

    NavHost(navController = navController, startDestination = startDestination) {

        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("main") { popUpTo("login") { inclusive = true } }
                },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }

        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("main") { popUpTo("login") { inclusive = true } }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable("main") {
            MainScreen(
                onNavigateToConfig = { navController.navigate("config") }, // Nueva conexión
                onNavigateToCategory = { nombre, query ->
                    navController.navigate("category_detail/$nombre/$query")
                },
                onNavigateToBookDetail = { bookId ->
                    navController.navigate("book_detail/$bookId")
                },
                onNavigateToScanner = { navController.navigate("scanner") },
                onNavigateToAllCategories = { navController.navigate("all_categories") }
            )
        }

        // --- PANTALLA DE CONFIGURACIÓN ---
        composable("config") {
            val configViewModel: ConfigViewModel = viewModel(
                factory = ConfigViewModel.Factory(settingsManager)
            )
            ConfigScreen(
                viewModel = configViewModel,
                onBackClick = { navController.popBackStack() },
                onLogoutSuccess = {
                    // Volvemos al login y limpiamos el historial
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                },
                onNavigateToAbout = { /* Navegar a Acerca de */ },
                onNavigateToPrivacy = { /* Navegar a Privacidad */ }
                // TODO CREAR TODAS LAS FUNCIONES
            )
        }

        composable("scanner") {
            QRScannerScreen(
                onBookFound = { bookId ->
                    navController.navigate("book_detail/$bookId") {
                        popUpTo("scanner") { inclusive = true }
                    }
                }
            )
        }

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
                onBookClick = { bookId -> navController.navigate("book_detail/$bookId") },
                onScanClick = { navController.navigate("scanner") }
            )
        }

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

        composable("all_categories") {
            CategoriesScreen(
                onBackClick = { navController.popBackStack() },
                onCategoryClick = { name, query ->
                    navController.navigate("category_detail/$name/$query")
                },
                onBookClick = { bookId -> navController.navigate("book_detail/$bookId") },
                onScanClick = { navController.navigate("scanner") }
            )
        }
    }
}