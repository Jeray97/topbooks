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
import com.example.topbooks.ui.reviews.SingleCommentScreen

/**
 * Orquestador principal de la navegación de la aplicación.
 * Define todas las rutas, transiciones y la lógica de destino inicial basada en el estado de autenticación.
 *
 * @param settingsManager Gestor de preferencias locales (Modo oscuro, idioma, etc.).
 * @param navController Controlador de navegación de Compose.
 * @param authViewModel ViewModel global que gestiona el estado de la sesión del usuario.
 */
@Composable
fun AppNavigation(
    settingsManager: SettingsManager,
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()

    // Estado de guarda: Evita renderizar rutas antes de conocer el perfil del usuario
    if (authState.isLoadingProfile) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ColorArcMediumBrown)
        }
        return
    }

    // Lógica de destino inicial: Login -> Tutorial (si es nuevo) -> Main
    val startDestination = when {
        authState.currentUser == null -> "login"
        !authState.isTutorialCompleted -> "tutorial"
        else -> "main"
    }

    NavHost(navController = navController, startDestination = startDestination) {

        // --- FLUJO DE AUTENTICACIÓN ---

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

        // --- CONFIGURACIÓN INICIAL (ONBOARDING) ---
        composable("tutorial") {
            TutorialScreen(
                onFinished = {
                    android.util.Log.d("TOUR_DEBUG", "AppNavigation: onFinished recibido")
                    authViewModel.markTutorialCompleted()

                    navController.navigate("main") {
                        popUpTo("tutorial") { inclusive = true }
                    }
                }
            )
        }

        // --- CONTENIDO PRINCIPAL ---

        composable("main") {
            MainScreen(
                onNavigateToConfig = { navController.navigate("config") },
                onNavigateToCategory = { categoryName, query ->
                    navController.navigate("category_detail/$categoryName/$query")
                },
                onNavigateToBookDetail = { bookId ->
                    if (bookId.isNotEmpty()) navController.navigate("book_detail/$bookId")
                },
                onNavigateToScanner = { navController.navigate("scanner") },
                onNavigateToAllCategories = { navController.navigate("all_categories") },
                onNavigateToRecommended = { navController.navigate("recommended_screen") },
                onNavigateToFriendsActivity = { navController.navigate("social_activity") },
                onNavigateToFriendProfile = { userId -> navController.navigate("profile/$userId") },
                onNavigateToList = { type, userId ->
                    navController.navigate("user_list/$type/$userId")
                },
                onNavigateToJournal = { bookId ->
                    // Desde el MainScreen solo tenemos el ID, los demás parámetros opcionales tomarán valor por defecto
                    navController.navigate("reading_journal/$bookId")
                }
            )
        }

        // --- SECCIÓN DE PERFIL Y LISTAS ---

        composable(
            route = "profile/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ProfileScreen(
                userId = userId,
                onNavigateToSettings = { navController.navigate("config") },
                onNavigateToDetail = { id -> if (id.isNotEmpty()) navController.navigate("book_detail/$id") },
                onNavigateToList = { type, id -> navController.navigate("user_list/$type/$id") },
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
                onBookClick = { id -> if (id.isNotEmpty()) navController.navigate("book_detail/$id") },
                // Navegación hacia hilos de conversación desde listados
                onCommentClick = { _, cid ->
                    if (cid.isNotEmpty()) navController.navigate("single_comment/$cid")
                }
            )
        }

        // --- ACTIVIDAD SOCIAL Y RESEÑAS ---

        composable("social_activity") {
            SocialActivityScreen(
                onBackClick = { navController.popBackStack() },
                onBookClick = { id -> if (id.isNotEmpty()) navController.navigate("book_detail/$id") },
                onCommentClick = { _, cid ->
                    if (cid.isNotEmpty()) navController.navigate("single_comment/$cid")
                }
            )
        }

        composable(
            route = "reviews_thread/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) {
            ReviewsScreen(
                onBackClick = { navController.popBackStack() },
                onBookClick = { id -> if (id.isNotEmpty()) navController.navigate("book_detail/$id") }
            )
        }

        composable(
            route = "single_comment/{commentId}",
            arguments = listOf(navArgument("commentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val commentId = backStackEntry.arguments?.getString("commentId") ?: ""
            SingleCommentScreen(
                commentId = commentId,
                onBackClick = { navController.popBackStack() }
            )
        }

        // --- DETALLE DE LIBRO Y DIARIO ---

        composable(
            route = "book_detail/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            BookDetailScreen(
                bookId = bookId,
                onBackClick = { navController.popBackStack() },
                // AQUÍ EL ARREGLO: Recogemos los parámetros y construimos la URL con Query Parameters (?)
                onNavigateToJournal = { id, title, author, image, pages ->
                    navController.navigate("reading_journal/$id?title=$title&author=$author&image=$image&pages=$pages")
                }
            )
        }

        // AQUÍ EL ARREGLO: Añadimos los argumentos extra como opcionales para que Compose los intercepte
        composable(
            route = "reading_journal/{bookId}?title={title}&author={author}&image={image}&pages={pages}",
            arguments = listOf(
                navArgument("bookId") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType; defaultValue = "" },
                navArgument("author") { type = NavType.StringType; defaultValue = "" },
                navArgument("image") { type = NavType.StringType; defaultValue = "" },
                navArgument("pages") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            val title = backStackEntry.arguments?.getString("title") ?: ""
            val author = backStackEntry.arguments?.getString("author") ?: ""
            val image = backStackEntry.arguments?.getString("image") ?: ""
            val pages = backStackEntry.arguments?.getString("pages") ?: ""

            ReadingJournalScreen(
                bookId = bookId,
                initialTitle = title,
                initialAuthor = author,
                initialImage = image,
                initialPages = pages,
                onBackClick = { navController.popBackStack() }
            )
        }

        // --- CONFIGURACIÓN Y UTILIDADES ---

        composable("config") {
            ConfigScreen(
                viewModel = viewModel(factory = ConfigViewModel.Factory(settingsManager)),
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("scanner") {
            QRScannerScreen(
                onBackClick = { navController.popBackStack() },
                onBookFound = { id ->
                    navController.navigate("book_detail/$id") { popUpTo("scanner") { inclusive = true } }
                }
            )
        }

        // --- EXPLORACIÓN POR CATEGORÍAS Y RECOMENDADOS ---

        composable("all_categories") {
            CategoriesScreen(
                onBackClick = { navController.popBackStack() },
                onCategoryClick = { name, query ->
                    navController.navigate("category_detail/$name/$query")
                },
                onBookClick = { id -> if (id.isNotEmpty()) navController.navigate("book_detail/$id") },
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
                onBookClick = { id -> if (id.isNotEmpty()) navController.navigate("book_detail/$id") },
                onScanClick = { navController.navigate("scanner") }
            )
        }

        composable("recommended_screen") {
            RecommendedScreen(
                onBackClick = { navController.popBackStack() },
                onBookClick = { id -> if (id.isNotEmpty()) navController.navigate("book_detail/$id") },
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
            RecommendedSectionScreen(
                sectionType = backStackEntry.arguments?.getString("type") ?: "",
                genre = backStackEntry.arguments?.getString("genre") ?: "",
                colorArgb = backStackEntry.arguments?.getInt("color") ?: 0,
                onBackClick = { navController.popBackStack() },
                onBookClick = { id -> navController.navigate("book_detail/$id") }
            )
        }
    }
}