package com.example.topbooks.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
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
import com.example.topbooks.ui.book.ReadingJournalScreen
import com.example.topbooks.ui.category.CategoriesScreen
import com.example.topbooks.ui.category.CategoryDetailScreen
import com.example.topbooks.ui.config.ConfigScreen
import com.example.topbooks.ui.config.ConfigViewModel
import com.example.topbooks.ui.friends.SocialActivityScreen
import com.example.topbooks.ui.home.RecommendedScreen
import com.example.topbooks.ui.home.RecommendedSectionScreen
import com.example.topbooks.ui.scanner.QRScannerScreen
import com.example.topbooks.ui.theme.ColorArcMediumBrown
import com.example.topbooks.ui.tutorial.TutorialScreen
import com.example.topbooks.ui.reviews.ReviewsScreen
import com.example.topbooks.ui.profile.ProfileScreen
import com.example.topbooks.ui.profile.UserListScreen

@Composable
fun AppNavigation(
    settingsManager: SettingsManager,
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()

    if (authState.isLoadingProfile) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ColorArcMediumBrown)
        }
        return
    }

    val startDestination = when {
        authState.currentUser == null -> "login"
        !authState.isTutorialCompleted -> "tutorial"
        else -> "main"
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable("login") {
            LoginScreen(
                onNavigateToRegister = { navController.navigate("register") },
                onLoginSuccess = {
                    navController.navigate(if (authState.isTutorialCompleted) "main" else "tutorial") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("register") {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate("tutorial") {
                        popUpTo("register") { inclusive = true }
                    }
                }
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

        // Aquí es donde inyectamos tu MainScreen con su barra
        composable("main") {
            MainScreen(
                onNavigateToConfig = { navController.navigate("config") },
                onNavigateToCategory = { categoryName: String, query: String ->
                    navController.navigate("category_detail/$categoryName/$query")
                },
                onNavigateToBookDetail = { bookId: String ->
                    if (bookId.isNotEmpty()) navController.navigate("book_detail/$bookId")
                },
                onNavigateToScanner = { navController.navigate("scanner") },
                onNavigateToAllCategories = { navController.navigate("all_categories") },
                onNavigateToRecommended = { navController.navigate("recommended_screen") },
                onNavigateToFriendsActivity = { navController.navigate("social_activity") },
                onNavigateToFriendProfile = { userId: String -> navController.navigate("profile/$userId") },
                onNavigateToList = { type: String, userId: String ->
                    navController.navigate("user_list/$type/$userId")
                },
                onNavigateToJournal = { bookId: String ->
                    navController.navigate("reading_journal/$bookId")
                }
            )
        }

        composable(
            route = "profile/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ProfileScreen(
                userId = userId,
                onNavigateToSettings = { navController.navigate("config") },
                onNavigateToDetail = { id: String -> if (id.isNotEmpty()) navController.navigate("book_detail/$id") },
                onNavigateToList = { type: String, id: String -> navController.navigate("user_list/$type/$id") },
                onBackClick = { navController.popBackStack() }
            )
        }

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
                onBookClick = { id: String -> if (id.isNotEmpty()) navController.navigate("book_detail/$id") }
            )
        }

        composable("social_activity") {
            SocialActivityScreen(
                onBackClick = { navController.popBackStack() },
                onBookClick = { id: String -> if (id.isNotEmpty()) navController.navigate("book_detail/$id") },
                onCommentClick = { bid: String, _: String ->
                    if (bid.isNotEmpty()) navController.navigate("reviews_thread/$bid")
                }
            )
        }

        composable(
            route = "reviews_thread/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            ReviewsScreen(
                onBackClick = { navController.popBackStack() },
                onBookClick = { id: String -> if (id.isNotEmpty()) navController.navigate("book_detail/$id") }
            )
        }

        composable(
            route = "book_detail/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            BookDetailScreen(
                bookId = bookId,
                onBackClick = { navController.popBackStack() },
                onNavigateToJournal = { id: String, _: String, _: String, _: String, _: String ->
                    navController.navigate("reading_journal/$id")
                }
            )
        }

        composable(
            route = "reading_journal/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            ReadingJournalScreen(
                bookId = backStackEntry.arguments?.getString("bookId") ?: "",
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("config") {
            ConfigScreen(
                viewModel = viewModel(factory = ConfigViewModel.Factory(settingsManager)),
                onLogoutSuccess = {
                    authViewModel.signOut()
                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("scanner") {
            QRScannerScreen(
                onBackClick = { navController.popBackStack() },
                onBookFound = { id: String ->
                    navController.navigate("book_detail/$id") { popUpTo("scanner") { inclusive = true } }
                }
            )
        }

        composable("all_categories") {
            CategoriesScreen(
                onBackClick = { navController.popBackStack() },
                onCategoryClick = { name: String, query: String ->
                    navController.navigate("category_detail/$name/$query")
                },
                onBookClick = { id: String -> if (id.isNotEmpty()) navController.navigate("book_detail/$id") },
                onScanClick = { navController.navigate("scanner") }
            )
        }

        composable(
            route = "category_detail/{categoryName}/{query}",
            arguments = listOf(
                navArgument("categoryName") { type = NavType.StringType },
                navArgument("query") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            CategoryDetailScreen(
                categoryName = backStackEntry.arguments?.getString("categoryName") ?: "",
                query = backStackEntry.arguments?.getString("query") ?: "",
                onBackClick = { navController.popBackStack() },
                onBookClick = { id: String -> if (id.isNotEmpty()) navController.navigate("book_detail/$id") },
                onScanClick = { navController.navigate("scanner") }
            )
        }

        composable("recommended_screen") {
            RecommendedScreen(
                onBackClick = { navController.popBackStack() },
                onBookClick = { id: String -> if (id.isNotEmpty()) navController.navigate("book_detail/$id") },
                onSectionClick = { type: String, genre: String, color: Int ->
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
            RecommendedSectionScreen(
                sectionType = backStackEntry.arguments?.getString("type") ?: "",
                genre = backStackEntry.arguments?.getString("genre") ?: "",
                colorArgb = backStackEntry.arguments?.getInt("color") ?: 0,
                onBackClick = { navController.popBackStack() },
                onBookClick = { id: String -> navController.navigate("book_detail/$id") }
            )
        }
    }
}