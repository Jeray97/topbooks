package com.example.topbooks.ui

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.topbooks.ui.home.HomeScreen
import com.example.topbooks.ui.navigation.BottomNavItem
import com.example.topbooks.ui.theme.*
import com.example.topbooks.ui.profile.ProfileScreen
import com.example.topbooks.ui.friends.FriendsScreen
import com.example.topbooks.ui.reviews.ReviewsScreen
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID

@Composable
fun MainScreen(
    onNavigateToConfig: () -> Unit,
    onNavigateToCategory: (String, String) -> Unit,
    onNavigateToBookDetail: (String) -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToAllCategories: () -> Unit,
    onNavigateToRecommended: () -> Unit,
    onNavigateToFriendsActivity: () -> Unit,
    onNavigateToFriendProfile: (String) -> Unit,
    onNavigateToList: (String, String) -> Unit,
    onNavigateToJournal: (String) -> Unit
) {
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("topbooks_tour_prefs", Context.MODE_PRIVATE)
    var showInteractiveTour by remember { mutableStateOf(!sharedPrefs.getBoolean("has_seen_spotlight_tour", false)) }

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Progress,
        BottomNavItem.Friends,
        BottomNavItem.Reviews,
        BottomNavItem.Profile
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.navigationBarsPadding(),
                    containerColor = ColorArcMediumBrown,
                    tonalElevation = 0.dp
                ) {
                    items.forEach { item ->
                        val isSelected = currentRoute == item.route

                        NavigationBarItem(
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = item.icon),
                                        contentDescription = stringResource(id = item.title),
                                        modifier = Modifier.size(24.dp),
                                        tint = ColorArcMediumBrown
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = stringResource(id = item.title),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                bottomNavController.navigate(item.route) {
                                    popUpTo(bottomNavController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = bottomNavController,
                startDestination = BottomNavItem.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(BottomNavItem.Home.route) {
                    HomeScreen(
                        onCategoryClick = onNavigateToCategory,
                        onBookClick = onNavigateToBookDetail,
                        onScanClick = onNavigateToScanner,
                        onSeeAllCategoriesClick = onNavigateToAllCategories,
                        onRecommendedClick = onNavigateToRecommended,
                        onFriendsActivityClick = onNavigateToFriendsActivity
                    )
                }

                composable(BottomNavItem.Progress.route) {
                    com.example.topbooks.ui.progress.ProgressScreen(
                        onNavigateToList = { type ->
                            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                            onNavigateToList(type, uid)
                        },
                        onBookClick = onNavigateToBookDetail,
                        onJournalClick = onNavigateToJournal,
                        onAddJournalClick = {
                            val customBookId = "custom_${UUID.randomUUID()}"
                            onNavigateToJournal(customBookId)
                        }
                    )
                }

                composable(BottomNavItem.Friends.route) {
                    FriendsScreen(
                        onNavigateToProfile = onNavigateToFriendProfile,
                        onNavigateToActivity = onNavigateToFriendsActivity
                    )
                }

                composable(BottomNavItem.Reviews.route) {
                    ReviewsScreen(
                        onBackClick = { bottomNavController.popBackStack() },
                        onBookClick = onNavigateToBookDetail
                    )
                }

                composable(BottomNavItem.Profile.route) {
                    ProfileScreen(
                        userId = null,
                        onNavigateToSettings = onNavigateToConfig,
                        onNavigateToDetail = onNavigateToBookDetail,
                        onNavigateToList = onNavigateToList,
                        onBackClick = { bottomNavController.popBackStack() }
                    )
                }
            }
        }

        if (showInteractiveTour) {
            SpotlightTourOverlay(
                onFinish = {
                    // 🔥 ADVERTENCIA CORREGIDA: Usando la función KTX `edit` de AndroidX
                    sharedPrefs.edit { putBoolean("has_seen_spotlight_tour", true) }
                    showInteractiveTour = false
                }
            )
        }
    }
}

// --- COMPONENTES DEL FOCO DE LUZ ---

