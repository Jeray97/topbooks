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
        // ... (login, register, tutorial se mantienen igual)

        composable("main") {
            MainScreen(
                onNavigateToConfig = { navController.navigate("config") },
                onNavigateToCategory = { name, q -> navController.navigate("category_detail/$name/$q") },
                onNavigateToBookDetail = { id -> if(id.isNotEmpty()) navController.navigate("book_detail/$id") },
                onNavigateToScanner = { navController.navigate("scanner") },
                onNavigateToAllCategories = { navController.navigate("all_categories") },
                onNavigateToRecommended = { navController.navigate("recommended_screen") },
                onNavigateToFriendsActivity = { navController.navigate("social_activity") },
                onNavigateToFriendProfile = { id -> navController.navigate("profile/$id") },
                onNavigateToList = { type, id -> navController.navigate("user_list/$type/$id") }
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
                // PASAMOS LA RUTA AL DIARIO
                onNavigateToJournal = { id -> navController.navigate("reading_journal/$id") }
            )
        }

        // RUTA PARA LA FICHA DE LECTURA
        composable(
            route = "reading_journal/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            ReadingJournalScreen(
                bookId = bookId,
                onBackClick = { navController.popBackStack() }
            )
        }

        // ... (resto de rutas se mantienen igual)
        composable(route = "profile/{userId}", arguments = listOf(navArgument("userId") { type = NavType.StringType })) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ProfileScreen(userId = userId, onNavigateToSettings = { navController.navigate("config") }, onNavigateToDetail = { id -> if(id.isNotEmpty()) navController.navigate("book_detail/$id") }, onNavigateToList = { type, id -> if(id.isNotEmpty()) navController.navigate("user_list/$type/$id") }, onBackClick = { navController.popBackStack() })
        }

        composable(route = "user_list/{type}/{userId}", arguments = listOf(navArgument("type") { type = NavType.StringType }, navArgument("userId") { type = NavType.StringType })) { backStackEntry ->
            UserListScreen(type = backStackEntry.arguments?.getString("type") ?: "friends", userId = backStackEntry.arguments?.getString("userId") ?: "", onBackClick = { navController.popBackStack() }, onBookClick = { id -> if(id.isNotEmpty()) navController.navigate("book_detail/$id") }, onUserClick = { id -> if(id.isNotEmpty()) navController.navigate("profile/$id") })
        }

        composable("social_activity") {
            SocialActivityScreen(onBackClick = { navController.popBackStack() }, onBookClick = { id -> if(id.isNotEmpty()) navController.navigate("book_detail/$id") }, onCommentClick = { bid, cid -> if(bid.isNotEmpty()) navController.navigate("reviews_thread/$bid/$cid") })
        }

        composable(route = "reviews_thread/{bookId}/{commentId}", arguments = listOf(navArgument("bookId") { type = NavType.StringType }, navArgument("commentId") { type = NavType.StringType })) { backStackEntry ->
            ReviewsScreen(onBackClick = { navController.popBackStack() }, onBookClick = { id -> if(id.isNotEmpty()) navController.navigate("book_detail/$id") }, bookId = backStackEntry.arguments?.getString("bookId"), targetCommentId = backStackEntry.arguments?.getString("commentId"))
        }

        composable("config") { ConfigScreen(viewModel = viewModel(factory = ConfigViewModel.Factory(settingsManager)), onLogoutSuccess = { authViewModel.signOut(); navController.navigate("login") { popUpTo("main") { inclusive = true } } }, onBackClick = { navController.popBackStack() }) }
        composable("scanner") { QRScannerScreen(onBookFound = { id -> navController.navigate("book_detail/$id") { popUpTo("scanner") { inclusive = true } } }) }
    }
}