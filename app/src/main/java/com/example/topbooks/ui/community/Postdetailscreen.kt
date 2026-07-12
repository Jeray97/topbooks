package com.example.topbooks.ui.community

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.example.topbooks.ui.theme.GuardianCity

/* =============================================================================
 *  PANTALLA: DETALLE DE POST (Mockup 2)
 * =============================================================================
 *  Muestra:
 *    - Card grande del post con todo su contenido (book-strip, texto, etc.)
 *    - Reacciones agregadas: 3 top fijos (❤️📚🥲) + cualquier otro emoji con uso
 *    - Botón "+" que abre selector de emojis populares
 *    - Stats bar resumen: "12 reacciones · 3 respuestas · 5 guardados"
 *    - Acciones primarias: Me gusta / Responder / Guardar / Compartir
 *    - Hilo plano de respuestas con badge "autora" dorado cuando aplica
 *    - Compose bar fija abajo para responder
 *
 *  Esta pantalla **sustituye** a SingleCommentScreen. La firma es la misma
 *  (commentId, onBackClick) para no tocar la navegación: cuando se navega a
 *  "single_comment/{commentId}", el commentId se interpreta como postId.
 * ============================================================================= */

private val SUB_TEXT = Color(0xFF8D5B4C)
private val LIKE_COLOR = Color(0xFFFF4081)
private val SAVE_COLOR = Color(0xFFB9836B)
@Composable
private fun CARD_BG() = MaterialTheme.colorScheme.surface
private val CARD_BORDER = Color(0xFFECDDD2)
private val ACCENT_BEIGE = Color(0xFFF6E6DD)
private val FRIEND_RING_GRADIENT = listOf(Color(0xFFB9836B), Color(0xFF8D5B4C))
private val COMMUNITY_RING = Color(0xFFC89B8C)
private val AUTHOR_REPLY_BG = Color(0xFFF8EFE2)        // Tinte cálido para el reply de la autora
private val AUTHOR_REPLY_BORDER = Color(0xFFE6CDA8)    // Borde dorado claro

