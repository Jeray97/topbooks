package com.example.topbooks.ui.reviews

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.topbooks.data.model.Comment
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.ui.theme.*
import com.example.topbooks.utils.AvatarHelper

@Composable
fun ReviewsScreen(
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    viewModel: ReviewsViewModel = viewModel(),
    bookId: String? = null,
    targetCommentId: String? = null
) {
    val state by viewModel.uiState.collectAsState()

    // Si recibimos un bookId, cargamos ese libro específico nada más
    LaunchedEffect(bookId) {
        viewModel.loadSocialFeed(bookId, targetCommentId)
    }

    Scaffold(
        containerColor = ColorBackGroundGeneral,
        topBar = { TopBar(onBackClick = onBackClick) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = if (bookId != null) "Hilo de conversación" else "Actividad de la Comunidad",
                    fontFamily = CenturyGotic,
                    fontSize = 24.sp,
                    color = ColorArcMediumBrown,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (state.isLoading) {
                item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ColorArcMediumBrown) } }
            } else {
                // Mostramos las reseñas/comentarios mezclados
                val allReviews = state.friendsReviews + state.communityReviews

                items(allReviews.distinctBy { it.commentId }) { comment ->
                    // Si este comentario es el objetivo del Deep Link, podemos darle un borde o color diferente
                    val isHighlighted = comment.commentId == targetCommentId

                    ReviewItem(
                        comment = comment,
                        onBookClick = { onBookClick(comment.bookId) },
                        onReply = { text -> viewModel.addReply(comment, text) },
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
    isHighlighted: Boolean = false
) {
    var showReplyDialog by remember { mutableStateOf(false) }

    if (showReplyDialog) {
        ReplyDialog(onDismiss = { showReplyDialog = false }, onConfirm = onReply)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        // Si está resaltado, le ponemos un borde sutil
        border = if (isHighlighted) androidx.compose.foundation.BorderStroke(2.dp, ColorArcMediumBrown) else null,
        colors = CardDefaults.cardColors(containerColor = ColorArcMediumBrown)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatarItem(comment.userPhotoUrl, size = 45.dp)
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(comment.userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("en ${comment.bookTitle}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, modifier = Modifier.clickable { onBookClick() })
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(comment.text, color = Color.White, fontSize = 14.sp)

            // Respuestas del hilo
            if (comment.replies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                comment.replies.forEach { reply ->
                    Row(modifier = Modifier.padding(start = 24.dp, top = 8.dp), verticalAlignment = Alignment.Top) {
                        UserAvatarItem(reply.userPhotoUrl, size = 30.dp)
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text("${reply.userName} respondió:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(reply.text, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }

            Text(
                text = "Responder",
                color = Color(0xFFFFE082),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.End).clickable { showReplyDialog = true }.padding(top = 12.dp)
            )
        }
    }
}

@Composable
fun ReplyDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir respuesta") },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, placeholder = { Text("Escribe algo...") }, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { Button(onClick = { if(text.isNotEmpty()) onConfirm(text); onDismiss() }) { Text("Enviar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun UserAvatarItem(photoUrl: String, size: androidx.compose.ui.unit.Dp = 70.dp) {
    val resId = AvatarHelper.getDrawableId(photoUrl)
    Image(
        painter = painterResource(id = resId),
        contentDescription = null,
        modifier = Modifier.size(size).clip(CircleShape).background(Color.White).padding(2.dp).clip(CircleShape),
        contentScale = ContentScale.Crop
    )
}