package com.example.topbooks.ui.community

import android.app.Application
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.topbooks.data.model.Story
import com.example.topbooks.data.model.StoryType
import com.example.topbooks.ui.theme.ColorArcDarkBrown
import com.example.topbooks.ui.theme.ColorArcMediumBrown
import com.example.topbooks.ui.theme.ColorBackGroundGeneral
import com.example.topbooks.ui.theme.GuardianCity
import com.example.topbooks.ui.theme.CenturyGotic
import com.example.topbooks.utils.AvatarHelper
import kotlinx.coroutines.delay

private const val STORY_DURATION_MS = 5000L

@Composable
fun StoryViewerScreen(
    userId: String,
    isOwnProfile: Boolean = false,
    onClose: () -> Unit,
    viewModel: StoryViewModel = viewModel(factory = StoryViewModel.Factory(LocalContext.current.applicationContext as Application))
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(userId) {
        if (isOwnProfile) {
            viewModel.loadMyStories()
        } else {
            viewModel.loadStories(userId)
        }
    }

    if (state.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    if (state.stories.isEmpty()) {
        onClose()
        return
    }

    val currentStory = state.stories.getOrNull(state.currentStoryIndex)
    if (currentStory == null) {
        onClose()
        return
    }

    LaunchedEffect(currentStory.id) {
        viewModel.markAsViewed(currentStory.id)
    }

    var progressKey by remember { mutableStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }

    LaunchedEffect(state.currentStoryIndex) {
        progressKey++
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(parseStoryColor(currentStory.backgroundColor))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            StoryProgressBars(
                totalStories = state.stories.size,
                currentIndex = state.currentStoryIndex,
                isPaused = isPaused,
                progressKey = progressKey,
                onProgressFinished = {
                    if (viewModel.isLastStory()) {
                        onClose()
                    } else {
                        viewModel.nextStory()
                    }
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val avatarRes = AvatarHelper.getDrawableId(currentStory.userPhotoUrl)
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = avatarRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentStory.userName,
                        fontFamily = CenturyGotic,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Text(
                        text = formatStoryTime(currentStory),
                        fontFamily = CenturyGotic,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                if (isOwnProfile) {
                    IconButton(onClick = {
                        viewModel.deleteStory(currentStory.id) {
                            if (state.stories.size <= 1) onClose()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = Color.White
                        )
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(state.currentStoryIndex) {
                        detectTapGestures(
                            onTap = { offset ->
                                val screenWidth = size.width
                                if (offset.x < screenWidth / 3) {
                                    viewModel.previousStory()
                                } else {
                                    if (viewModel.isLastStory()) onClose()
                                    else viewModel.nextStory()
                                }
                            },
                            onPress = {
                                isPaused = true
                                tryAwaitRelease()
                                isPaused = false
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                StoryContent(story = currentStory)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StoryProgressBars(
    totalStories: Int,
    currentIndex: Int,
    isPaused: Boolean,
    progressKey: Int,
    onProgressFinished: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 0 until totalStories) {
            val progress = when {
                i < currentIndex -> 1f
                i == currentIndex -> {
                    var targetProgress by remember(progressKey) { mutableStateOf(0f) }
                    val animatedProgress by animateFloatAsState(
                        targetValue = if (isPaused) targetProgress else 1f,
                        animationSpec = tween(
                            durationMillis = STORY_DURATION_MS.toInt(),
                            easing = LinearEasing
                        ),
                        finishedListener = { if (i == currentIndex) onProgressFinished() }
                    )
                    LaunchedEffect(progressKey, isPaused) {
                        if (!isPaused) targetProgress = 1f
                    }
                    animatedProgress
                }
                else -> 0f
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White)
                )
            }
        }
    }
}

@Composable
private fun StoryContent(story: Story) {
    val storyType = try {
        StoryType.valueOf(story.type)
    } catch (e: Exception) {
        StoryType.BOOK_COVER
    }

    when (storyType) {
        StoryType.BOOK_COVER -> BookCoverStory(story)
        StoryType.QUOTE -> QuoteStory(story)
        StoryType.READING_STATUS -> ReadingStatusStory(story)
    }
}

@Composable
private fun BookCoverStory(story: Story) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (story.bookImageUrl.isNotBlank()) {
            Image(
                painter = rememberAsyncImagePainter(model = story.bookImageUrl),
                contentDescription = story.bookTitle,
                modifier = Modifier
                    .size(width = 200.dp, height = 300.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(width = 200.dp, height = 300.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ColorArcMediumBrown()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = story.bookTitle.take(2),
                    fontFamily = GuardianCity,
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp,
                    color = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = story.bookTitle,
            fontFamily = GuardianCity,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        if (story.bookAuthor.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = story.bookAuthor,
                fontFamily = CenturyGotic,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
        if (story.text.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = story.text,
                fontFamily = CenturyGotic,
                fontSize = 14.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun QuoteStory(story: Story) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "\"",
            fontFamily = GuardianCity,
            fontWeight = FontWeight.Bold,
            fontSize = 72.sp,
            color = Color.White.copy(alpha = 0.4f)
        )
        Text(
            text = story.text,
            fontFamily = GuardianCity,
            fontStyle = FontStyle.Italic,
            fontSize = 22.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (story.bookTitle.isNotBlank()) {
            Text(
                text = "— ${story.bookTitle}",
                fontFamily = CenturyGotic,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
        if (story.bookAuthor.isNotBlank()) {
            Text(
                text = story.bookAuthor,
                fontFamily = CenturyGotic,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ReadingStatusStory(story: Story) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📖",
            fontSize = 48.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Estoy leyendo",
            fontFamily = CenturyGotic,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (story.bookImageUrl.isNotBlank()) {
            Image(
                painter = rememberAsyncImagePainter(model = story.bookImageUrl),
                contentDescription = story.bookTitle,
                modifier = Modifier
                    .size(width = 140.dp, height = 210.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = story.bookTitle,
            fontFamily = GuardianCity,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        if (story.bookAuthor.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = story.bookAuthor,
                fontFamily = CenturyGotic,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
        if (story.text.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = story.text,
                fontFamily = CenturyGotic,
                fontSize = 13.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun parseStoryColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF8D5B4C)
    }
}

private fun formatStoryTime(story: Story): String {
    val createdAt = story.createdAt?.time ?: return ""
    val diff = System.currentTimeMillis() - createdAt
    val hours = diff / (1000 * 60 * 60)
    return when {
        hours < 1 -> "Hace menos de 1h"
        hours < 24 -> "Hace ${hours}h"
        else -> "Hace ${hours / 24}d"
    }
}
