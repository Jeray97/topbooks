package com.example.topbooks.ui.reviews

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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

    // Controlador para mover la lista
    val listState = rememberLazyListState()

    LaunchedEffect(bookId) {
        viewModel.loadSocialFeed(bookId, targetCommentId)
    }

    // Unificamos las reseñas aquí arriba para poder buscar en ellas antes de pintar la lista
    val allReviews = remember(state.friendsReviews, state.communityReviews) {
        (state.friendsReviews + state.communityReviews).distinctBy { it.commentId }
    }

    // Efecto que se dispara cuando los comentarios terminan de cargar
    LaunchedEffect(allReviews, targetCommentId) {
        if (targetCommentId != null && allReviews.isNotEmpty()) {
            // Buscamos en qué posición (índice) está nuestro comentario
            val index = allReviews.indexOfFirst { it.commentId == targetCommentId }
            if (index != -1) {
                // Hacemos scroll suave hasta ese ítem.
                // Sumamos +1 porque el "item" del título principal ("Hilo de conversación") ocupa la posición 0.
                listState.animateScrollToItem(index + 1)
            }
        }
    }

    Scaffold(
        containerColor = ColorBackGroundGeneral,
        topBar = { TopBar(onBackClick = onBackClick) }
    ) { padding ->
        LazyColumn(
            state = listState, //Enganchamos el controlador a la lista
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp) // Un poco más de espacio entre tarjetas
        ) {
            item {
                Text(
                    text = if (bookId != null) "Hilo de conversación" else "Actividad de la Comunidad",
                    fontFamily = CenturyGotic,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = ColorArcDarkBrown,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (state.isLoading) {
                item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ColorArcMediumBrown) } }
            } else {
                items(allReviews) { comment ->
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
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 8.dp else 2.dp),
        border = if (isHighlighted) androidx.compose.foundation.BorderStroke(2.dp, ColorArcMediumBrown) else null,
        colors = CardDefaults.cardColors(containerColor = Color.White) // FONDOS BLANCOS
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ETIQUETA DE DESTACADO (Deep Link)
            if (isHighlighted) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Destacado", color = ColorArcMediumBrown, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // CABECERA DEL COMENTARIO (Con Capítulo)
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatarItem(comment.userPhotoUrl, size = 48.dp)
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(comment.userName, color = ColorArcDarkBrown, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    // Verificamos si hay capítulo para mostrarlo
                    val subtitleText = if (comment.chapter.isNotBlank()) {
                        "sobre ${comment.bookTitle} • Cap. ${comment.chapter}"
                    } else {
                        "sobre ${comment.bookTitle}"
                    }

                    Text(
                        text = subtitleText,
                        color = ColorArcMediumBrown,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { onBookClick() }
                    )
                }
            }

            // TEXTO PRINCIPAL
            Spacer(modifier = Modifier.height(12.dp))
            Text(comment.text, color = Color.DarkGray, fontSize = 15.sp, lineHeight = 22.sp)

            // BURBUJAS DE RESPUESTA
            if (comment.replies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    comment.replies.forEach { reply ->
                        // La burbuja de respuesta
                        Surface(
                            color = Color(0xFFF5F5F5), // Gris muy clarito
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                            modifier = Modifier.padding(start = 16.dp) // Indentación
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    UserAvatarItem(reply.userPhotoUrl, size = 24.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(reply.userName, color = ColorArcDarkBrown, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(reply.text, color = Color.DarkGray, fontSize = 13.sp, lineHeight = 18.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)

            // BOTÓN DE RESPONDER MEJORADO
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { showReplyDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = ColorArcMediumBrown)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = "Responder", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Responder", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
            Text("Añadir respuesta", fontFamily = GuardianCity, fontWeight = FontWeight.Bold, color = ColorArcDarkBrown)
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Escribe algo amable...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ColorArcMediumBrown)
            )
        },
        confirmButton = {
            Button(
                onClick = { if(text.isNotBlank()) onConfirm(text); onDismiss() },
                colors = ButtonDefaults.buttonColors(containerColor = ColorArcMediumBrown),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Publicar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun UserAvatarItem(photoUrl: String, size: androidx.compose.ui.unit.Dp = 70.dp) {
    val resId = AvatarHelper.getDrawableId(photoUrl)
    Image(
        painter = painterResource(id = resId),
        contentDescription = null,
        modifier = Modifier.size(size).clip(CircleShape).background(Color.White).border(1.dp, Color.LightGray.copy(alpha = 0.5f), CircleShape),
        contentScale = ContentScale.Crop
    )
}