@Composable
fun SpotlightTourOverlay(onFinish: () -> Unit) {
    var currentStep by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    // 🔥 MAGIA: Guardamos el tamaño exacto de la pantalla en una variable
    var boxSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    // Usamos un Box normal, Android Studio ya no se quejará
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding() // Protegemos contra la botonera
            .onSizeChanged { boxSize = it } // Leemos el tamaño en tiempo real
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        // Esperamos un milisegundo a que Android calcule el tamaño antes de dibujar
        if (boxSize.width == 0 || boxSize.height == 0) return@Box

        val screenWidth = boxSize.width.toFloat()
        val screenHeight = boxSize.height.toFloat()

        // CONTROLES INDEPENDIENTES
        // Altura general
        val moverArriba = with(density) { 0.dp.toPx() }

        // Ajuste horizontal del Buscador (Paso 1)
        val ajusteBuscadorX = with(density) { -10.dp.toPx() }

        // Ajuste horizontal de los Botones (Pasos 2 al 6)
        val ajusteBotonesX = with(density) { 2.dp.toPx() }

        val bottomNavY = screenHeight - with(density) { 40.dp.toPx() } - moverArriba
        val searchBarY = with(density) { 105.dp.toPx() } - moverArriba

        val sectionWidth = screenWidth / 5f

        val targetOffset = when (currentStep) {
            // Buscador: Usamos su propio ajuste para que quede centrado
            1 -> Offset((screenWidth / 2f) - ajusteBuscadorX, searchBarY)

            // Botones: Les aplicamos su propio micro-ajuste a la izquierda
            2 -> Offset((sectionWidth * 0.5f) - ajusteBotonesX, bottomNavY)
            3 -> Offset((sectionWidth * 1.5f) - ajusteBotonesX, bottomNavY)
            4 -> Offset((sectionWidth * 2.5f) - ajusteBotonesX, bottomNavY)
            5 -> Offset((sectionWidth * 3.5f) - ajusteBotonesX, bottomNavY)
            6 -> Offset((sectionWidth * 4.5f) - ajusteBotonesX, bottomNavY)
            else -> Offset(screenWidth / 2f, screenHeight / 2f)
        }

        val targetRadius = when (currentStep) {
            0 -> 0f
            1 -> with(density) { 160.dp.toPx() }
            in 2..6 -> with(density) { 40.dp.toPx() }
            else -> 0f
        }

        val animX by animateFloatAsState(targetValue = targetOffset.x, animationSpec = tween(400), label = "animX")
        val animY by animateFloatAsState(targetValue = targetOffset.y, animationSpec = tween(400), label = "animY")
        val animRadius by animateFloatAsState(targetValue = targetRadius, animationSpec = tween(400), label = "animRadius")

        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path().apply {
                fillType = PathFillType.EvenOdd

                addRect(Rect(0f, 0f, size.width, size.height))
                if (animRadius > 0f) {
                    addOval(Rect(center = Offset(animX, animY), radius = animRadius))
                }
            }
            drawPath(path = path, color = Color.Black.copy(alpha = 0.85f))
        }

        val bubblePadding = when (currentStep) {
            0 -> PaddingValues(32.dp)
            1 -> PaddingValues(top = 220.dp, start = 24.dp, end = 24.dp)
            else -> PaddingValues(bottom = 120.dp, start = 24.dp, end = 24.dp)
        }

        val bubbleAlignment = when (currentStep) {
            0 -> Alignment.Center
            1 -> Alignment.TopCenter
            else -> Alignment.BottomCenter
        }

        Box(
            modifier = Modifier.fillMaxSize().padding(bubblePadding),
            contentAlignment = bubbleAlignment
        ) {
            val stepData = getStepData(currentStep)

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth(0.95f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (stepData.highlightIcon != null) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(ColorHeaderBeige, CircleShape)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = stepData.highlightIcon,
                                contentDescription = null,
                                tint = ColorArcMediumBrown,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    Text(
                        text = stepData.title,
                        fontFamily = GuardianCity,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorArcDarkBrown,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stepData.desc,
                        fontFamily = CenturyGotic,
                        fontSize = 15.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (currentStep < 6) currentStep++ else onFinish()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ColorArcMediumBrown),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text(
                            text = if (currentStep < 6) "Siguiente" else "¡A Leer!",
                            fontWeight = FontWeight.Bold,
                            fontFamily = CenturyGotic,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// 🔥 NUEVO: Añadimos highlightIcon a la clase de datos (es opcional)
data class StepData(
    val title: String,
    val desc: String,
    val highlightIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
)

fun getStepData(step: Int): StepData = when (step) {
    0 -> StepData("¡Tu Biblioteca está lista!", "Vamos a darte un rápido recorrido por tu nueva aplicación para que conozcas sus rincones.")
    1 -> StepData("🔍 Buscador y Escáner", "Usa la barra para buscar por título y autor, o toca el icono del escáner para registrar libros físicos directamente usando su código de barras.", Icons.Default.QrCodeScanner)
    2 -> StepData("🏠 Inicio", "Descubre novedades, explora géneros y encuentra libros recomendados especialmente para ti.")
    3 -> StepData("📈 Tu Progreso", "Registra tus lecturas, marca libros pendientes y crea Diarios de Lectura inmersivos.")
    4 -> StepData("👥 Comunidad", "Añade a tus amigos para cotillear qué están leyendo y descubrir sus libros favoritos.")
    5 -> StepData("⭐ Reseñas", "Lee las opiniones de otros usuarios, publica las tuyas y debate en los comentarios.")
    6 -> StepData("👤 Tu Perfil", "Configura tu avatar, revisa tus géneros y accede a todas tus listas guardadas. ¡Todo listo!")
    else -> StepData("", "")
}