@Composable
fun PostDetailScreen(
    postId: String,
    onBackClick: () -> Unit,
    onAuthorClick: (String) -> Unit = {},
    onBookClick: (String) -> Unit = {},
    onShareClick: (Post) -> Unit = {},
    viewModel: PostDetailViewModel = viewModel(factory = PostDetailViewModel.Factory(LocalContext.current.applicationContext as Application))
) {
    val state by viewModel.uiState.collectAsState()
    var replyText by remember { mutableStateOf("") }

    // Carga inicial
    LaunchedEffect(postId) {
        viewModel.loadPost(postId)
    }

    Scaffold(
        containerColor = ColorBackGroundGeneral(),
        topBar = { TopBar(onBackClick = onBackClick) },
        bottomBar = {
            if (state.post != null) {
                ReplyComposeBar(
                    text = replyText,
                    onTextChange = { replyText = it },
                    isSending = state.isSendingReply,
                    onSendClick = {
                        if (replyText.isNotBlank()) {
                            viewModel.sendReply(replyText) {
                                replyText = ""
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.fillMaxSize().padding(padding))
            state.post == null -> NotFoundState(Modifier.fillMaxSize().padding(padding))
            else -> PostDetailContent(
                state = state,
                padding = padding,
                onLikeClick = { viewModel.toggleLike() },
                onSaveClick = { viewModel.toggleSave() },
                onReactionClick = { emoji -> viewModel.toggleReaction(emoji) },
                onAddReactionClick = { viewModel.toggleEmojiPicker() },
                onPickEmoji = { emoji -> viewModel.toggleReaction(emoji) },
                onReplyLikeClick = { replyId -> viewModel.toggleReplyLike(replyId) },
                onAuthorClick = onAuthorClick,
                onBookClick = onBookClick,
                onShareClick = { state.post?.let { onShareClick(it) } }
            )
        }
    }
}


/* ─────────────────────────────────────────────────────────────────────────────
 *  ESTADOS DE CARGA / ERROR
 * ───────────────────────────────────────────────────────────────────────────── */

@Composable
private fun LoadingState(modifier: Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = ColorArcMediumBrown())
    }
}

@Composable
private fun NotFoundState(modifier: Modifier) {
    Box(modifier = modifier.padding(40.dp), contentAlignment = Alignment.Center) {
        Text(
            text = "No hemos encontrado este post.",
            fontFamily = CenturyGotic,
            fontSize = 14.sp,
            color = SUB_TEXT,
            fontStyle = FontStyle.Italic
        )
    }
}


/* ─────────────────────────────────────────────────────────────────────────────
 *  CONTENIDO PRINCIPAL (cuando ya cargó el post)
 * ───────────────────────────────────────────────────────────────────────────── */

@Composable
private fun PostDetailContent(
    state: PostDetailUiState,
    padding: PaddingValues,
    onLikeClick: () -> Unit,
    onSaveClick: () -> Unit,
    onReactionClick: (String) -> Unit,
    onAddReactionClick: () -> Unit,
    onPickEmoji: (String) -> Unit,
    onReplyLikeClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit,
    onBookClick: (String) -> Unit,
    onShareClick: () -> Unit
) {
    val post = state.post ?: return

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
        // Card principal del post
        item {
            MainPostCard(
                post = post,
                reactions = state.reactions,
                totalReactionCount = state.totalReactionCount,
                replyCount = state.replies.size,
                savedCount = state.savedCount,
                emojiPickerOpen = state.emojiPickerOpen,
                onLikeClick = onLikeClick,
                onSaveClick = onSaveClick,
                onReactionClick = onReactionClick,
                onAddReactionClick = onAddReactionClick,
                onPickEmoji = onPickEmoji,
                onAuthorClick = { onAuthorClick(post.author.id) },
                onBookClick = { post.book?.let { onBookClick(it.id) } },
                onShareClick = onShareClick
            )
        }

        item { Spacer(Modifier.height(12.dp)) }

        // Subtítulo del hilo
        if (state.replies.isNotEmpty()) {
            item {
                Text(
                    text = "↳ Hilo de conversación",
                    fontFamily = CenturyGotic,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = SUB_TEXT,
                    modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                )
            }
        }

        // Lista de respuestas (hilo plano)
        items(state.replies, key = { it.id }) { reply ->
            ReplyCard(
                reply = reply,
                onLikeClick = { onReplyLikeClick(reply.id) },
                onAuthorClick = { onAuthorClick(reply.author.id) }
            )
            Spacer(Modifier.height(8.dp))
        }

        // Espacio inferior para que la última respuesta no quede pegada al compose bar
        item { Spacer(Modifier.height(12.dp)) }
    }
}


/* ─────────────────────────────────────────────────────────────────────────────
 *  CARD PRINCIPAL DEL POST
 * ───────────────────────────────────────────────────────────────────────────── */

@Composable
private fun MainPostCard(
    post: Post,
    reactions: List<Reaction>,
    totalReactionCount: Int,
    replyCount: Int,
    savedCount: Int,
    emojiPickerOpen: Boolean,
    onLikeClick: () -> Unit,
    onSaveClick: () -> Unit,
    onReactionClick: (String) -> Unit,
    onAddReactionClick: () -> Unit,
    onPickEmoji: (String) -> Unit,
    onAuthorClick: () -> Unit,
    onBookClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CARD_BG())
            .border(1.dp, CARD_BORDER, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        // Header: avatar + nombre + meta + tag
        PostDetailHeader(post = post, onAuthorClick = onAuthorClick)

        Spacer(Modifier.height(10.dp))

        // Contenido según tipo
        when (post.type) {
            PostType.QUOTE -> QuoteBlock(post)
            else -> {
                post.book?.let {
                    BookStripDetail(book = it, rating = post.rating, onClick = onBookClick)
                }
                if (post.body.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = post.body,
                        fontFamily = CenturyGotic,
                        fontSize = 14.sp,
                        color = ColorTextPrimary(),
                        lineHeight = 22.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Reacciones múltiples (pills)
        ReactionsRow(
            reactions = reactions,
            onReactionClick = onReactionClick,
            onAddClick = onAddReactionClick
        )

        // Selector de emojis adicionales (animado)
        AnimatedVisibility(
            visible = emojiPickerOpen,
            enter = fadeIn() + scaleIn(initialScale = 0.92f),
            exit = fadeOut() + scaleOut(targetScale = 0.92f)
        ) {
            EmojiPicker(
                emojis = POPULAR_REACTIONS,
                onPick = onPickEmoji
            )
        }

        Spacer(Modifier.height(10.dp))

        // Stats bar
        StatsBar(
            totalReactions = totalReactionCount,
            replies = replyCount,
            saved = savedCount
        )

        Spacer(Modifier.height(8.dp))

        // Acciones primarias: Me gusta / Responder / Guardada / Compartir
        PrimaryActions(
            isLiked = post.isLikedByMe,
            isSaved = post.isSavedByMe,
            onLikeClick = onLikeClick,
            onSaveClick = onSaveClick,
            onShareClick = onShareClick
        )
    }
}


/* ─── HEADER del post ─── */

@Composable
private fun PostDetailHeader(post: Post, onAuthorClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(onClick = onAuthorClick)
    ) {
        AvatarCircle(author = post.author, size = 40.dp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = post.author.displayName,
                    fontFamily = CenturyGotic,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorTextPrimary(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (post.author.isVerified) {
                    Spacer(Modifier.width(5.dp))
                    VerifiedBadgeDot()
                }
            }
            Text(
                text = "${formatRelativeTime(post.createdAtMillis)} · pública",
                fontFamily = CenturyGotic,
                fontSize = 11.sp,
                color = SUB_TEXT
            )
        }
        ActionTagPill(post.type)
    }
}

@Composable
private fun AvatarCircle(author: PostAuthor, size: androidx.compose.ui.unit.Dp) {
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
        Image(
            painter = painterResource(id = avatarRes),
            contentDescription = author.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun VerifiedBadgeDot() {
    Box(
        modifier = Modifier.size(12.dp).clip(CircleShape).background(SAVE_COLOR),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "✓", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActionTagPill(type: PostType) {
    val (bg, fg) = when (type) {
        PostType.REVIEW -> Color(0xFFFFE5EE) to Color(0xFFC73670)
        PostType.FINISHED -> Color(0xFFE3F0D8) to Color(0xFF4A8520)
        PostType.QUOTE -> ACCENT_BEIGE to SUB_TEXT
        PostType.READING -> Color(0xFFE5EDFA) to Color(0xFF1E5BB8)
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


/* ─── BOOK STRIP versión grande con click ─── */

@Composable
private fun BookStripDetail(book: PostBook, rating: Int?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ACCENT_BEIGE)
            .clickable(onClick = onClick)
            .padding(10.dp),
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
                    .size(width = 50.dp, height = 75.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(width = 50.dp, height = 75.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(fallbackCoverColor(book.id))
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book.title,
                fontFamily = CenturyGotic,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = ColorTextPrimary(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = book.author,
                fontFamily = CenturyGotic,
                fontSize = 11.sp,
                color = SUB_TEXT,
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            rating?.let {
                Spacer(Modifier.height(4.dp))
                Row {
                    repeat(it) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = ColorJournalStar(),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}


/* ─── BLOQUE DE CITA (mismo estilo que en feed pero más grande) ─── */

@Composable
private fun QuoteBlock(post: Post) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 0.dp))
            .background(ACCENT_BEIGE)
            .drawLeftBorder(SAVE_COLOR, 3.dp)
            .padding(start = 16.dp, top = 14.dp, end = 14.dp, bottom = 14.dp)
    ) {
        Text(
            text = "\"${post.body}\"",
            fontFamily = GuardianCity,
            fontStyle = FontStyle.Italic,
            fontSize = 16.sp,
            color = ColorTextPrimary(),
            lineHeight = 24.sp
        )
        post.quoteSource?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "— $it",
                fontFamily = CenturyGotic,
                fontSize = 11.sp,
                color = SUB_TEXT,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

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


/* ─── REACCIONES (pills + selector) ─── */

@Composable
private fun ReactionsRow(
    reactions: List<Reaction>,
    onReactionClick: (String) -> Unit,
    onAddClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        reactions.forEach { reaction ->
            ReactionPill(reaction = reaction, onClick = { onReactionClick(reaction.emoji) })
        }
        // Botón "+ reaccionar" para abrir el selector
        AddReactionPill(onClick = onAddClick)
    }
}

@Composable
private fun ReactionPill(reaction: Reaction, onClick: () -> Unit) {
    val bg = if (reaction.reactedByMe) Color(0xFFFFE5EE) else ACCENT_BEIGE
    val border = if (reaction.reactedByMe) Color(0xFFFFC2D6) else CARD_BORDER
    val fg = if (reaction.reactedByMe) Color(0xFFC73670) else ColorTextPrimary()

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = reaction.emoji, fontSize = 13.sp)
        if (reaction.count > 0) {
            Spacer(Modifier.width(4.dp))
            Text(
                text = reaction.count.toString(),
                fontFamily = CenturyGotic,
                fontSize = 11.sp,
                color = fg,
                fontWeight = if (reaction.reactedByMe) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun AddReactionPill(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, CARD_BORDER, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = SAVE_COLOR,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = "reaccionar",
            fontFamily = CenturyGotic,
            fontSize = 11.sp,
            color = SAVE_COLOR
        )
    }
}

@Composable
private fun EmojiPicker(emojis: List<String>, onPick: (String) -> Unit) {
    Column(modifier = Modifier.padding(top = 10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ACCENT_BEIGE)
                .padding(10.dp)
        ) {
            // Distribuimos los emojis en filas de 5
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                emojis.chunked(5).forEach { rowEmojis ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        rowEmojis.forEach { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable { onPick(emoji) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 18.sp)
                            }
                        }
                        // Si la fila no llega a 5, rellenamos con espacios para alinear
                        repeat(5 - rowEmojis.size) {
                            Spacer(Modifier.size(36.dp))
                        }
                    }
                }
            }
        }
    }
}


/* ─── STATS BAR ─── */

@Composable
private fun StatsBar(totalReactions: Int, replies: Int, saved: Int) {
    val pieces = mutableListOf<String>()
    if (totalReactions > 0) pieces += "$totalReactions ${if (totalReactions == 1) "reacción" else "reacciones"}"
    if (replies > 0) pieces += "$replies ${if (replies == 1) "respuesta" else "respuestas"}"
    if (saved > 0) pieces += "$saved guardados"

    if (pieces.isEmpty()) {
        // Si no hay nada que mostrar, espacio mínimo
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ACCENT_BEIGE))
        return
    }

    Column {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ACCENT_BEIGE))
        Text(
            text = pieces.joinToString(" · "),
            fontFamily = CenturyGotic,
            fontSize = 11.sp,
            color = SUB_TEXT,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ACCENT_BEIGE))
    }
}


