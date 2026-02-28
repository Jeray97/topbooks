package com.example.topbooks.ui.profile

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.topbooks.R
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.ui.theme.*
import com.example.topbooks.utils.AvatarHelper
import java.util.Locale

@Composable
fun ProfileScreen(
    userId: String? = null,
    onNavigateToSettings: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToList: (String, String) -> Unit,
    onBackClick: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val user = state.user

    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }

    var showAvatarDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Diálogos visuales recuperados e implementados
    if (showAvatarDialog && state.isMe) {
        AvatarSelectionDialog(
            currentAvatar = user.photoURL,
            onDismiss = { showAvatarDialog = false },
            onSelect = {
                viewModel.updateAvatar(it) // TODO RESUELTO
            }
        )
    }

    if (showEditProfileDialog && state.isMe) {
        EditProfileDialog(
            currentName = user.displayName,
            currentBio = user.bio,
            onDismiss = { showEditProfileDialog = false },
            onSave = { n, b ->
                viewModel.updateProfileData(n, b) // TODO RESUELTO
            }
        )
    }

    Scaffold(
        containerColor = ColorBackGroundGeneral,
        topBar = {
            Box(contentAlignment = Alignment.CenterEnd) {
                TopBar(onBackClick = onBackClick)
                if (state.isMe) {
                    IconButton(onClick = onNavigateToSettings, modifier = Modifier.padding(end = 8.dp)) {
                        Icon(Icons.Default.Settings, null, tint = ColorArcDarkBrown, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ColorArcMediumBrown)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // AVATAR
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, ColorArcMediumBrown, CircleShape)
                        .clickable(enabled = state.isMe) { showAvatarDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    ProfileImage(user.photoURL)
                    if (state.isMe) {
                        Icon(
                            Icons.Default.Edit, null, tint = ColorArcMediumBrown,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .background(Color.White, CircleShape)
                                .padding(4.dp)
                                .size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // NOMBRE
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(enabled = state.isMe) { showEditProfileDialog = true }
                ) {
                    Text(text = user.displayName.ifEmpty { "Lector Anónimo" }, fontFamily = GuardianCity, fontSize = 28.sp, color = ColorTituloTopBooks, fontWeight = FontWeight.Bold)
                    if (state.isMe) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // BIO
                Text(
                    text = user.bio.ifEmpty { "Sin biografía aún." },
                    fontFamily = GuardianCity, fontSize = 14.sp, color = Color.Gray, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp).clickable(enabled = state.isMe) { showEditProfileDialog = true }
                )

                // BOTÓN AÑADIR/ELIMINAR AMIGO
                if (!state.isMe) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (state.isFriend) Color.LightGray.copy(alpha = 0.3f) else ColorArcMediumBrown.copy(alpha = 0.1f))
                            .clickable { viewModel.toggleFriend(user.uid, user.displayName, user.photoURL) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = if (state.isFriend) R.drawable.libro_abierto else R.drawable.libro_cerrado),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.isFriend) "Eliminar amigo" else "Añadir a amigos",
                            fontFamily = GuardianCity,
                            fontWeight = FontWeight.Bold,
                            color = if (state.isFriend) Color.Gray else ColorArcMediumBrown
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ESTADÍSTICAS
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatBox("Amigos", user.friendsCount.toString()) { onNavigateToList("friends", user.uid) }
                    StatBox("Reseñas", user.reviewsCount.toString()) { onNavigateToList("reviews", user.uid) }
                    StatBox("Leídos", user.booksCompleted.toString()) { onNavigateToList("read", user.uid) }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // FAVORITOS
                if (state.favoriteCovers.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Libros Favoritos", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ColorArcDarkBrown)
                            if (state.favoriteIds.size > 5) {
                                Text("Ver todos", color = ColorArcMediumBrown, fontSize = 12.sp, modifier = Modifier.clickable { onNavigateToList("favorites", user.uid) })
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(state.favoriteCovers.size) { index ->
                                val coverUrl = state.favoriteCovers[index]
                                val bookId = state.favoriteIds.getOrNull(index) ?: ""
                                AsyncImage(
                                    model = coverUrl, contentDescription = null,
                                    modifier = Modifier
                                        .size(80.dp, 120.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onNavigateToDetail(bookId) },
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }

                // SECCIÓN GÉNEROS
                ProfileGenresSection(user.favoriteGenres)

                Spacer(modifier = Modifier.height(32.dp))

                // GRID DASHBOARD
                ProfileDashboardGrid(state, user.uid, onNavigateToList)

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun ProfileImage(photoUrl: String) {
    if (photoUrl.startsWith("http")) {
        AsyncImage(model = photoUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    } else {
        Image(painter = painterResource(AvatarHelper.getDrawableId(photoUrl)), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
fun StatBox(label: String, value: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ColorArcDarkBrown)
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun ProfileGenresSection(genres: List<String>) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f)), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Géneros Favoritos", fontWeight = FontWeight.Bold, color = ColorArcDarkBrown, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            if (genres.isEmpty()) {
                Text("No hay géneros seleccionados.", fontSize = 12.sp, color = Color.Gray)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    items(genres) { ProfileGenreItem(it) }
                }
            }
        }
    }
}

@Composable
fun ProfileGenreItem(genreCode: String) {
    val (iconRes, nameRes) = getCategoryResources(genreCode)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, ColorArcMediumBrown, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (nameRes != null) stringResource(id = nameRes) else formatFallbackName(genreCode),
            fontSize = 11.sp, color = ColorArcDarkBrown, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, lineHeight = 12.sp, maxLines = 2
        )
    }
}

@Composable
fun ProfileDashboardGrid(state: ProfileUiState, userId: String, onNavigateToList: (String, String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardItem(
                title = if(state.isMe) "Mis diarios" else "Sus diarios",
                onClick = { onNavigateToList("journals", userId) }
            ) {
                Icon(Icons.Default.Book, null, tint = Color(0xFF9575CD), modifier = Modifier.size(32.dp))
            }
            DashboardItem(
                title = if(state.isMe) "Mis marcadores" else "Sus marcadores",
                onClick = { onNavigateToList("bookmarks", userId) }
            ) {
                Icon(Icons.Default.Bookmark, null, tint = Color(0xFFFFD54F), modifier = Modifier.size(32.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardItem(
                title = if(state.isMe) "Mis reseñas" else "Sus reseñas",
                onClick = { onNavigateToList("reviews", userId) }
            ) {
                Row { repeat(3) { Icon(Icons.Default.Star, null, tint = Color(0xFFFFD54F), modifier = Modifier.size(24.dp)) } }
            }
            DashboardItem(
                title = if(state.isMe) "Mis comentarios" else "Sus comentarios",
                onClick = { onNavigateToList("comments", userId) }
            ) {
                Icon(Icons.AutoMirrored.Filled.Comment, null, tint = Color(0xFF9575CD), modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
fun DashboardItem(title: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().height(95.dp).clickable { onClick() }
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = ColorArcDarkBrown, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { content() }
        }
    }
}

@Composable
fun AvatarSelectionDialog(currentAvatar: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Elige tu avatar") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AvatarHelper.avatars) { (name, res) ->
                    Image(painterResource(res), null, Modifier.size(60.dp).clip(CircleShape)
                        .border(if(currentAvatar==name) 3.dp else 0.dp, ColorArcMediumBrown, CircleShape)
                        .clickable { onSelect(name); onDismiss() })
                }
            }
        }, confirmButton = {}
    )
}

@Composable
fun EditProfileDialog(currentName: String, currentBio: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var n by remember { mutableStateOf(currentName) }
    var b by remember { mutableStateOf(currentBio) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Editar Perfil") },
        text = {
            Column {
                OutlinedTextField(value = n, onValueChange = { n = it }, label = { Text("Nombre") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = b, onValueChange = { b = it }, label = { Text("Bio") })
            }
        },
        confirmButton = { Button(onClick = { onSave(n, b); onDismiss() }) { Text("Guardar") } }
    )
}

fun getCategoryResources(code: String): Pair<Int, Int?> {
    return when (code.uppercase(Locale.ROOT).trim()) {
        "HISTORY", "HISTORIA" -> Pair(R.drawable.cat_historia_icon, R.string.cat_historia_text)
        "FANTASY", "FANTASIA" -> Pair(R.drawable.cat_fantasia_icon, R.string.cat_fantasia_text)
        "SCIFI", "CIENCIA_FICCION" -> Pair(R.drawable.cat_ciencia_ficcion_icon, R.string.cat_ciencia_ficcion_text)
        "ROMANCE" -> Pair(R.drawable.cat_romance_icon, R.string.cat_romance_text)
        "MYSTERY", "MISTERIO" -> Pair(R.drawable.cat_misterio_icon, R.string.cat_misterio_text)
        "MANGA" -> Pair(R.drawable.cat_manga_icon, R.string.cat_manga_text)
        "KIDS", "INFANTIL" -> Pair(R.drawable.cat_infantil_icon, R.string.cat_infantil_text)
        "PHILOSOPHY", "FILOSOFIA" -> Pair(R.drawable.cat_filosofia_icon, R.string.cat_filosofia_text)
        "POETRY", "POESIA" -> Pair(R.drawable.cat_poesia_icon, R.string.cat_poesia_text)
        "GRAPHIC_NOVEL", "NOVELA_GRAFICA" -> Pair(R.drawable.cat_novela_grafica_icon, R.string.cat_novela_grafica_text)
        "ADVENTURE", "AVENTURAS" -> Pair(R.drawable.cat_aventura_icon, R.string.cat_aventura_text)
        "RELIGION" -> Pair(R.drawable.cat_religion_icon, R.string.cat_religion_text)
        else -> Pair(R.drawable.home_icon, null)
    }
}

fun formatFallbackName(code: String): String {
    return code.lowercase()
        .replace("_", " ")
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
}