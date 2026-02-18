package com.example.topbooks.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.topbooks.ui.friends.FriendsActivityScreen
import com.example.topbooks.ui.home.RecommendedScreen
import com.example.topbooks.ui.home.RecommendedSectionScreen
import com.example.topbooks.ui.scanner.QRScannerScreen
import com.example.topbooks.ui.theme.ColorArcMediumBrown
import com.example.topbooks.ui.tutorial.TutorialScreen

@Composable
fun AppNavigation(
    settingsManager: SettingsManager,
    authViewModel: AuthViewModel = viewModel()
) {
    val navController = rememberNavController()

    if (authViewModel.isLoadingProfile) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ColorArcMediumBrown)
        }
        return
    }

    val startDestination = when {
        authViewModel.currentUser == null -> "login"
        !authViewModel.isTutorialCompleted -> "tutorial"
        else -> "main"
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(if (authViewModel.isTutorialCompleted) "main" else "tutorial") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }

        composable("register") {
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = {
                    navController.navigate("tutorial") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable("tutorial") {
            TutorialScreen(onFinished = {
                authViewModel.checkUserProfile()
                navController.navigate("main") {
                    popUpTo("tutorial") { inclusive = true }
                }
            })
        }

        // --- PANTALLA PRINCIPAL ---
        composable("main") {
            MainScreen(
                onNavigateToConfig = { navController.navigate("config") },
                onNavigateToCategory = { categoryName, query ->
                    navController.navigate("category_detail/$categoryName/$query")
                },
                onNavigateToBookDetail = { bookId ->
                    navController.navigate("book_detail/$bookId")
                },
                onNavigateToScanner = { navController.navigate("scanner") },
                onNavigateToAllCategories = { navController.navigate("all_categories") },
                onNavigateToRecommended = { navController.navigate("recommended_screen") },
                // --- FIX: ESTA ES LA LÍNEA QUE FALTABA ---
                onNavigateToFriendsActivity = { navController.navigate("friends_activity") }
            )
        }

        composable("recommended_screen") {
            RecommendedScreen(
                onBackClick = { navController.popBackStack() },
                onBookClick = { bookId -> navController.navigate("book_detail/$bookId") },
                onSectionClick = { type, genre, color ->
                    navController.navigate("recommended_section/$type/$genre/$color")
                }
            )
        }

        composable(
            route = "recommended_section/{type}/{genre}/{color}",
            arguments = listOf(
                navArgument("type") { type = NavType.StringType },
                navArgument("genre") { type = NavType.StringType },
                navArgument("color") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "POPULAR"
            val genre = backStackEntry.arguments?.getString("genre") ?: "General"
            val color = backStackEntry.arguments?.getInt("color") ?: 0xFF000000.toInt()

            RecommendedSectionScreen(
                sectionType = type,
                genre = genre,
                colorArgb = color,
                onBackClick = { navController.popBackStack() },
                onBookClick = { bookId -> navController.navigate("book_detail/$bookId") }
            )
        }

        composable(
            route = "book_detail/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: return@composable
            BookDetailScreen(
                bookId = bookId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = "category_detail/{categoryName}/{query}",
            arguments = listOf(
                navArgument("categoryName") { type = NavType.StringType },
                navArgument("query") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
            val query = backStackEntry.arguments?.getString("query") ?: ""

            CategoryDetailScreen(
                categoryName = categoryName,
                query = query,
                onBackClick = { navController.popBackStack() },
                onBookClick = { bookId -> navController.navigate("book_detail/$bookId") },
                onScanClick = { navController.navigate("scanner") }
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

        composable("config") {
            val viewModel: ConfigViewModel = viewModel(factory = ConfigViewModel.Factory(settingsManager))
            ConfigScreen(
                viewModel = viewModel,
                onLogoutSuccess = {
                    authViewModel.signOut()
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                },
                onBackClick = { navController.popBackStack() }
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

        composable("friends_activity") {
            FriendsActivityScreen(
                onBackClick = { navController.popBackStack() },
                onBookClick = { bookId -> navController.navigate("book_detail/$bookId") }
            )
        }
    }
}