/* ─── ACCIONES PRIMARIAS ─── */

@Composable
private fun PrimaryActions(
    isLiked: Boolean,
    isSaved: Boolean,
    onLikeClick: () -> Unit,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionItem(
            icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            label = "Me gusta",
            color = if (isLiked) LIKE_COLOR else SUB_TEXT,
            isHighlighted = isLiked,
            onClick = onLikeClick
        )
        ActionItem(
            icon = Icons.Default.ChatBubbleOutline,
            label = "Responder",
            color = SUB_TEXT,
            onClick = { /* compose bar abajo ya está disponible */ }
        )
        ActionItem(
            icon = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
            label = if (isSaved) "Guardada" else "Guardar",
            color = if (isSaved) SAVE_COLOR else SUB_TEXT,
            isHighlighted = isSaved,
            onClick = onSaveClick
        )
        ActionItem(
            icon = Icons.Default.Share,
            label = "Compartir",
            color = SUB_TEXT,
            onClick = onShareClick
        )
    }
}

@Composable
private fun ActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    isHighlighted: Boolean = false,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isHighlighted) 1.05f else 1f,
        label = "actionScale"
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            fontFamily = CenturyGotic,
            fontSize = 12.sp,
            color = color,
            fontWeight = if (isHighlighted) FontWeight.Medium else FontWeight.Normal
        )
    }
}


