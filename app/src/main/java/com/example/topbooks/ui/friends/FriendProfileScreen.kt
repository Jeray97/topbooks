package com.example.topbooks.ui.friends

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.topbooks.R
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.model.Review
import com.example.topbooks.data.model.User
import com.example.topbooks.ui.components.TopBar
// import com.example.topbooks.ui.profile.DashboardCard // Usamos versión local para asegurar estilo
import com.example.topbooks.ui.theme.*
import com.example.topbooks.utils.AvatarHelper
import com.example.topbooks.utils.Resource
import java.util.Locale

@Composable
fun FriendProfileScreen(
    userId: String,
    onBackClick: () -> Unit,
    // Callback opcional para navegar al libro si tocas una portada
    onBookClick: (String) -> Unit = {},
    viewModel: FriendProfileViewModel = viewModel()
) {
    val state by viewModel.friendState.collectAsState()
    val reviews by viewModel.reviews.collectAsState()
    val favoriteBooks by viewModel.favoriteBooks.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadFriendProfile(userId)
    }

    Scaffold(
        // FONDO OSCURO: Igual que en ProfileScreen
        containerColor = ColorArcDarkBrown,
        topBar = { TopBar(onBackClick = onBackClick) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val resource = state) {
                is Resource.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = ColorArcMediumBrown
                    )
                }
                is Resource.Error -> {
                    Text(
                        text = "No se pudo cargar el perfil",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White
                    )
                }
                is Resource.Success -> {
                    FriendProfileContent(
                        user = resource.data,
                        favoriteBooks = favoriteBooks,
                        reviews = reviews,
                        onBookClick = onBookClick
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
fun FriendProfileContent(
    user: User,
    favoriteBooks: List<Book>, // Recibimos libros
    reviews: List<Review>,     // Recibimos reseñas
    onBookClick: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // Scroll vertical habilitado
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- AVATAR ---
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(2.dp, ColorArcMediumBrown, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (user.photoURL.startsWith("http")) {
                AsyncImage(
                    model = user.photoURL,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = AvatarHelper.getDrawableId(user.photoURL)),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- NOMBRE ---
        Text(
            text = user.displayName,
            fontFamily = GuardianCity,
            fontSize = 28.sp,
            color = ColorTituloTopBooks,
            fontWeight = FontWeight.Bold
        )

        // --- BIO ---
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            // Usamos la bio del usuario si existe, si no el texto por defecto
            text = if (user.bio.isNotEmpty()) user.bio else "Amante de la lectura y el café.",
            fontFamily = GuardianCity,
            fontSize = 14.sp,
            color = Color.Gray,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- ESTADÍSTICAS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatBox("Amigos", user.friendsCount.toString())
            StatBox("Reseñas", user.reviewsCount.toString())
            StatBox("Leídos", user.booksCompleted.toString())
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- TARJETA DE GUSTOS ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Géneros Favoritos",
                    fontWeight = FontWeight.Bold,
                    color = ColorArcDarkBrown,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (user.favoriteGenres.isEmpty()) {
                    Text("Aún no ha seleccionado géneros.", fontSize = 14.sp, color = Color.Gray)
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(user.favoriteGenres) { genreCode ->
                            GenreItem(genreCode = genreCode)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- GRID 2X2 (Estilo ProfileScreen) ---
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // COLUMNA IZQUIERDA
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // 1. Sus Favoritos
                FriendDashboardCard("Sus favoritos") {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (favoriteBooks.isEmpty()) {
                            Image(painterResource(R.drawable.cat_resenas_icon), null, Modifier.size(30.dp, 45.dp), alpha = 0.5f)
                        } else {
                            favoriteBooks.take(3).forEach { book ->
                                AsyncImage(
                                    model = book.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(30.dp, 45.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { onBookClick(book.id) },
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }

                // 2. Sus Marcadores (Simulado visualmente)
                FriendDashboardCard("Sus marcadores") {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Favorite, null, tint = Color.Gray)
                        Icon(Icons.Default.Favorite, null, tint = Color(0xFFFFD54F))
                        Icon(Icons.Default.Favorite, null, tint = Color.Gray)
                    }
                }
            }

            // COLUMNA DERECHA
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // 3. Sus Reseñas
                FriendDashboardCard("Sus reseñas") {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (reviews.isEmpty()) {
                            Icon(Icons.Default.Star, null, tint = Color.Gray)
                        } else {
                            // Mostramos hasta 3 estrellas doradas
                            repeat(minOf(reviews.size, 3)) {
                                Icon(Icons.Default.Star, null, tint = Color(0xFFFFD54F))
                            }
                        }
                    }
                }

                // 4. Sus Comentarios
                FriendDashboardCard("Sus comentarios") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("${user.commentsCount}", fontWeight = FontWeight.Bold, color = ColorArcDarkBrown, fontSize = 18.sp)
                        Icon(Icons.Default.AccountCircle, null, tint = Color(0xFF9575CD))
                    }
                }
            }
        }

        // Espacio extra al final para el scroll
        Spacer(modifier = Modifier.height(80.dp))
    }
}

// --- COMPONENTE LOCAL PARA EL GRID ---
@Composable
fun FriendDashboardCard(title: String, iconContent: @Composable () -> Unit) {
    Surface(
        color = Color.White, // <--- CAMBIO: FONDO BLANCO COMO PEDISTE
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(95.dp)
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                title,
                color = ColorArcDarkBrown,
                fontFamily = CenturyGotic,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Box(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                iconContent()
            }
        }
    }
}

