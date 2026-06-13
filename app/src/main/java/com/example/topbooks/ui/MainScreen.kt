package com.example.topbooks.ui

import android.content.Context
import androidx.annotation.StringRes
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
import com.example.topbooks.R
import com.example.topbooks.ui.home.HomeScreen
import com.example.topbooks.ui.navigation.BottomNavItem
import com.example.topbooks.ui.theme.*
import com.example.topbooks.ui.profile.ProfileScreen
import com.example.topbooks.ui.friends.FriendsScreen
import com.example.topbooks.ui.reviews.ReviewsScreen
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID

/**
 * PANTALLA PRINCIPAL DE LA APLICACIÓN (Main Container).
 * Actúa como el host principal que contiene la barra de navegación inferior y el
 * motor de navegación interno para las pestañas principales.
 *
 * @param onNavigateToConfig Navegación a la pantalla de configuración.
 * @param onNavigateToCategory Navegación al detalle de una categoría (Nombre, Query).
 * @param onNavigateToBookDetail Navegación al detalle técnico de un libro.
 * @param onNavigateToScanner Navegación al escáner de códigos de barras.
 * @param onNavigateToAllCategories Navegación al catálogo completo de categorías.
 * @param onNavigateToRecommended Navegación a la sección de recomendaciones extendidas.
 * @param onNavigateToFriendsActivity Navegación al muro de actividad social.
 * @param onNavigateToFriendProfile Navegación al perfil público de un amigo.
 * @param onNavigateToList Navegación a listas de usuario filtradas (Leídos, favoritos, etc).
 * @param onNavigateToJournal Navegación al diario de lectura de un libro.
 */
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
    onNavigateToJournal: (String) -> Unit,
    onNavigateToCreateStory: () -> Unit = {},
    onNavigateToStoryViewer: (userId: String) -> Unit = {},
    onNavigateToPostDetail: (postId: String) -> Unit = {},
    onNavigateToCreatePost: () -> Unit = {},
    onNavigateToClubs: () -> Unit = {}
) {
    // NavController específico para las pestañas de la barra inferior
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Control del tour interactivo mediante preferencias compartidas
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("topbooks_tour_prefs", Context.MODE_PRIVATE)
    val hasSeenTour = sharedPrefs.getBoolean("has_seen_spotlight_tour", false)

    android.util.Log.d("TOUR_DEBUG", "MainScreen creado - hasSeenTour=$hasSeenTour")

    var showInteractiveTour by remember {
        mutableStateOf(!hasSeenTour)
    }

    LaunchedEffect(showInteractiveTour) {
        android.util.Log.d("TOUR_DEBUG", "Tour visible: $showInteractiveTour")
    }

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
                                    // Evitamos acumular pantallas en el backstack al cambiar de pestaña
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
            // Grafo de navegación interno de la pantalla principal
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
                            // Generamos un ID temporal para diarios de libros no registrados en la API
                            val customBookId = "custom_${UUID.randomUUID()}"
                            onNavigateToJournal(customBookId)
                        }
                    )
                }

                composable(BottomNavItem.Friends.route) {
                    FriendsScreen(
                        onNavigateToProfile = onNavigateToFriendProfile,
                        onNavigateToActivity = onNavigateToFriendsActivity,
                        onNavigateToClubs = onNavigateToClubs
                    )
                }

                composable(BottomNavItem.Reviews.route) {
                    ReviewsScreen(
                        onBackClick = { bottomNavController.popBackStack() },
                        onBookClick = onNavigateToBookDetail,
                        onPostClick = onNavigateToPostDetail,
                        onCreateStoryClick = onNavigateToCreateStory,
                        onStoryClick = onNavigateToStoryViewer,
                        onCreatePostClick = onNavigateToCreatePost
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

        // Capa superior para el tour de bienvenida
        if (showInteractiveTour) {
            SpotlightTourOverlay(
                onFinish = {
                    android.util.Log.d("TOUR_DEBUG", "Tour terminado -> guardando flag")

                    sharedPrefs.edit { putBoolean("has_seen_spotlight_tour", true) }
                    showInteractiveTour = false
                }
            )
        }
    }
}