/* ─────────────────────────────────────────────────────────────────────────────
 *  CARD DE UNA RESPUESTA (REPLY)
 * ───────────────────────────────────────────────────────────────────────────── */

@Composable
private fun ReplyCard(
    reply: PostReply,
    onLikeClick: () -> Unit,
    onAuthorClick: () -> Unit
) {
    val bg = if (reply.isFromOriginalAuthor) AUTHOR_REPLY_BG else CARD_BG()
    val border = if (reply.isFromOriginalAuthor) AUTHOR_REPLY_BORDER else CARD_BORDER

    // Sangría a la izquierda + línea conectora horizontal sutil al inicio
    Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(bg)
                .border(1.dp, border, RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            // Header: avatar + nombre + badge "autora" + tiempo
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(fallbackCoverColor(reply.author.id))
                        .clickable(onClick = onAuthorClick)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = reply.author.displayName,
                    fontFamily = CenturyGotic,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorTextPrimary(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (reply.isFromOriginalAuthor) {
                    Spacer(Modifier.width(6.dp))
                    AuthorBadge()
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatRelativeTime(reply.createdAtMillis),
                    fontFamily = CenturyGotic,
                    fontSize = 10.sp,
                    color = SUB_TEXT
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = reply.body,
                fontFamily = CenturyGotic,
                fontSize = 12.sp,
                color = ColorTextPrimary(),
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(8.dp))
            // Acciones de la respuesta: like + responder
            Row(verticalAlignment = Alignment.CenterVertically) {
                ReplyActionButton(
                    icon = if (reply.isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    text = if (reply.likeCount > 0) reply.likeCount.toString() else "Me gusta",
                    color = if (reply.isLikedByMe) LIKE_COLOR else SUB_TEXT,
                    onClick = onLikeClick
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = "Responder",
                    fontFamily = CenturyGotic,
                    fontSize = 10.sp,
                    color = SUB_TEXT,
                    modifier = Modifier.clickable { /* TODO: respuesta a respuesta */ }
                )
            }
        }
    }
}

@Composable
private fun AuthorBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SAVE_COLOR)
            .padding(horizontal = 6.dp, vertical = 1.dp)
    ) {
        Text(
            text = "autora",
            fontFamily = CenturyGotic,
            fontSize = 9.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ReplyActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(13.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            fontFamily = CenturyGotic,
            fontSize = 10.sp,
            color = color
        )
    }
}


