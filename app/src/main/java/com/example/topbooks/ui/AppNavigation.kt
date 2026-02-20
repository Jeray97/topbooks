package com.example.topbooks.ui

import android.net.Uri
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
import com.example.topbooks.ui.book.BookDetailScreen
import com.example.topbooks.ui.book.ReadingJournalScreen
import com.example.topbooks.ui.profile.ProfileScreen
import com.example.topbooks.ui.profile.UserListScreen
import com.example.topbooks.ui.friends.SocialActivityScreen
import com.example.topbooks.ui.reviews.ReviewsScreen
import com.example.topbooks.ui.theme.ColorArcMediumBrown

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

    val startDestination = if (authViewModel.currentUser == null) "login" else "main"

    NavHost(navController = navController, startDestination = startDestination) {

        // --- RUTAS BÁSICAS ---
        composable("login") { /* ... tu lógica de login ... */ }
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

        // --- DETALLE DEL LIBRO (CON TU DISEÑO ORIGINAL) ---
        composable(
            route = "book_detail/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            BookDetailScreen(
                bookId = bookId,
                onBackClick = { navController.popBackStack() },
                // Enviamos los datos necesarios para precargar el diario
                onNavigateToJournal = { id, title, author, img, pages ->
                    val encodedUrl = Uri.encode(img)
                    navController.navigate("reading_journal/$id?title=$title&author=$author&img=$encodedUrl")
                }
            )
        }

        // --- FICHA DE LECTURA (RECIBE DATOS OPCIONALES) ---
        composable(
            route = "reading_journal/{bookId}?title={title}&author={author}&img={img}",
            arguments = listOf(
                navArgument("bookId") { type = NavType.StringType },
                navArgument("title") { defaultValue = ""; type = NavType.StringType },
                navArgument("author") { defaultValue = ""; type = NavType.StringType },
                navArgument("img") { defaultValue = ""; type = NavType.StringType },
                navArgument("pages") { defaultValue = ""; type = NavType.StringType }
            )
        ) { backStackEntry ->
            ReadingJournalScreen(
                bookId = backStackEntry.arguments?.getString("bookId") ?: "",
                initialTitle = backStackEntry.arguments?.getString("title") ?: "",
                initialAuthor = backStackEntry.arguments?.getString("author") ?: "",
                initialImage = backStackEntry.arguments?.getString("img") ?: "",
                initialPages = backStackEntry.arguments?.getString("pages") ?: "",
                onBackClick = { navController.popBackStack() }
            )
        }

        // --- RESTO DE RUTAS ---
        composable("profile/{userId}") { /* ... */ }
        composable("user_list/{type}/{userId}") { /* ... */ }
    }
}