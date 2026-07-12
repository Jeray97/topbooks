package com.example.topbooks.ui.club

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.topbooks.data.model.DiscussionMessage
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.ui.theme.CenturyGotic
import com.example.topbooks.ui.theme.ColorArcDarkBrown
import com.example.topbooks.ui.theme.ColorArcMediumBrown
import com.example.topbooks.ui.theme.ColorBackGroundGeneral
import com.example.topbooks.ui.theme.ColorTextPrimary
import com.example.topbooks.ui.theme.GuardianCity
import com.example.topbooks.utils.AvatarHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SUB_TEXT = Color(0xFF8D5B4C)
private val MESSAGE_BG_MINE = Color(0xFFF6E6DD)
private val MESSAGE_BG_OTHER = Color.White
private val MESSAGE_BORDER = Color(0xFFECDDD2)

@Composable
fun DiscussionScreen(
    clubId: String,
    discussionId: String,
    onBackClick: () -> Unit,
    viewModel: DiscussionViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(clubId, discussionId) {
        viewModel.loadDiscussion(clubId, discussionId)
    }

    LaunchedEffect(state.discussion?.messages?.size) {
        val messageCount = state.discussion?.messages?.size ?: 0
        if (messageCount > 0) {
            listState.animateScrollToItem(messageCount - 1)
        }
    }

    Scaffold(
        containerColor = ColorBackGroundGeneral(),
        topBar = {
            TopBar(
                onBackClick = onBackClick,
                title = state.discussion?.title ?: "Discusión"
            )
        },
        bottomBar = {
            if (state.discussion != null) {
                MessageComposeBar(
                    text = messageText,
                    onTextChange = { messageText = it },
                    isSending = state.isSending,
                    onSendClick = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(messageText)
                            messageText = ""
                        }
                    }
                )
            }
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ColorArcMediumBrown())
                }
            }
            state.discussion == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Discusión no encontrada",
                        fontFamily = CenturyGotic,
                        fontSize = 14.sp,
                        color = SUB_TEXT
                    )
                }
            }
            else -> {
                val discussion = state.discussion!!
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        DiscussionHeader(
                            title = discussion.title,
                            chapter = discussion.chapter,
                            creatorName = discussion.creatorName,
                            isSpoiler = discussion.isSpoiler,
                            messageCount = discussion.messageCount
                        )
                    }

                    if (discussion.messages.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Aún no hay mensajes. ¡Inicia la conversación!",
                                    fontFamily = CenturyGotic,
                                    fontSize = 13.sp,
                                    color = SUB_TEXT
                                )
                            }
                        }
                    } else {
                        items(discussion.messages, key = { it.id }) { message ->
                            MessageBubble(message = message)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscussionHeader(
    title: String,
    chapter: String,
    creatorName: String,
    isSpoiler: Boolean,
    messageCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MESSAGE_BORDER, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isSpoiler) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Spoiler",
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = title,
                fontFamily = GuardianCity,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = ColorArcDarkBrown(),
                modifier = Modifier.weight(1f)
            )
        }

        if (chapter.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Capítulo: $chapter",
                fontFamily = CenturyGotic,
                fontSize = 12.sp,
                color = SUB_TEXT
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Iniciada por $creatorName",
                fontFamily = CenturyGotic,
                fontSize = 11.sp,
                color = SUB_TEXT,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$messageCount mensajes",
                fontFamily = CenturyGotic,
                fontSize = 11.sp,
                color = SUB_TEXT
            )
        }
    }
}

@Composable
private fun MessageBubble(message: DiscussionMessage) {
    val avatarRes = AvatarHelper.getDrawableId(message.userPhotoUrl)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Image(
            painter = painterResource(id = avatarRes),
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, Color.LightGray.copy(alpha = 0.3f), CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = message.userName,
                    fontFamily = CenturyGotic,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = ColorArcDarkBrown(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatMessageTime(message.createdAt),
                    fontFamily = CenturyGotic,
                    fontSize = 10.sp,
                    color = SUB_TEXT
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp))
                    .background(MESSAGE_BG_OTHER)
                    .border(
                        1.dp,
                        MESSAGE_BORDER,
                        RoundedCornerShape(topStart = 4.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
                    )
                    .padding(10.dp)
            ) {
                Text(
                    text = message.text,
                    fontFamily = CenturyGotic,
                    fontSize = 13.sp,
                    color = ColorTextPrimary(),
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun MessageComposeBar(
    text: String,
    onTextChange: (String) -> Unit,
    isSending: Boolean,
    onSendClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MESSAGE_BORDER)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    text = "Escribe un mensaje...",
                    fontFamily = CenturyGotic,
                    fontSize = 13.sp
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = ColorBackGroundGeneral(),
                unfocusedContainerColor = ColorBackGroundGeneral(),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(24.dp),
            maxLines = 4
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onSendClick,
            enabled = text.isNotBlank() && !isSending,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (text.isNotBlank() && !isSending) ColorArcDarkBrown()
                    else Color.LightGray
                )
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun formatMessageTime(date: Date?): String {
    if (date == null) return ""
    val now = System.currentTimeMillis()
    val diff = now - date.time
    val minutes = diff / (1000 * 60)
    val hours = diff / (1000 * 60 * 60)
    val days = diff / (1000 * 60 * 60 * 24)

    return when {
        minutes < 1 -> "Ahora"
        minutes < 60 -> "Hace ${minutes}m"
        hours < 24 -> "Hace ${hours}h"
        days < 7 -> "Hace ${days}d"
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(date)
    }
}