@Composable
fun GenreItem(genreCode: String) {
    // 1. Obtenemos los recursos basados en el CÓDIGO (Ej: "FANTASY")
    val (iconRes, nameRes) = getCategoryResources(genreCode)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp)
    ) {
        // ICONO
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, ColorArcMediumBrown, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // TEXTO: Si encontramos traducción, la usamos. Si no, mostramos el código original formateado.
        Text(
            text = if (nameRes != null) stringResource(id = nameRes) else formatFallbackName(genreCode),
            fontSize = 11.sp,
            color = ColorArcDarkBrown,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp,
            maxLines = 2
        )
    }
}

fun getCategoryResources(code: String): Pair<Int, Int?> {
    // Normalizamos a mayúsculas para evitar errores (ej: "Fantasy" -> "FANTASY")
    return when (code.uppercase(Locale.ROOT).trim()) {
        "HISTORY", "HISTORIA" -> Pair(R.drawable.cat_historia_icon, R.string.cat_historia_text)
        "FANTASY", "FANTASIA" -> Pair(R.drawable.cat_fantasia_icon, R.string.cat_fantasia_text)
        "SCIFI", "CIENCIA_FICCION" -> Pair(R.drawable.cat_ciencia_ficcion_icon, R.string.cat_ciencia_ficcion_text)
        "ROMANCE" -> Pair(R.drawable.cat_romance_icon, R.string.cat_romance_text)
        "MYSTERY", "MISTERIO" -> Pair(R.drawable.cat_misterio_icon, R.string.cat_misterio_text)
        "MANGA" -> Pair(R.drawable.cat_manga_icon, R.string.cat_manga_text)
        "FANTASY", "FANTASIA" -> Pair(R.drawable.cat_fantasia_icon, R.string.cat_fantasia_text)
        "KIDS", "INFANTIL" -> Pair(R.drawable.cat_infantil_icon, R.string.cat_infantil_text)
        "PHILOSOPHY", "FILOSOFIA" -> Pair(R.drawable.cat_filosofia_icon, R.string.cat_filosofia_text)
        "POETRY", "POESIA" -> Pair(R.drawable.cat_poesia_icon, R.string.cat_poesia_text)
        "GRAPHIC_NOVEL", "NOVELA_GRAFICA" -> Pair(R.drawable.cat_novela_grafica_icon, R.string.cat_novela_grafica_text)
        "ADVENTURE", "AVENTURAS" -> Pair(R.drawable.cat_aventura_icon, R.string.cat_aventura_text)
        "SCIFI", "CIENCIA_FICCION" -> Pair(R.drawable.cat_ciencia_ficcion_icon, R.string.cat_ciencia_ficcion_text)
        "RELIGION" -> Pair(R.drawable.cat_religion_icon, R.string.cat_religion_text)

        // Fallback: Si llega un código nuevo que no conocemos
        else -> Pair(R.drawable.home_icon, null)
    }
}

// Función auxiliar para que si falla el mapeo, no se vea "CATEGORY_UNKNOWN" sino "Category Unknown"
fun formatFallbackName(code: String): String {
    return code.lowercase()
        .replace("_", " ")
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
}

@Composable
fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ColorArcDarkBrown
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FriendProfileScreenPreview() {
    val dummyUser = User(
        displayName = "Capibara Lector",
        photoURL = "capibara_2",
        friendsCount = 42,
        reviewsCount = 12,
        booksCompleted = 7,
        // Simulamos que la DB devuelve CÓDIGOS
        favoriteGenres = listOf("FANTASY", "SCIFI", "HISTORY", "MANGA", "HORROR", "RELIGION")
    )

    Scaffold(
        containerColor = ColorBackGroundGeneral,
        topBar = { TopBar(onBackClick = {}) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            FriendProfileContent(user = dummyUser, favoriteBooks = emptyList(), reviews = emptyList())
        }
    }
}