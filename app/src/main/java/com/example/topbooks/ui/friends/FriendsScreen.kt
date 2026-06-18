package com.example.topbooks.ui.friends

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.topbooks.R
import com.example.topbooks.ui.theme.*
import com.example.topbooks.utils.AvatarHelper

/**
 * Modelo de datos visual (UI Model) para representar una interacción rápida en la pantalla de amigos.
 */
data class Interaction(
    val userPhoto: String = "",
    val userName: String = "",
    val actionText: String = "",
    val bookTitle: String = ""
)

/**
 * PANTALLA PRINCIPAL DE LA SECCIÓN SOCIAL (Amigos). (Stateful Composable)
 * * Permite buscar nuevos usuarios, ver solicitudes/sugerencias y revisar la actividad
 * reciente de la comunidad.
 *
 * @param onNavigateToProfile Callback para visitar el perfil de un usuario específico.
 * @param onNavigateToActivity Callback para abrir la pantalla detallada del Feed Social.
 * @param viewModel ViewModel encargado de la lógica de búsqueda y amistades.
 */
@Composable
fun FriendsScreen(
    onNavigateToProfile: (String) -> Unit,
    onNavigateToActivity: () -> Unit,
    onNavigateToClubs: () -> Unit = {},
    viewModel: FriendsViewModel = viewModel()
) {
    // Observamos el estado global de la pantalla
    val uiState by viewModel.uiState.collectAsState()


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackGroundGeneral)
            .padding(16.dp)
    ) {
        // --- TÍTULO ---
        Text(
            text = stringResource(R.string.friends_title),
            fontFamily = CenturyGotic,
            fontSize = 28.sp,
            color = ColorArcMediumBrown,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // --- BARRA DE BÚSQUEDA DE USUARIOS ---
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) }, // Llama a la búsqueda en tiempo real
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            placeholder = { Text(stringResource(R.string.friends_search_hint), color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            trailingIcon = {
                // Muestra la 'X' para borrar solo si hay texto escrito
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.friends_action_clear), tint = Color.Gray)
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = ColorArcMediumBrown
            )
        )

        // --- ENRUTAMIENTO VISUAL CONDICIONAL ---
        // Si el usuario está escribiendo, mostramos los resultados. Si no, mostramos el Dashboard (Descubrir).
        if (uiState.searchQuery.isNotEmpty()) {

            SearchResultsList(
                results = uiState.searchResults,
                isSearching = uiState.isSearching,
                onFriendAction = { user -> viewModel.toggleFriend(user) },
                onNavigateToProfile = onNavigateToProfile
            )

        } else {
            // MODO DISCOVER / DASHBOARD
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {

                // ACCESO RÁPIDO: CLUBES DE LECTURA
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clickable { onNavigateToClubs() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ColorArcDarkBrown)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Clubes de lectura",
                                fontFamily = CenturyGotic,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Lee y discute con otros lectores",
                                fontFamily = CenturyGotic,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // 1. SECCIÓN: Mis amigos
                val myFriends = uiState.myFriends
                SocialSection(
                    title = stringResource(R.string.friends_section_my_friends),
                    isEmpty = myFriends.isEmpty(),
                    emptyMessage = stringResource(R.string.friends_empty_my_friends)
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(myFriends) { user ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(75.dp)
                                    .clickable { onNavigateToProfile(user.uid) }
                            ) {
                                UserAvatarItem(user.photoUrl)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = user.displayName,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1, // Evita que nombres largos rompan el diseño
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. SECCIÓN: Sugerencias ("Con tus mismos gustos")
                SocialSection(
                    title = stringResource(R.string.friends_section_suggestions),
                    isEmpty = uiState.suggestedUsers.isEmpty(),
                    emptyMessage = stringResource(R.string.friends_empty_suggestions)
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(uiState.suggestedUsers) { user ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(75.dp)
                                    .clickable { onNavigateToProfile(user.uid) }
                            ) {
                                UserAvatarItem(user.photoUrl)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = user.displayName,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. SECCIÓN: Interacciones recientes
                val recentInteractions = uiState.recentInteractions
                SocialSection(
                    title = stringResource(R.string.friends_section_interactions),
                    isEmpty = recentInteractions.isEmpty(),
                    emptyMessage = stringResource(R.string.friends_empty_interactions)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        recentInteractions.forEach { interaction ->
                            InteractionItem(
                                interaction = interaction,
                                onClick = onNavigateToActivity
                            )
                        }
                    }
                }

                // Espacio extra al final para que el scroll no quede tapado por la barra de navegación (BottomBar)
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

// =========================================================================================
// --- MICROCOMPONENTES DE LA INTERFAZ (STATELESS) ---
// =========================================================================================

/**
 * Muestra la lista de usuarios encontrados al utilizar la barra de búsqueda.
 * Gestiona automáticamente los estados de carga y "sin resultados".
 */
@Composable
fun SearchResultsList(
    results: List<SocialUser>,
    isSearching: Boolean,
    onFriendAction: (SocialUser) -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    if (isSearching) {
        // Indicador de carga centrado
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            CircularProgressIndicator(color = ColorArcMediumBrown, modifier = Modifier.padding(top = 20.dp))
        }
    } else if (results.isEmpty()) {
        // Mensaje de que no se ha encontrado a nadie
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Text(stringResource(R.string.friends_search_empty), color = Color.Gray, modifier = Modifier.padding(top = 20.dp))
        }
    } else {
        // Lista vertical de usuarios encontrados
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(results) { user ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToProfile(user.uid) }
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatarItem(user.photoUrl)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = user.displayName, fontWeight = FontWeight.Bold, color = ColorArcDarkBrown, modifier = Modifier.weight(1f))

                    // Botón para añadir/eliminar amigo (cambia dinámicamente según el estado 'isFriend')
                    IconButton(onClick = { onFriendAction(user) }) {
                        Icon(
                            imageVector = if (user.isFriend) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null,
                            tint = if (user.isFriend) Color(0xFF4CAF50) else ColorArcMediumBrown,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Contenedor visual estandarizado para cada bloque de la pantalla (Amigos, Sugerencias, etc).
 * Incluye un fondo marrón redondeado y gestiona automáticamente el mensaje cuando no hay datos.
 */
@Composable
fun SocialSection(
    title: String,
    isEmpty: Boolean,
    emptyMessage: String = stringResource(id = R.string.friends_section_loading_default),
    content: @Composable () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = ColorArcMediumBrown, shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, color = Color.White, fontSize = 20.sp, fontFamily = CenturyGotic, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
            if (isEmpty) {
                Box(modifier = Modifier.fillMaxWidth().height(50.dp), contentAlignment = Alignment.Center) {
                    Text(emptyMessage, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            } else {
                content()
            }
        }
    }
}

/**
 * Fila interactiva que muestra una actividad reciente de forma resumida utilizando [buildAnnotatedString]
 * para poder darle diferentes colores y grosores a cada palabra dentro de una misma frase.
 */
@Composable
fun InteractionItem(interaction: Interaction, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            UserAvatarItem(photoUrl = interaction.userPhoto, size = 40.dp)
            Spacer(modifier = Modifier.width(12.dp))

            // Construimos un texto con estilos mezclados (Negrita + Color + Normal)
            val text = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = ColorArcDarkBrown)) { append(interaction.userName) }
                append(stringResource(R.string.friends_interaction_format, interaction.actionText))
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFB9836B))) { append(interaction.bookTitle) }
            }
            Text(text = text, fontSize = 13.sp, color = Color.DarkGray, lineHeight = 16.sp)
        }
    }
}

/**
 * Componente visual inteligente para mostrar la foto de perfil de un usuario.
 * * Lógica de Fallback: Detecta automáticamente si el string proporcionado es una URL de internet (http...)
 * o si es una referencia local (ej: "capibara_1") y decide si debe usar Coil o cargar un recurso local.
 */
@Composable
fun UserAvatarItem(photoUrl: String, size: androidx.compose.ui.unit.Dp = 70.dp) {
    val imageModifier = Modifier.size(size).clip(CircleShape).background(Color.White).padding(2.dp).clip(CircleShape)

    if (photoUrl.startsWith("http")) {
        // La foto viene de Google (Autenticación)
        AsyncImage(
            model = photoUrl,
            contentDescription = null,
            modifier = imageModifier,
            contentScale = ContentScale.Crop,
            error = painterResource(id = R.drawable.capibara_1)
        )
    } else {
        // La foto es un avatar local de la app
        val resourceId = if (photoUrl.isEmpty()) R.drawable.capibara_1 else AvatarHelper.getDrawableId(photoUrl)
        Image(
            painter = painterResource(id = resourceId),
            contentDescription = null,
            modifier = imageModifier,
            contentScale = ContentScale.Crop
        )
    }
}