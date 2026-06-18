package com.example.topbooks.ui.community

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.utils.AvatarHelper
import com.example.topbooks.ui.theme.CenturyGotic
import com.example.topbooks.ui.theme.ColorArcDarkBrown
import com.example.topbooks.ui.theme.ColorArcMediumBrown
import com.example.topbooks.ui.theme.ColorBackGroundGeneral
import com.example.topbooks.ui.theme.ColorJournalRomance
import com.example.topbooks.ui.theme.ColorJournalStar
import com.example.topbooks.ui.theme.ColorTextPrimary
import com.example.topbooks.ui.theme.ColorTituloTopBooks
import com.example.topbooks.ui.theme.GuardianCity

/* =============================================================================
 *  PANTALLA: FEED COMUNIDAD
 * =============================================================================
 *  Pantalla principal del feed social. Implementa el Mockup 1:
 *    - TopBar oficial reutilizada
 *    - Cabecera "Comunidad" + subtítulo dinámico
 *    - 3 tabs (Comunidad / Amigos / Top)
 *    - Story-bar horizontal con amigos lectores
 *    - LazyColumn de cards (reseña / cita / terminado)
 *    - FAB para crear post
 *
 *  Esta pantalla es independiente — se monta directamente cuando estás en la
 *  pestaña Reseñas del bottom nav. La pantalla anterior (hilo de un libro)
 *  sigue funcionando porque ReviewsScreen.kt la sigue mostrando cuando
 *  bookId != null.
 * ============================================================================= */

private val SUB_TEXT = Color(0xFF8D5B4C)
private val LIKE_COLOR = ColorJournalRomance        // #FF4081
private val SAVE_COLOR = Color(0xFFB9836B)           // marrón dorado
private val CARD_BG = Color.White
private val CARD_BORDER = Color(0xFFECDDD2)
private val ACTION_TAG_BG = Color(0xFFF6E6DD)
private val BOOK_STRIP_BG = Color(0xFFF6E6DD)
private val FRIEND_RING_GRADIENT = listOf(Color(0xFFB9836B), Color(0xFF8D5B4C))
private val COMMUNITY_RING = Color(0xFFC89B8C)
private val TAB_INACTIVE_BG = Color(0xFFFFFFFF).copy(alpha = 0.6f)
private val TAB_ACTIVE_BG = Color(0xFF8D5B4C)

@Composable
fun CommunityFeedScreen(
    onBackClick: () -> Unit,
    onPostClick: (Post) -> Unit,
    onCreatePostClick: () -> Unit,
    onCreateStoryClick: () -> Unit = {},
    onStoryClick: (userId: String) -> Unit = {},
    viewModel: CommunityViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = ColorBackGroundGeneral,
        //topBar = { TopBar(onBackClick = onBackClick) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreatePostClick,
                containerColor = ColorArcDarkBrown,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Compartir", fontFamily = CenturyGotic) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ─── CABECERA ───
            item { HeaderSection(state.newPostsCountToday) }

            // ─── TABS ───
            item {
                TabsRow(
                    activeTab = state.activeTab,
                    onTabClick = { tab -> viewModel.selectTab(tab) }
                )
            }

            // ─── STORY-BAR ───
            item {
                StoryBar(
                    stories = state.stories,
                    onCreateStoryClick = onCreateStoryClick,
                    onStoryClick = onStoryClick
                )
            }

            // ─── ESTADO CARGANDO ───
            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ColorArcMediumBrown)
                    }
                }
            } else if (state.posts.isEmpty()) {
                // ─── ESTADO VACÍO ───
                item { EmptyFeedMessage(state.activeTab) }
            } else {
                // ─── LISTA DE POSTS ───
                items(state.posts, key = { it.id }) { post ->
                    Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        PostCard(
                            post = post,
                            onLikeClick = { viewModel.toggleLike(post) },
                            onSaveClick = { viewModel.toggleSave(post) },
                            onCommentClick = { onPostClick(post) },
                            onCardClick = { onPostClick(post) }
                        )
                    }
                }

                // ─── ESPACIO INFERIOR PARA NO TAPAR EL FAB ───
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}


/* ─────────────────────────────────────────────────────────────────────────────
 *  CABECERA: "Comunidad" + subtítulo dinámico
 * ───────────────────────────────────────────────────────────────────────────── */