/**
 * COMPONENTE DE TOUR INTERACTIVO (Spotlight).
 * Utiliza un Canvas para dibujar una máscara oscura sobre la pantalla con un recorte circular
 * animado que resalta los elementos clave de la interfaz.
 */
@Composable
fun SpotlightTourOverlay(onFinish: () -> Unit) {
    var currentStep by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    var boxSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .onSizeChanged { boxSize = it }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {} // Bloqueamos clics accidentales en el fondo
            )
    ) {
        if (boxSize.width == 0 || boxSize.height == 0) return@Box

        val screenWidth = boxSize.width.toFloat()
        val screenHeight = boxSize.height.toFloat()

        // Parametrización de posiciones calculadas matemáticamente según el layout
        val moverArriba = with(density) { 0.dp.toPx() }
        val ajusteBotonesX = with(density) { 2.dp.toPx() }
        val bottomNavY = screenHeight - with(density) { 40.dp.toPx() } - moverArriba
        val searchBarY = with(density) { 105.dp.toPx() } - moverArriba

        val sectionWidth = screenWidth / 5f

        // Determinamos las coordenadas del foco según el paso actual
        val targetOffset = when (currentStep) {
            1 -> Offset(screenWidth / 2f, searchBarY)
            2 -> Offset((sectionWidth * 0.5f) - ajusteBotonesX, bottomNavY)
            3 -> Offset((sectionWidth * 1.5f) - ajusteBotonesX, bottomNavY)
            4 -> Offset((sectionWidth * 2.5f) - ajusteBotonesX, bottomNavY)
            5 -> Offset((sectionWidth * 3.5f) - ajusteBotonesX, bottomNavY)
            6 -> Offset((sectionWidth * 4.5f) - ajusteBotonesX, bottomNavY)
            else -> Offset(screenWidth / 2f, screenHeight / 2f)
        }

        // Tamaño del círculo de luz
        val targetRadius = when (currentStep) {
            0 -> 0f
            1 -> with(density) { 160.dp.toPx() }
            in 2..6 -> with(density) { 40.dp.toPx() }
            else -> 0f
        }

        // Animaciones suaves de transición entre pasos del tour
        val animX by animateFloatAsState(targetValue = targetOffset.x, animationSpec = tween(400), label = "animX")
        val animY by animateFloatAsState(targetValue = targetOffset.y, animationSpec = tween(400), label = "animY")
        val animRadius by animateFloatAsState(targetValue = targetRadius, animationSpec = tween(400), label = "animRadius")

        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path().apply {
                fillType = PathFillType.EvenOdd // Técnica para restar el círculo del rectángulo oscuro
                addRect(Rect(0f, 0f, size.width, size.height))
                if (animRadius > 0f) {
                    addOval(Rect(center = Offset(animX, animY), radius = animRadius))
                }
            }
            drawPath(path = path, color = Color.Black.copy(alpha = 0.85f))
        }

        // Configuración visual de la burbuja informativa
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
                        text = stringResource(id = stepData.titleRes),
                        fontFamily = GuardianCity,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorArcDarkBrown,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(id = stepData.descRes),
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
                            text = if (currentStep < 6) stringResource(R.string.tour_btn_next) else stringResource(R.string.tour_btn_finish),
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

/**
 * Modelo de datos para cada paso del tour de usuario.
 */
data class StepData(
    @param:StringRes val titleRes: Int,
    @param:StringRes val descRes: Int,
    val highlightIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
)

/**
 * Retorna los recursos de texto e iconos para un paso específico del tutorial.
 */
fun getStepData(step: Int): StepData = when (step) {
    0 -> StepData(R.string.tour_step0_title, R.string.tour_step0_desc)
    1 -> StepData(R.string.tour_step1_title, R.string.tour_step1_desc, Icons.Default.QrCodeScanner)
    2 -> StepData(R.string.tour_step2_title, R.string.tour_step2_desc)
    3 -> StepData(R.string.tour_step3_title, R.string.tour_step3_desc)
    4 -> StepData(R.string.tour_step4_title, R.string.tour_step4_desc)
    5 -> StepData(R.string.tour_step5_title, R.string.tour_step5_desc)
    6 -> StepData(R.string.tour_step6_title, R.string.tour_step6_desc)
    else -> StepData(R.string.tour_step0_title, R.string.tour_step0_desc)
}