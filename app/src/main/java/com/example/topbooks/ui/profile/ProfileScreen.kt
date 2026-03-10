package com.example.topbooks.ui.profile

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
import com.example.topbooks.utils.CategoryProvider

/**
 * PANTALLA DE PERFIL (Stateful Composable).
 * Esta pantalla es polimórfica: se adapta para mostrar el perfil del usuario actual (con opciones de edición)
 * o el perfil de un tercero (con opciones de red social como añadir amigo).
 *
 * @param userId ID del usuario a visualizar. Si es null, se asume el perfil del usuario autenticado.
 * @param onNavigateToSettings Navega a la configuración de la app.
 * @param onNavigateToDetail Navega al detalle de un libro específico.
 * @param onNavigateToList Navega a listas filtradas (reseñas, diarios, etc.).
 * @param onBackClick Acción para regresar.
 * @param viewModel Lógica de negocio y gestión de estado del perfil.
 */
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

    // Disparar la carga del perfil cada vez que el userId cambie
    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }

    // Estados para el control de diálogos de edición
    var showAvatarDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }


    // --- DIÁLOGOS DE EDICIÓN (Solo activos si es MI perfil) ---
    if (showAvatarDialog && state.isMe) {
        AvatarSelectionDialog(
            currentAvatar = user.photoURL,
            onDismiss = { showAvatarDialog = false },
            onSelect = { viewModel.updateAvatar(it) }
        )
    }

    if (showEditProfileDialog && state.isMe) {
        EditProfileDialog(
            currentName = user.displayName,
            currentBio = user.bio,
            onDismiss = { showEditProfileDialog = false },
            onSave = { n, b -> viewModel.updateProfileData(n, b) }
        )
    }

    Scaffold(
        containerColor = ColorBackGroundGeneral,
        topBar = {
            Box(contentAlignment = Alignment.CenterEnd) {
                TopBar(onBackClick = onBackClick)
                // Mostrar botón de ajustes solo en el perfil propio
                if (state.isMe) {
                    IconButton(onClick = onNavigateToSettings, modifier = Modifier.padding(end = 8.dp)) {
                        Icon(Icons.Default.Settings, null, tint = ColorArcDarkBrown, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    ) { padding ->
        if (state.isLoading) {
            // Pantalla de carga centralizada
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
                // --- 1. SECCIÓN: AVATAR ---
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, ColorArcMediumBrown, CircleShape)
                        // El clic para editar solo funciona si es Mi Perfil
                        .clickable(enabled = state.isMe) { showAvatarDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    ProfileImage(user.photoURL)
                    if (state.isMe) {
                        // Icono de edición flotante
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

                // --- 2. SECCIÓN: NOMBRE Y BIO ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(enabled = state.isMe) { showEditProfileDialog = true }
                ) {
                    Text(
                        text = user.displayName.ifEmpty { stringResource(R.string.profile_anonymous_reader) },
                        fontFamily = GuardianCity,
                        fontSize = 28.sp,
                        color = ColorTituloTopBooks,
                        fontWeight = FontWeight.Bold
                    )
                    if (state.isMe) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = user.bio.ifEmpty { stringResource(R.string.profile_no_bio_yet) },
                    fontFamily = GuardianCity, fontSize = 14.sp, color = Color.Gray, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp).clickable(enabled = state.isMe) { showEditProfileDialog = true }
                )

                // --- 3. SECCIÓN: RED SOCIAL (Solo visible en perfiles ajenos) ---
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
                            text = if (state.isFriend) stringResource(R.string.profile_remove_friend) else stringResource(R.string.profile_add_friend),
                            fontFamily = GuardianCity,
                            fontWeight = FontWeight.Bold,
                            color = if (state.isFriend) Color.Gray else ColorArcMediumBrown
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // --- 4. SECCIÓN: GÉNEROS FAVORITOS ---
                ProfileGenresSection(user.favoriteGenres)

                Spacer(modifier = Modifier.height(32.dp))

                // --- 5. GRID DE ACCESOS DIRECTOS (DASHBOARD) ---
                ProfileDashboardGrid(state, user.uid, onNavigateToList)

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

/**
 * Componente que renderiza la imagen del perfil.
 * Diferencia automáticamente entre una URL de red (Google) o un avatar local (Capibara).
 */
@Composable
fun ProfileImage(photoUrl: String) {
    if (photoUrl.startsWith("http")) {
        AsyncImage(
            model = photoUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    } else {
        Image(
            painter = painterResource(AvatarHelper.getDrawableId(photoUrl)),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

/**
 * Sección horizontal que muestra los géneros favoritos del usuario utilizando iconos temáticos.
 */
@Composable
fun ProfileGenresSection(genres: List<String>) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f)), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.profile_favorite_genres), fontWeight = FontWeight.Bold, color = ColorArcDarkBrown, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            if (genres.isEmpty()) {
                Text(stringResource(R.string.profile_no_genres_selected), fontSize = 12.sp, color = Color.Gray)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    items(genres) { ProfileGenreItem(it) }
                }
            }
        }
    }
}

/**
 * Representa un ícono de género individual dentro del perfil.
 */
@Composable
fun ProfileGenreItem(genreCode: String) {
    val categoryData = CategoryProvider.getCategoryResources(genreCode)

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, ColorArcMediumBrown, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = categoryData.iconRes),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (categoryData.nameRes != null) stringResource(id = categoryData.nameRes)
            else CategoryProvider.formatFallbackName(genreCode),
            fontSize = 11.sp,
            color = ColorArcDarkBrown,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp,
            maxLines = 2
        )
    }
}

