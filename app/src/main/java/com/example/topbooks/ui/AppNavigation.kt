package com.example.topbooks.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.topbooks.ui.auth.AuthViewModel
import com.example.topbooks.ui.auth.LoginScreen
import com.example.topbooks.ui.auth.RegisterScreen
import com.example.topbooks.ui.book.BookDetailScreen
import com.example.topbooks.ui.category.CategoriesScreen
import com.example.topbooks.ui.category.CategoryDetailScreen
import com.example.topbooks.ui.scanner.QRScannerScreen

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel = viewModel()
) {
    val navController = rememberNavController()

    // Si hay usuario, vamos directo a la pantalla principal (Main)
    val startDestination = if (authViewModel.currentUser != null) "main" else "login"

    NavHost(navController = navController, startDestination = startDestination) {

        // 1. LOGIN
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

        // 2. REGISTRO
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

        // 3. PANTALLA PRINCIPAL
        composable("main") {
            MainScreen(
                onLogout = {
                    authViewModel.signOut()
                    navController.navigate("login") { popUpTo("main") { inclusive = true } }
                },
                onNavigateToCategory = { nombre, query ->
                    navController.navigate("category_detail/$nombre/$query")
                },
                //Pasamos la navegación al detalle del libro
                onNavigateToBookDetail = { bookId ->
                    navController.navigate("book_detail/$bookId")
                },
                //Pasamos la navegación al escáner
                onNavigateToScanner = {
                    navController.navigate("scanner")
                },
                //Pasamos la navegación a todas las categorías
                onNavigateToAllCategories = {
                    navController.navigate("all_categories")
                }
            )
        }

        // 4. ESCÁNER QR
        composable("scanner") {
            QRScannerScreen(
                onBookFound = { bookId ->
                    // Si encontramos libro, vamos al detalle y borramos el scanner del historial
                    navController.navigate("book_detail/$bookId") {
                        popUpTo("scanner") { inclusive = true }
                    }
                }
            )
        }

        // 5. DETALLE DE CATEGORÍA
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

        // 6. DETALLE DE LIBRO (Tarjeta Marrón)
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

        // 7. IR A TODAS LAS CATEGORIAS
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