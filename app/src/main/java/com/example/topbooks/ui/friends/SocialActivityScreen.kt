package com.example.topbooks.ui.friends

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.topbooks.R
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.ui.theme.*
import com.example.topbooks.utils.AvatarHelper
import com.example.topbooks.utils.Resource

/**
 * PANTALLA PRINCIPAL DE ACTIVIDAD SOCIAL (Feed General). (Stateful Composable)
 * * A diferencia del feed básico, este muestra interacciones complejas como Respuestas anidadas (Replies)
 * y adiciones a Favoritos, diferenciándolas por color y formato.
 *
 * @param onBackClick Acción para navegar hacia atrás.
 * @param onBookClick Callback para abrir los detalles de un libro al tocar su portada.
 * @param onCommentClick Callback para navegar directamente a la sección de un comentario específico.
 * @param viewModel ViewModel encargado de cargar y proveer el flujo de actividades.
 */
@Composable
fun SocialActivityScreen(
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    onCommentClick: (String, String) -> Unit,
    viewModel: SocialActivityViewModel = viewModel()
) {
    // Observamos el estado reactivo que maneja la carga, éxito y error (Resource)
    val state by viewModel.uiState.collectAsState()

    // Disparamos la carga del feed solo la primera vez que la pantalla entra en composición
    LaunchedEffect(Unit) {
        viewModel.loadActivityFeed()
    }

    Scaffold(
        containerColor = ColorBackGroundGeneral(),
        topBar = { TopBar(onBackClick = onBackClick) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Título superior
            Text(
                text = stringResource(R.string.social_activity_title),
                fontFamily = CenturyGotic,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTituloTopBooks(),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            // --- MÁQUINA DE ESTADOS (Resource) ---
            when (val resource = state) {
                is Resource.Loading -> {
                    // Estado de carga: Muestra el spinner circular centrado
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ColorArcMediumBrown())
                    }
                }
                is Resource.Success -> {
                    if (resource.data.isEmpty()) {
                        // Si la lista de actividad llega vacía, mostramos el componente Placeholder
                        EmptySocialMessage(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        // Renderizamos la lista dinámica de actividades
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            items(resource.data) { item ->
                                SocialActivityCard(
                                    item = item,
                                    onBookClick = onBookClick,
                                    onCommentClick = onCommentClick
                                )
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    // Fallback visual en caso de error de red (muestra la pantalla vacía)
                    EmptySocialMessage(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                else -> {}
            }
        }
    }
}

/**
 * Componente visual que representa una única tarjeta de actividad en el feed.
 * * TÉCNICA VISUAL: Utiliza colores pastel de fondo según el tipo de acción y puede mostrar
 * "citas" anidadas si la acción es una respuesta a otro usuario.
 */
@Composable
fun SocialActivityCard(
    item: SocialActivityItem,
    onBookClick: (String) -> Unit,
    onCommentClick: (String, String) -> Unit
) {
    // Definimos el color del globo (burbuja de chat) basado en el tipo de actividad
    val bubbleColor = when(item.type) {
        ActivityType.FAVORITE -> Color(0xFFFCE4EC) // Tono rosado para Favoritos
        ActivityType.REPLY -> Color(0xFFE3F2FD)    // Tono azulado para Respuestas
        else -> ColorHeaderBeige().copy(alpha = 0.9f) // Beige por defecto para Reseñas y Comentarios
    }

    Row(
        // IntrinsicSize.Min fuerza a que todos los elementos de la Row midan lo mismo que el más alto
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- COLUMNA IZQ: PORTADA DEL LIBRO ---
        Card(
            modifier = Modifier.width(90.dp).height(140.dp)
                .shadow(6.dp, RoundedCornerShape(8.dp))
                .clickable { onBookClick(item.bookId) },
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(3.dp, Color.White),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.bookImageUrl)
                    .crossfade(true)
                    .error(R.drawable.cat_resenas_icon) // Fallback si no hay portada
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // --- COLUMNA DER: GLOBO DE CONTENIDO ---
        Card(
            modifier = Modifier.weight(1f).fillMaxHeight().padding(vertical = 4.dp)
                .clickable {
                    // Navegación condicional: Si es un comentario/respuesta, vamos al hilo. Si no, al libro.
                    if (item.commentId != null) {
                        onCommentClick(item.bookId, item.commentId)
                    } else {
                        onBookClick(item.bookId)
                    }
                },
            // Forma asimétrica para simular un globo de chat (esquina inferior izquierda recta)
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 2.dp),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp).fillMaxSize()) {

                // 1. Cabecera dinámica (ej: "Juan ha comentado:", "Ana respondió a Pedro:")
                val actionText = when(item.type) {
                    ActivityType.REVIEW -> stringResource(R.string.social_action_review)
                    ActivityType.FAVORITE -> stringResource(R.string.social_action_favorite)
                    ActivityType.COMMENT -> stringResource(R.string.social_action_comment)
                    ActivityType.REPLY -> stringResource(R.string.social_action_reply, item.replyToName ?: stringResource(R.string.social_default_user))
                }

                Text(
                    text = stringResource(R.string.social_action_format, item.friendName, actionText),
                    fontSize = 12.sp,
                    color = ColorArcDarkBrown(),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.bookTitle,
                    fontSize = 11.sp,
                    color = Color(0xFF8D6E63),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Lógica Condicional: Renderizado del texto original al que se está respondiendo (Quote)
                if (item.type == ActivityType.REPLY && item.replyToContent != null) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.05f), // Fondo oscurecido para resaltar la cita
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = item.replyToName ?: stringResource(R.string.social_default_user),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Text(
                                text = item.replyToContent,
                                fontSize = 11.sp,
                                color = Color.Gray,
                                maxLines = 2,
                                fontStyle = FontStyle.Italic, // Cursiva para diferenciarlo del texto principal
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // 3. Contenido principal de la actividad (El comentario, reseña o estado nuevo)
                Text(
                    text = item.content,
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    lineHeight = 15.sp,
                    modifier = Modifier.weight(1f) // Empuja el pie del globo hacia abajo
                )

                // 4. Pie del globo: Avatar pequeño a la izquierda, Ícono de tipo de actividad a la derecha
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SmallAvatar(item.friendPhotoUrl)

                    // Condicional para mostrar estrellas (Reseñas) o un corazón (Favoritos)
                    if (item.type == ActivityType.REVIEW && item.rating > 0) {
                        Row {
                            Icon(Icons.Default.Star, null, tint = Color(0xFFFFD54F), modifier = Modifier.size(12.dp))
                            Text("${item.rating}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (item.type == ActivityType.FAVORITE) {
                        Icon(Icons.Default.Favorite, null, tint = Color(0xFFE57373), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

/**
 * Micro-componente que renderiza un avatar circular en miniatura.
 * Implementa la misma lógica de fallback inteligente que hemos visto en otras pantallas
 * (distinguiendo entre URLs web y recursos locales).
 */
@Composable
fun SmallAvatar(photoUrl: String) {
    val modifier = Modifier.size(24.dp).clip(CircleShape).border(1.dp, Color.White, CircleShape).background(Color.White)
    if (photoUrl.startsWith("http")) {
        AsyncImage(model = photoUrl, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    } else {
        Image(painter = painterResource(AvatarHelper.getDrawableId(photoUrl)), contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    }
}

/**
 * Pantalla vacía (Placeholder) que se muestra si el feed aún no tiene contenido o hubo un error de red.
 */
@Composable
fun EmptySocialMessage(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(painter = painterResource(R.drawable.social), contentDescription = null, tint = ColorArcMediumBrown().copy(alpha = 0.4f), modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.social_empty_title), fontWeight = FontWeight.Bold, color = Color.Gray)
        Text(stringResource(R.string.social_empty_desc), fontSize = 12.sp, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}