/**
 * Rejilla de botones del dashboard (Diarios, Marcadores, Reseñas, Comentarios).
 */
@Composable
fun ProfileDashboardGrid(state: ProfileUiState, userId: String, onNavigateToList: (String, String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardItem(
                title = if(state.isMe) stringResource(R.string.profile_my_journals) else stringResource(R.string.profile_their_journals),
                onClick = { onNavigateToList("journals", userId) }
            ) {
                Icon(Icons.Default.Book, null, tint = Color(0xFF9575CD), modifier = Modifier.size(32.dp))
            }
            DashboardItem(
                title = if(state.isMe) stringResource(R.string.profile_my_bookmarks) else stringResource(R.string.profile_their_bookmarks),
                onClick = { onNavigateToList("bookmarks", userId) }
            ) {
                Icon(Icons.Default.Bookmark, null, tint = Color(0xFFFFD54F), modifier = Modifier.size(32.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardItem(
                title = if(state.isMe) stringResource(R.string.profile_my_reviews) else stringResource(R.string.profile_their_reviews),
                onClick = { onNavigateToList("reviews", userId) }
            ) {
                Row { repeat(3) { Icon(Icons.Default.Star, null, tint = Color(0xFFFFD54F), modifier = Modifier.size(24.dp)) } }
            }
            DashboardItem(
                title = if(state.isMe) stringResource(R.string.profile_my_comments) else stringResource(R.string.profile_their_comments),
                onClick = { onNavigateToList("comments", userId) }
            ) {
                Icon(Icons.AutoMirrored.Filled.Comment, null, tint = Color(0xFF9575CD), modifier = Modifier.size(32.dp))
            }
        }
    }
}

/**
 * Componente individual para los botones del Dashboard.
 */
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

/**
 * Diálogo interactivo para seleccionar un nuevo avatar de la colección local.
 */
@Composable
fun AvatarSelectionDialog(currentAvatar: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.profile_choose_avatar)) },
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

/**
 * Diálogo de edición para actualizar el nombre público y la biografía del lector.
 */
@Composable
fun EditProfileDialog(currentName: String, currentBio: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var n by remember { mutableStateOf(currentName) }
    var b by remember { mutableStateOf(currentBio) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.profile_edit_profile_title)) },
        text = {
            Column {
                OutlinedTextField(value = n, onValueChange = { n = it }, label = { Text(stringResource(R.string.profile_edit_name_label)) })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = b, onValueChange = { b = it }, label = { Text(stringResource(R.string.profile_edit_bio_label)) })
            }
        },
        confirmButton = { Button(onClick = { onSave(n, b); onDismiss() }) { Text(stringResource(R.string.profile_edit_save_button)) } }
    )
}