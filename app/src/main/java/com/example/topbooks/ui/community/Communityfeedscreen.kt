package com.example.topbooks.ui.community

import android.app.Application
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
import com.example.topbooks.utils.AvatarHelper
import com.example.topbooks.ui.theme.GuardianCity
import com.example.topbooks.ui.theme.LoginColors

@Composable
fun CommunityFeedScreen(
    onBackClick: () -> Unit,
    onPostClick: (Post) -> Unit,
    onCreatePostClick: () -> Unit,
    onCreateStoryClick: () -> Unit = {},
    onStoryClick: (userId: String) -> Unit = {},
    viewModel: CommunityViewModel = viewModel(factory = CommunityViewModel.Factory(LocalContext.current.applicationContext as Application))
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = LoginColors.Background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreatePostClick,
                containerColor = LoginColors.Primary,
                contentColor = LoginColors.OnPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Compartir", fontWeight = FontWeight.SemiBold) },
                shape = RoundedCornerShape(12.dp)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item { HeaderSection(state.newPostsCountToday) }

            item {
                TabsRow(
                    activeTab = state.activeTab,
                    onTabClick = { tab -> viewModel.selectTab(tab) }
                )
            }

            item {
                StoryBar(
                    stories = state.stories,
                    onCreateStoryClick = onCreateStoryClick,
                    onStoryClick = onStoryClick
                )
            }

            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = LoginColors.Primary)
                    }
                }
            } else if (state.posts.isEmpty()) {
                item { EmptyFeedMessage(state.activeTab) }
            } else {
                items(state.posts, key = { it.id }) { post ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        PostCard(
                            post = post,
                            onLikeClick = { viewModel.toggleLike(post) },
                            onSaveClick = { viewModel.toggleSave(post) },
                            onCommentClick = { onPostClick(post) },
                            onCardClick = { onPostClick(post) }
                        )
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun HeaderSection(newPostsCount: Int) {
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
    ) {
        Text(
            text = "Comunidad",
            fontFamily = GuardianCity,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = LoginColors.Primary
        )
        if (newPostsCount > 0) {
            Text(
                text = if (newPostsCount == 1) "1 nueva reseña hoy"
                else "$newPostsCount nuevas reseñas hoy",
                fontSize = 12.sp,
                color = LoginColors.OnSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun TabsRow(activeTab: FeedTab, onTabClick: (FeedTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TabPill(
            label = "Comunidad",
            icon = Icons.Default.Public,
            isActive = activeTab == FeedTab.COMMUNITY,
            onClick = { onTabClick(FeedTab.COMMUNITY) }
        )
        TabPill(
            label = "Amigos",
            icon = Icons.Default.People,
            isActive = activeTab == FeedTab.FRIENDS,
            onClick = { onTabClick(FeedTab.FRIENDS) }
        )
        TabPill(
            label = "Top",
            icon = Icons.Default.Star,
            isActive = activeTab == FeedTab.TOP,
            onClick = { onTabClick(FeedTab.TOP) }
        )
    }
}

@Composable
private fun TabPill(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        if (isActive) LoginColors.Primary else LoginColors.SurfaceContainer,
        label = "tabBg"
    )
    val textColor by animateColorAsState(
        if (isActive) LoginColors.OnPrimary else LoginColors.OnSurface,
        label = "tabText"
    )

    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(9999.dp))
            .background(bgColor)
            .then(
                if (!isActive) Modifier.border(1.dp, LoginColors.OutlineVariant, RoundedCornerShape(9999.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

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
            .padding(horizontal = 16.dp, vertical = 16.dp),
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
            .width(72.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .border(2.dp, LoginColors.SurfaceTint, CircleShape)
                .background(LoginColors.SurfaceContainerLow),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = LoginColors.SurfaceTint,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Tu lectura",
            fontSize = 12.sp,
            color = LoginColors.OnSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StoryAvatar(story: StoryItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .border(2.dp, LoginColors.Surface, CircleShape)
                .clip(CircleShape)
        ) {
            val avatarRes = AvatarHelper.getDrawableId(story.author.photoUrl)
            androidx.compose.foundation.Image(
                painter = painterResource(id = avatarRes),
                contentDescription = story.author.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = story.author.displayName.split(" ").first(),
            fontSize = 14.sp,
            color = LoginColors.OnSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        story.currentBook?.let { book ->
            Text(
                text = if (story.hasFinished) "✓ Terminó"
                else "📖 ${book.title.take(8)}",
                fontSize = 12.sp,
                color = LoginColors.Outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

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
            fontSize = 16.sp,
            color = LoginColors.OnSurfaceVariant,
            fontStyle = FontStyle.Italic
        )
    }
}

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
            .clip(RoundedCornerShape(12.dp))
            .background(LoginColors.Surface)
            .border(1.dp, LoginColors.OutlineVariant, RoundedCornerShape(12.dp))
            .clickable(onClick = onCardClick)
            .padding(16.dp)
    ) {
        PostHeader(post)

        Spacer(Modifier.height(16.dp))

        when (post.type) {
            PostType.QUOTE -> QuoteContent(post)
            else -> {
                post.book?.let { BookStrip(it, post.rating) }
                if (post.body.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = post.body,
                        fontSize = 16.sp,
                        color = LoginColors.OnSurface,
                        lineHeight = 24.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        PostActions(
            post = post,
            onLikeClick = onLikeClick,
            onSaveClick = onSaveClick,
            onCommentClick = onCommentClick
        )
    }
}

@Composable
private fun PostHeader(post: Post) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AvatarWithRing(author = post.author, size = 40.dp)

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = post.author.displayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LoginColors.OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (post.author.isVerified) {
                    Spacer(Modifier.width(6.dp))
                    VerifiedBadge()
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatRelativeTime(post.createdAtMillis),
                    fontSize = 12.sp,
                    color = LoginColors.OnSurfaceVariant
                )
                post.rating?.let { stars ->
                    Text(
                        text = " · ",
                        fontSize = 12.sp,
                        color = LoginColors.OnSurfaceVariant
                    )
                    StarRow(stars)
                }
            }
        }

        ActionTag(post.type)
    }
}

@Composable
private fun AvatarWithRing(author: PostAuthor, size: androidx.compose.ui.unit.Dp) {
    val ringColor = if (author.isFriend) LoginColors.SecondaryContainer else LoginColors.OutlineVariant
    Box(
        modifier = Modifier
            .size(size + 4.dp)
            .clip(CircleShape)
            .border(2.dp, ringColor, CircleShape)
            .padding(2.dp)
            .clip(CircleShape)
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
            .size(14.dp)
            .clip(CircleShape)
            .background(LoginColors.SurfaceTint),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "✓",
            fontSize = 9.sp,
            color = LoginColors.OnPrimary,
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
                tint = LoginColors.SecondaryContainer,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun ActionTag(type: PostType) {
    val (bg, fg) = when (type) {
        PostType.REVIEW -> Color(0xFFFFDAD8) to LoginColors.PrimaryContainer
        PostType.FINISHED -> Color(0xFFE3F0D8) to Color(0xFF4A8520)
        PostType.QUOTE -> LoginColors.SurfaceContainer to LoginColors.OnSurfaceVariant
        PostType.READING -> Color(0xFFE5EDFA) to Color(0xFF1E5BB8)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9999.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = postTypeTagLabel(type),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = fg
        )
    }
}

@Composable
private fun BookStrip(book: PostBook, rating: Int?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(LoginColors.SurfaceContainer)
            .border(1.dp, LoginColors.OutlineVariant, RoundedCornerShape(8.dp))
            .drawLeftBorder(LoginColors.Primary, 4.dp)
            .padding(12.dp),
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
                    .size(width = 48.dp, height = 64.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 64.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(fallbackCoverColor(book.id))
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = LoginColors.OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = book.author,
                fontSize = 12.sp,
                color = LoginColors.OnSurfaceVariant,
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

@Composable
private fun QuoteContent(post: Post) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(LoginColors.SurfaceContainer)
            .drawLeftBorder(LoginColors.Primary, 4.dp)
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Text(
            text = "\"${post.body}\"",
            fontFamily = GuardianCity,
            fontStyle = FontStyle.Italic,
            fontSize = 16.sp,
            color = LoginColors.OnSurface,
            lineHeight = 24.sp
        )
        post.quoteSource?.let { source ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = "— $source",
                fontSize = 12.sp,
                color = LoginColors.OnSurfaceVariant,
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

@Composable
private fun PostActions(
    post: Post,
    onLikeClick: () -> Unit,
    onSaveClick: () -> Unit,
    onCommentClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(LoginColors.OutlineVariant.copy(alpha = 0.3f))
    )
    Spacer(Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        ActionButton(
            icon = if (post.isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            label = if (post.likeCount > 0) post.likeCount.toString() else "Me gusta",
            tint = if (post.isLikedByMe) LoginColors.SurfaceTint else LoginColors.OnSurfaceVariant,
            onClick = onLikeClick,
            isHighlighted = post.isLikedByMe
        )

        ActionButton(
            icon = Icons.Default.ChatBubbleOutline,
            label = if (post.commentCount > 0) post.commentCount.toString() else "Responder",
            tint = LoginColors.OnSurfaceVariant,
            onClick = onCommentClick
        )

        ActionButton(
            icon = if (post.isSavedByMe) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
            label = if (post.isSavedByMe) "Guardada" else "Guardar",
            tint = if (post.isSavedByMe) LoginColors.SurfaceTint else LoginColors.OnSurfaceVariant,
            onClick = onSaveClick,
            isHighlighted = post.isSavedByMe
        )

        Spacer(Modifier.weight(1f))

        Icon(
            imageVector = Icons.Default.MoreHoriz,
            contentDescription = null,
            tint = LoginColors.OnSurfaceVariant,
            modifier = Modifier
                .size(20.dp)
                .clickable { }
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
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = tint,
            fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
