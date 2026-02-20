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
import com.example.topbooks.ui.config.ConfigScreen
import com.example.topbooks.ui.config.ConfigViewModel
import com.example.topbooks.ui.friends.SocialActivityScreen
import com.example.topbooks.ui.scanner.QRScannerScreen
import com.example.topbooks.ui.theme.ColorArcMediumBrown
import com.example.topbooks.ui.tutorial.TutorialScreen
import com.example.topbooks.ui.reviews.ReviewsScreen
import com.example.topbooks.ui.profile.ProfileScreen
import com.example.topbooks.ui.profile.UserListScreen

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
                onNavigateToFriendsActivity = { navController.navigate("social_activity") },
                onNavigateToFriendProfile = { userId -> navController.navigate("profile/$userId") },
                // Pasamos la nueva función de navegación a listas
                onNavigateToList = { type, userId ->
                    navController.navigate("user_list/$type/$userId")
                }
            )
        }

        // --- RUTA DEL PERFIL (PARA AMIGOS Y PARA TI MISMO) ---
        composable(
            route = "profile/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ProfileScreen(
                userId = userId,
                onNavigateToSettings = { navController.navigate("config") },
                onNavigateToDetail = { bookId -> navController.navigate("book_detail/$bookId") },
                onNavigateToList = { type, id -> navController.navigate("user_list/$type/$id") },
                onBackClick = { navController.popBackStack() }
            )
        }

        // --- RUTA DEL HILO DE COMENTARIOS ---
        composable(
            route = "reviews_thread/{bookId}/{commentId}",
            arguments = listOf(
                navArgument("bookId") { type = NavType.StringType },
                navArgument("commentId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            val commentId = backStackEntry.arguments?.getString("commentId") ?: ""
            ReviewsScreen(
                onBackClick = { navController.popBackStack() },
                onBookClick = { id -> navController.navigate("book_detail/$id") },
                bookId = bookId,
                targetCommentId = commentId
            )
        }

        // --- RUTA DE ACTIVIDAD SOCIAL (¡FIXED!) ---
        composable("social_activity") {
            SocialActivityScreen(
                onBackClick = { navController.popBackStack() },
                onBookClick = { bookId -> navController.navigate("book_detail/$bookId") },
                // AQUÍ ESTABA EL ERROR: Faltaba pasar este parámetro
                onCommentClick = { bookId, commentId ->
                    navController.navigate("reviews_thread/$bookId/$commentId")
                }
            )
        }

        // --- RUTA PARA LISTAS (AMIGOS, LEÍDOS, RESEÑAS) ---
        composable(
            route = "user_list/{type}/{userId}",
            arguments = listOf(
                navArgument("type") { type = NavType.StringType },
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "friends"
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            UserListScreen(
                type = type,
                userId = userId,
                onBackClick = { navController.popBackStack() },
                onBookClick = { bookId -> navController.navigate("book_detail/$bookId") },
                onUserClick = { targetId -> navController.navigate("profile/$targetId") }
            )
        }

        composable("config") {
            val viewModel: ConfigViewModel = viewModel(factory = ConfigViewModel.Factory(settingsManager))
            ConfigScreen(
                viewModel = viewModel,
                onLogoutSuccess = {
                    authViewModel.signOut()
                    navController.navigate("login") { popUpTo("main") { inclusive = true } }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("scanner") {
            QRScannerScreen(
                onBookFound = { bookId ->
                    navController.navigate("book_detail/$bookId") { popUpTo("scanner") { inclusive = true } }
                }
            )
        }
    }
}