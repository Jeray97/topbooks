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
import androidx.compose.ui.graphics.Color
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
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.ui.theme.*
import com.example.topbooks.utils.AvatarHelper

/**
 * PANTALLA DE RESEÑAS Y COMUNIDAD (Stateful Composable).
 * Muestra un listado de comentarios que puede ser global (Muro de la comunidad)
 * o específico para un libro (Hilo de discusión).
 *
 * @param onBackClick Acción para regresar.
 * @param onBookClick Acción al pulsar sobre el título del libro en un comentario.
 * @param viewModel Gestiona la carga de la actividad social y la publicación de respuestas.
 * @param bookId Si se provee, filtra los comentarios solo para ese libro.
 * @param targetCommentId Si se provee, resalta y desplaza la vista hacia ese comentario.
 */
@Composable
fun ReviewsScreen(
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    viewModel: ReviewsViewModel = viewModel(),
    bookId: String? = null,
    targetCommentId: String? = null
) {
    val state by viewModel.uiState.collectAsState()

    // Recarga el feed social cada vez que cambia el libro objetivo
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Cabecera dinámica según el contexto de la pantalla
            item {
                Text(
                    text = if (bookId != null) stringResource(R.string.reviews_title_thread) else stringResource(R.string.reviews_title_community),
                    fontFamily = CenturyGotic,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = ColorArcDarkBrown,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ColorArcMediumBrown)
                    }
                }
            } else {
                // Unificamos las reseñas de amigos y comunidad evitando duplicados
                val allReviews = state.friendsReviews + state.communityReviews

                items(allReviews.distinctBy { it.commentId }) { comment ->
                    // Lógica para resaltar un comentario específico (ej. desde una notificación)
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

/**
 * REPRESENTACIÓN VISUAL DE UNA RESEÑA (Stateless Composable).
 * Muestra el comentario principal, metadatos del libro y el hilo de respuestas anidadas.
 */
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

    // Diálogo flotante para escribir una respuesta
    if (showReplyDialog) {
        ReplyDialog(onDismiss = { showReplyDialog = false }, onConfirm = onReply)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        // Elevación y borde extra si el comentario está resaltado
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 8.dp else 2.dp),
        border = if (isHighlighted) androidx.compose.foundation.BorderStroke(2.dp, ColorArcMediumBrown) else null,
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ETIQUETA DE DESTACADO (Solo si aplica)
            if (isHighlighted) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.reviews_badge_highlighted), color = ColorArcMediumBrown, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // CABECERA: Datos del autor y contexto del libro
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatarItem(comment.userPhotoUrl, size = 48.dp)
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(comment.userName, color = ColorArcDarkBrown, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    // Subtítulo que indica el libro y opcionalmente el capítulo
                    val subtitleText = if (comment.chapter.isNotBlank()) {
                        stringResource(R.string.reviews_subtitle_with_chapter, comment.bookTitle, comment.chapter)
                    } else {
                        stringResource(R.string.reviews_subtitle_book_only, comment.bookTitle)
                    }

                    Text(
                        text = subtitleText,
                        color = ColorArcMediumBrown,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { onBookClick() }
                    )
                }
            }

            // CUERPO: Texto de la reseña o comentario
            Spacer(modifier = Modifier.height(12.dp))
            Text(comment.text, color = Color.DarkGray, fontSize = 15.sp, lineHeight = 22.sp)

            // HILO DE RESPUESTAS (Anidadas)
            if (comment.replies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    comment.replies.forEach { reply ->
                        // Burbuja de respuesta con sangría a la izquierda
                        Surface(
                            color = Color(0xFFF5F5F5),
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                            modifier = Modifier.padding(start = 16.dp)
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

            // ACCIONES: Botón de responder
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        // Antes de permitir responder, verificamos la identidad del usuario
                        onCheckVerification { isVerified ->
                            if (isVerified) showReplyDialog = true
                            else Toast.makeText(context, context.getString(R.string.reviews_toast_verify_email), Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ColorArcMediumBrown)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = stringResource(R.string.reviews_button_reply), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.reviews_button_reply), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

/**
 * Diálogo interactivo para redactar una respuesta a un comentario.
 */
@Composable
fun ReplyDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.reviews_dialog_title), fontFamily = GuardianCity, fontWeight = FontWeight.Bold, color = ColorArcDarkBrown)
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(R.string.reviews_dialog_placeholder)) },
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
            ) { Text(stringResource(R.string.reviews_dialog_publish)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.reviews_dialog_cancel), color = Color.Gray) }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

/**
 * Mini-componente para mostrar el avatar del usuario de forma circular.
 */
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