@Composable
private fun HeaderSection(newPostsCount: Int) {
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp)
    ) {
        Text(
            text = "Comunidad",
            fontFamily = GuardianCity,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = ColorTituloTopBooks
        )
        if (newPostsCount > 0) {
            Text(
                text = if (newPostsCount == 1) "1 nueva reseña hoy"
                else "$newPostsCount nuevas reseñas hoy",
                fontFamily = CenturyGotic,
                fontSize = 12.sp,
                color = SUB_TEXT,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}


/* ─────────────────────────────────────────────────────────────────────────────
 *  TABS: Comunidad / Amigos / Top
 * ───────────────────────────────────────────────────────────────────────────── */

@Composable
private fun TabsRow(activeTab: FeedTab, onTabClick: (FeedTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TabPill(
            label = "Comunidad",
            icon = Icons.Default.Public,
            isActive = activeTab == FeedTab.COMMUNITY,
            modifier = Modifier.weight(1f),
            onClick = { onTabClick(FeedTab.COMMUNITY) }
        )
        TabPill(
            label = "Amigos",
            icon = Icons.Default.People,
            isActive = activeTab == FeedTab.FRIENDS,
            modifier = Modifier.weight(1f),
            onClick = { onTabClick(FeedTab.FRIENDS) }
        )
        TabPill(
            label = "Top",
            icon = Icons.Default.Star,
            isActive = activeTab == FeedTab.TOP,
            modifier = Modifier.weight(1f),
            onClick = { onTabClick(FeedTab.TOP) }
        )
    }
}

@Composable
private fun TabPill(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Animamos el cambio de color para que el tab "deslice" entre estados
    val bgColor by animateColorAsState(
        if (isActive) TAB_ACTIVE_BG else TAB_INACTIVE_BG,
        label = "tabBg"
    )
    val textColor by animateColorAsState(
        if (isActive) ColorBackGroundGeneral else SUB_TEXT,
        label = "tabText"
    )

    Row(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            fontFamily = CenturyGotic,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


/* ─────────────────────────────────────────────────────────────────────────────
 *  STORY-BAR: amigos lectores en una fila scrolleable
 * ───────────────────────────────────────────────────────────────────────────── */

@Composable
private fun StoryBar(
    stories: List<StoryItem>,
    onCreateStoryClick: () -> Unit,
    onStoryClick: (userId: String) -> Unit
) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AddYourReadingStory(onClick = onCreateStoryClick)

        stories.forEach { story ->
            StoryAvatar(story = story, onClick = { onStoryClick(story.author.id) })
        }
    }
}

@Composable
private fun AddYourReadingStory(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(COMMUNITY_RING)
                .padding(2.5.dp)
                .clip(CircleShape)
                .background(ColorArcDarkBrown)
                .border(2.5.dp, ColorBackGroundGeneral, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = ColorBackGroundGeneral,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Tu lectura",
            fontFamily = CenturyGotic,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = ColorTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StoryAvatar(story: StoryItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(FRIEND_RING_GRADIENT))
                .padding(2.5.dp)
                .clip(CircleShape)
                .border(2.5.dp, ColorBackGroundGeneral, CircleShape)
        ) {
            val avatarRes = AvatarHelper.getDrawableId(story.author.photoUrl)
            androidx.compose.foundation.Image(
                painter = painterResource(id = avatarRes),
                contentDescription = story.author.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = story.author.displayName.split(" ").first(),  // Solo el primer nombre
            fontFamily = CenturyGotic,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = ColorTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        story.currentBook?.let { book ->
            Text(
                text = if (story.hasFinished) "✓ Terminó"
                else "📖 ${book.title.take(8)}",
                fontFamily = CenturyGotic,
                fontSize = 9.sp,
                color = SUB_TEXT,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


/* ─────────────────────────────────────────────────────────────────────────────
 *  ESTADO VACÍO
 * ───────────────────────────────────────────────────────────────────────────── */

@Composable
private fun EmptyFeedMessage(tab: FeedTab) {
    val message = when (tab) {
        FeedTab.FRIENDS -> "Tus amigos aún no han publicado nada. ¡Anímate tú!"
        FeedTab.COMMUNITY -> "Aún no hay actividad en la comunidad."
        FeedTab.TOP -> "Todavía no hay posts destacados esta semana."
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            fontFamily = CenturyGotic,
            fontSize = 13.sp,
            color = SUB_TEXT,
            fontStyle = FontStyle.Italic
        )
    }
}


/* ─────────────────────────────────────────────────────────────────────────────
 *  POST CARD: la card que se repite en el feed
 * ───────────────────────────────────────────────────────────────────────────── */

@Composable
private fun PostCard(
    post: Post,
    onLikeClick: () -> Unit,
    onSaveClick: () -> Unit,
    onCommentClick: () -> Unit,
    onCardClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CARD_BG)
            .border(1.dp, CARD_BORDER, RoundedCornerShape(18.dp))
            .clickable(onClick = onCardClick)
            .padding(14.dp)
    ) {
        // Header: avatar + nombre + meta + tag
        PostHeader(post)

        Spacer(Modifier.height(10.dp))

        // Contenido según tipo
        when (post.type) {
            PostType.QUOTE -> QuoteContent(post)
            else -> {
                post.book?.let { BookStrip(it, post.rating) }
                if (post.body.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = post.body,
                        fontFamily = CenturyGotic,
                        fontSize = 13.sp,
                        color = ColorTextPrimary,
                        lineHeight = 20.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Acciones: like / comment / save / overflow
        PostActions(
            post = post,
            onLikeClick = onLikeClick,
            onSaveClick = onSaveClick,
            onCommentClick = onCommentClick
        )
    }
}


/* ─── HEADER DE LA CARD ─── */

@Composable
private fun PostHeader(post: Post) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Avatar con anillo según relación (amigo dorado / comunidad gris)
        AvatarWithRing(author = post.author, size = 36.dp)

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = post.author.displayName,
                    fontFamily = CenturyGotic,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (post.author.isVerified) {
                    Spacer(Modifier.width(5.dp))
                    VerifiedBadge()
                }
            }
            // Meta: fecha + estrellas (si las hay)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatRelativeTime(post.createdAtMillis),
                    fontFamily = CenturyGotic,
                    fontSize = 11.sp,
                    color = SUB_TEXT
                )
                post.rating?.let { stars ->
                    Text(
                        text = " · ",
                        fontFamily = CenturyGotic,
                        fontSize = 11.sp,
                        color = SUB_TEXT
                    )
                    StarRow(stars)
                }
            }
        }

        // Tag de tipo: "reseñó" / "cita" / "terminó"
        ActionTag(post.type)
    }
}

@Composable
private fun AvatarWithRing(author: PostAuthor, size: androidx.compose.ui.unit.Dp) {
    val ringBrush = if (author.isFriend) Brush.linearGradient(FRIEND_RING_GRADIENT)
    else Brush.linearGradient(listOf(COMMUNITY_RING, COMMUNITY_RING))
    Box(
        modifier = Modifier
            .size(size + 4.dp)
            .clip(CircleShape)
            .background(ringBrush)
            .padding(2.dp)
            .clip(CircleShape)
            .border(2.dp, Color.White, CircleShape)
    ) {
        val avatarRes = AvatarHelper.getDrawableId(author.photoUrl)
        androidx.compose.foundation.Image(
            painter = painterResource(id = avatarRes),
            contentDescription = author.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun VerifiedBadge() {
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(SAVE_COLOR),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "✓",
            fontSize = 8.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StarRow(stars: Int) {
    Row {
        repeat(stars) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = ColorJournalStar,
                modifier = Modifier.size(11.dp)
            )
        }
    }
}

@Composable
private fun ActionTag(type: PostType) {
    val (bg, fg) = when (type) {
        PostType.REVIEW -> Color(0xFFFFE5EE) to Color(0xFFC73670)        // Rosa romance
        PostType.FINISHED -> Color(0xFFE3F0D8) to Color(0xFF4A8520)     // Verde terminado
        PostType.QUOTE -> ACTION_TAG_BG to SUB_TEXT                       // Beige neutro
        PostType.READING -> Color(0xFFE5EDFA) to Color(0xFF1E5BB8)      // Azul leyendo
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(
            text = postTypeTagLabel(type),
            fontFamily = CenturyGotic,
            fontSize = 10.sp,
            color = fg
        )
    }
}


/* ─── BOOK STRIP (información compacta del libro) ─── */

@Composable
private fun BookStrip(book: PostBook, rating: Int?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BOOK_STRIP_BG)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (book.coverUrl?.isNotBlank() == true) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(book.coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = book.title,
                modifier = Modifier
                    .size(width = 38.dp, height = 56.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(width = 38.dp, height = 56.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(fallbackCoverColor(book.id))
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book.title,
                fontFamily = CenturyGotic,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = ColorTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = book.author,
                fontFamily = CenturyGotic,
                fontSize = 10.sp,
                color = SUB_TEXT,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        rating?.let { stars ->
            Spacer(Modifier.width(8.dp))
            StarRow(stars)
        }
    }
}


/* ─── CONTENIDO TIPO CITA: serif italic + borde dorado lateral ─── */

@Composable
private fun QuoteContent(post: Post) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 0.dp))
            .background(BOOK_STRIP_BG)
            .border(
                width = 0.dp,
                color = Color.Transparent,
                shape = RoundedCornerShape(0.dp)
            )
            .drawLeftBorder(SAVE_COLOR, 3.dp)
            .padding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 12.dp)
    ) {
        Text(
            text = "\"${post.body}\"",
            fontFamily = GuardianCity,  // Serif para citas
            fontStyle = FontStyle.Italic,
            fontSize = 14.sp,
            color = ColorTextPrimary,
            lineHeight = 22.sp
        )
        post.quoteSource?.let { source ->
            Spacer(Modifier.height(6.dp))
            Text(
                text = "— $source",
                fontFamily = CenturyGotic,
                fontSize = 10.sp,
                color = SUB_TEXT,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Modifier auxiliar para dibujar SOLO un borde lateral izquierdo (los Modifier
 * border() estándar dibujan los 4 lados o ninguno). Lo hacemos con un Canvas
 * lineal que pinta una franja vertical en la izquierda.
 */
private fun Modifier.drawLeftBorder(color: Color, width: androidx.compose.ui.unit.Dp): Modifier =
    this.then(
        Modifier.drawWithContent {
            drawContent()
            val w = width.toPx()
            drawRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                size = androidx.compose.ui.geometry.Size(w, size.height)
            )
        }
    )


/* ─── ACCIONES: like / comment / save / overflow ─── */

@Composable
private fun PostActions(
    post: Post,
    onLikeClick: () -> Unit,
    onSaveClick: () -> Unit,
    onCommentClick: () -> Unit
) {
    // Línea separadora superior + fila de acciones
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(BOOK_STRIP_BG)
    )
    Spacer(Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // LIKE
        ActionButton(
            icon = if (post.isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            label = if (post.likeCount > 0) post.likeCount.toString() else "Me gusta",
            tint = if (post.isLikedByMe) LIKE_COLOR else SUB_TEXT,
            onClick = onLikeClick,
            isHighlighted = post.isLikedByMe
        )

        // COMMENT
        ActionButton(
            icon = Icons.Default.ChatBubbleOutline,
            label = if (post.commentCount > 0) post.commentCount.toString() else "Responder",
            tint = SUB_TEXT,
            onClick = onCommentClick
        )

        // SAVE
        ActionButton(
            icon = if (post.isSavedByMe) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
            label = if (post.isSavedByMe) "Guardada" else "Guardar",
            tint = if (post.isSavedByMe) SAVE_COLOR else SUB_TEXT,
            onClick = onSaveClick,
            isHighlighted = post.isSavedByMe
        )

        Spacer(Modifier.weight(1f))

        // MENU OVERFLOW
        Icon(
            imageVector = Icons.Default.MoreHoriz,
            contentDescription = null,
            tint = SUB_TEXT,
            modifier = Modifier
                .size(16.dp)
                .clickable { /* TODO: menú reportar/ocultar/etc */ }
        )
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    isHighlighted: Boolean = false
) {
    // Animación sutil de escala al pulsar el like
    val scale by animateFloatAsState(
        targetValue = if (isHighlighted) 1.05f else 1f,
        label = "actionScale"
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            fontFamily = CenturyGotic,
            fontSize = 12.sp,
            color = tint,
            fontWeight = if (isHighlighted) FontWeight.Medium else FontWeight.Normal
        )
    }
}