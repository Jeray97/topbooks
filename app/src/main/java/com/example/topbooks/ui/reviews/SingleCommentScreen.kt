package com.example.topbooks.ui.reviews

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.topbooks.R
import com.example.topbooks.data.model.Comment
import com.example.topbooks.data.model.Reply
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.ui.theme.*
import com.example.topbooks.utils.AvatarHelper
import com.google.firebase.auth.FirebaseAuth

@Composable
fun SingleCommentScreen(
    commentId: String,
    onBackClick: () -> Unit,
    viewModel: SingleCommentViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var replyText by remember { mutableStateOf("") }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(commentId) {
        viewModel.loadComment(commentId)
    }

    Scaffold(
        containerColor = ColorBackGroundGeneral,
        topBar = { TopBar(onBackClick = onBackClick) },
        bottomBar = {
            if (state.comment != null) {
                ReplyBottomBar(
                    text = replyText,
                    onTextChange = { replyText = it },
                    isSending = state.isSendingReply,
                    onSendClick = {
                        // 🔥 VALIDACIÓN EN LA VISTA: Limpiamos y comprobamos antes de procesar
                        val textoLimpio = replyText.trim()
                        if (textoLimpio.isNotEmpty()) {
                            viewModel.checkEmailVerification { verified ->
                                if (verified) {
                                    viewModel.sendReply(textoLimpio) {
                                        replyText = "" // Limpiamos la caja al enviarse
                                    }
                                } else {
                                    Toast.makeText(context, context.getString(R.string.reviews_toast_verify_email), Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        if (state.isLoading && state.comment == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = ColorArcMediumBrown)
            }
        } else if (state.comment != null) {
            val comment = state.comment!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.single_comment_thread),
                        fontFamily = CenturyGotic,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTituloTopBooks,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item {
                    PremiumMainCommentCard(comment)
                }

                if (comment.replies.isNotEmpty()) {
                    items(comment.replies) { reply ->
                        PremiumReplyBubble(reply, reply.userId == currentUserId)
                    }
                } else {
                    item {
                        Text(
                            stringResource(R.string.single_comment_no_replies),
                            color = Color.Gray,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(60.dp)) }
            }
        } else {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(stringResource(R.string.single_comment_not_found), color = Color.Gray)
            }
        }
    }
}

@Composable
fun PremiumMainCommentCard(comment: Comment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ColorArcMediumBrown),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White).padding(2.dp)
                ) {
                    LoadableAvatar(photoUrl = comment.userPhotoUrl, modifier = Modifier.fillMaxSize().clip(CircleShape))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(comment.userName, fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 17.sp, fontFamily = GuardianCity)
                    if (comment.chapter.isNotEmpty()) {
                        Surface(color = Color.White.copy(alpha = 0.2f), shape = CircleShape, modifier = Modifier.padding(top = 4.dp)) {
                            Text(
                                stringResource(R.string.single_comment_chapter, comment.chapter),
                                fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(0.9f),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Surface(color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text(comment.text, color = Color.White, fontSize = 15.sp, lineHeight = 22.sp, fontFamily = GuardianCity, modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
fun PremiumReplyBubble(reply: Reply, isMe: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isMe) {
            LoadableAvatar(photoUrl = reply.userPhotoUrl, modifier = Modifier.padding(top = 4.dp).size(32.dp).clip(CircleShape))
            Spacer(Modifier.width(12.dp))
        }

        Surface(
            color = Color.White,
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            shadowElevation = 2.dp,
            modifier = Modifier.weight(1f, fill = false).widthIn(max = 280.dp)
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (!isMe) {
                    Text(reply.userName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ColorArcDarkBrown, fontFamily = GuardianCity)
                    Spacer(Modifier.height(4.dp))
                }
                Text(reply.text, color = if (isMe) ColorArcDarkBrown else Color.DarkGray, fontSize = 14.sp, lineHeight = 19.sp, fontFamily = GuardianCity)
            }
        }

        if (isMe) {
            Spacer(Modifier.width(12.dp))
            LoadableAvatar(photoUrl = reply.userPhotoUrl, modifier = Modifier.padding(top = 4.dp).size(32.dp).clip(CircleShape))
        }
    }
}

@Composable
fun ReplyBottomBar(text: String, onTextChange: (String) -> Unit, isSending: Boolean, onSendClick: () -> Unit) {
    Surface(
        color = Color.White,
        shadowElevation = 16.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text(stringResource(R.string.reviews_dialog_placeholder), color = Color.LightGray) },
                modifier = Modifier.weight(1f),
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ColorArcMediumBrown,
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                    cursorColor = ColorArcMediumBrown
                )
            )

            Spacer(modifier = Modifier.width(12.dp))

            if (isSending) {
                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ColorArcMediumBrown, modifier = Modifier.size(24.dp))
                }
            } else {
                FloatingActionButton(
                    //Aquí también podemos hacer que el botón no responda si el texto recortado está vacío
                    onClick = { if (text.trim().isNotEmpty()) onSendClick() },
                    containerColor = if (text.trim().isNotEmpty()) ColorArcMediumBrown else Color.LightGray.copy(alpha = 0.5f),
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.boton_enviar_desc),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LoadableAvatar(photoUrl: String, modifier: Modifier) {
    if (photoUrl.startsWith("http")) {
        AsyncImage(model = photoUrl, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    } else {
        Image(painter = painterResource(AvatarHelper.getDrawableId(photoUrl)), contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    }
}