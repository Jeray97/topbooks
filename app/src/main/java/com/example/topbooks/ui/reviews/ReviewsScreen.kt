package com.example.topbooks.ui.reviews

import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.topbooks.R
import com.example.topbooks.data.model.Comment
import com.example.topbooks.ui.theme.*
import com.example.topbooks.utils.AvatarHelper
import com.example.topbooks.ui.community.CommunityFeedScreen

@Composable
fun ReviewsScreen(
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    onPostClick: (postId: String) -> Unit = {},
    onCreateStoryClick: () -> Unit = {},
    onStoryClick: (userId: String) -> Unit = {},
    onCreatePostClick: () -> Unit = {},
    viewModel: ReviewsViewModel = viewModel(),
    bookId: String? = null,
    targetCommentId: String? = null
) {
    if (bookId == null) {
        CommunityFeedScreen(
            onBackClick = onBackClick,
            onPostClick = { post ->
                onPostClick(post.id)
            },
            onCreatePostClick = onCreatePostClick,
            onCreateStoryClick = onCreateStoryClick,
            onStoryClick = onStoryClick
        )
        return
    }
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(bookId) {
        viewModel.loadSocialFeed(bookId, targetCommentId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.reviews_title_thread),
                    fontFamily = GuardianCity,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else {
                val allReviews = state.friendsReviews + state.communityReviews

                items(allReviews.distinctBy { it.commentId }) { comment ->
                    val isHighlighted = comment.commentId == targetCommentId

                    ReviewItem(
                        comment = comment,
                        onBookClick = { onBookClick(comment.bookId) },
                        onReply = { text -> viewModel.addReply(comment, text) },
                        onCheckVerification = { callback -> viewModel.checkEmailVerification(callback) },
                        isHighlighted = isHighlighted
                    )
                }
            }
        }
    }
}

@Composable
fun ReviewItem(
    comment: Comment,
    onBookClick: () -> Unit,
    onReply: (String) -> Unit,
    onCheckVerification: ((Boolean) -> Unit) -> Unit,
    isHighlighted: Boolean = false
) {
    var showReplyDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showReplyDialog) {
        ReplyDialog(onDismiss = { showReplyDialog = false }, onConfirm = onReply)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 8.dp else 2.dp),
        border = if (isHighlighted) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.secondaryContainer) else null,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (isHighlighted) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.reviews_badge_highlighted), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatarItem(comment.userPhotoUrl, size = 40.dp)
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(comment.userName, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                    val subtitleText = if (comment.chapter.isNotBlank()) {
                        stringResource(R.string.reviews_subtitle_with_chapter, comment.bookTitle, comment.chapter)
                    } else {
                        stringResource(R.string.reviews_subtitle_book_only, comment.bookTitle)
                    }

                    Text(
                        text = subtitleText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { onBookClick() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(comment.text, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, lineHeight = 24.sp)

            if (comment.replies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    comment.replies.forEach { reply ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
                            modifier = Modifier.padding(start = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    UserAvatarItem(reply.userPhotoUrl, size = 24.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(reply.userName, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(reply.text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        onCheckVerification { isVerified ->
                            if (isVerified) showReplyDialog = true
                            else Toast.makeText(context, context.getString(R.string.reviews_toast_verify_email), Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = stringResource(R.string.reviews_button_reply), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.reviews_button_reply), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun ReplyDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.reviews_dialog_title), fontFamily = GuardianCity, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(R.string.reviews_dialog_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { if(text.isNotBlank()) onConfirm(text); onDismiss() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.reviews_dialog_publish), color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.reviews_dialog_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun UserAvatarItem(photoUrl: String, size: androidx.compose.ui.unit.Dp = 70.dp) {
    val resId = AvatarHelper.getDrawableId(photoUrl)
    Image(
        painter = painterResource(id = resId),
        contentDescription = null,
        modifier = Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
        contentScale = ContentScale.Crop
    )
}