/* ─────────────────────────────────────────────────────────────────────────────
 *  COMPOSE BAR (responder en el hilo)
 * ───────────────────────────────────────────────────────────────────────────── */

@Composable
private fun ReplyComposeBar(
    text: String,
    onTextChange: (String) -> Unit,
    isSending: Boolean,
    onSendClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Avatar pequeño del usuario actual
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(COMMUNITY_RING, ColorArcDarkBrown())))
        )

        // Input expandible
        TextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(ACCENT_BEIGE),
            placeholder = {
                Text(
                    text = "Escribe una respuesta…",
                    fontFamily = CenturyGotic,
                    fontSize = 12.sp,
                    color = SUB_TEXT,
                    fontStyle = FontStyle.Italic
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = ACCENT_BEIGE,
                unfocusedContainerColor = ACCENT_BEIGE,
                disabledContainerColor = ACCENT_BEIGE,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = ColorArcDarkBrown(),
                focusedTextColor = ColorTextPrimary(),
                unfocusedTextColor = ColorTextPrimary()
            )
        )

        // Botón de envío
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (text.isNotBlank() && !isSending) ColorArcDarkBrown() else COMMUNITY_RING)
                .clickable(enabled = text.isNotBlank() && !isSending, onClick = onSendClick),
            contentAlignment = Alignment.Center
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}