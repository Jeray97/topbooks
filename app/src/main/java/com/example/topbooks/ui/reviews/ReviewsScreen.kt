package com.example.topbooks.ui.reviews

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.topbooks.R
import com.example.topbooks.data.model.Comment
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.ui.theme.*
import com.example.topbooks.utils.AvatarHelper

@Composable
fun ReviewsScreen(
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    viewModel: ReviewsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.reviews_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = { TopBar(onBackClick = onBackClick) }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).padding(horizontal = 16.dp)) {
                Text(
                    text = "Interacciones por capítulos",
                    fontFamily = CenturyGotic,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTituloTopBooks,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(state.friendsReviews) { comment ->
                        ChatStyleReviewCard(comment, onBookClick, onReply = { id, txt -> viewModel.postReply(id, txt) })
                    }
                    if (state.communityReviews.isNotEmpty()) {
                        item { Text("Comunidad", color = ColorArcDarkBrown, fontWeight = FontWeight.Bold) }
                        items(state.communityReviews) { comment ->
                            ChatStyleReviewCard(comment, onBookClick, onReply = { id, txt -> viewModel.postReply(id, txt) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatStyleReviewCard(comment: Comment, onBookClick: (String) -> Unit, onReply: (String, String) -> Unit) {
    var showReplyDialog by remember { mutableStateOf(false) }

    if (showReplyDialog) {
        ReplyDialog(onDismiss = { showReplyDialog = false }, onConfirm = { onReply(comment.commentId, it) })
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        // Columna del Avatar Principal
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(70.dp)) {
            Surface(color = Color.White, shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                Text(comment.userName.split(" ").firstOrNull() ?: "", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
            }
            Image(
                painter = painterResource(AvatarHelper.getDrawableId(comment.userPhotoUrl)),
                contentDescription = null,
                modifier = Modifier.size(60.dp).clip(CircleShape).border(2.dp, Color.White, CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        // Burbuja de Comentario
        Card(
            shape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFA1887F)),
            modifier = Modifier.weight(1f).padding(start = 8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header: Libro y Capítulo
                Text("- Libro: ${comment.bookTitle}", color = Color.White, fontSize = 12.sp)
                if (comment.chapter.isNotEmpty()) {
                    Text("- Capítulo: ${comment.chapter}", color = Color.White, fontSize = 12.sp)
                }

                Spacer(Modifier.height(8.dp))
                Text("${comment.userName} comentó:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(comment.text, color = Color.White, fontSize = 14.sp)

                // Hilo de Respuestas
                comment.replies.forEach { reply ->
                    Row(modifier = Modifier.padding(top = 12.dp), verticalAlignment = Alignment.Top) {
                        Image(
                            painter = painterResource(AvatarHelper.getDrawableId(reply.userPhotoUrl)),
                            contentDescription = null,
                            modifier = Modifier.size(30.dp).clip(CircleShape).background(Color.White)
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text("${reply.userName} respondió:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(reply.text, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                // Botón Responder
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