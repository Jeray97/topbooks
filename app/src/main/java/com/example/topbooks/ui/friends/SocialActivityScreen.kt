package com.example.topbooks.ui.friends

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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
import com.example.topbooks.R
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.ui.theme.*
import com.example.topbooks.utils.AvatarHelper
import com.example.topbooks.utils.Resource

@Composable
fun SocialActivityScreen(
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    viewModel: SocialActivityViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = ColorBackGroundGeneral,
        topBar = { TopBar(onBackClick = onBackClick) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(
                text = "Actividad Reciente",
                fontFamily = CenturyGotic,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTituloTopBooks,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            when (val resource = state) {
                is Resource.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ColorArcMediumBrown)
                    }
                }
                is Resource.Success -> {
                    if (resource.data.isEmpty()) {
                        EmptySocialMessage(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            items(resource.data) { item ->
                                SocialActivityCard(item = item, onBookClick = onBookClick)
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    EmptySocialMessage(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                else -> {}
            }
        }
    }
}

@Composable
fun SocialActivityCard(item: SocialActivityItem, onBookClick: (String) -> Unit) {
    val bubbleColor = when(item.type) {
        ActivityType.FAVORITE -> Color(0xFFFCE4EC)
        ActivityType.REPLY -> Color(0xFFE3F2FD) // Azul suave para respuestas
        else -> ColorHeaderBeige.copy(alpha = 0.9f)
    }

    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier.width(90.dp).height(140.dp)
                .shadow(6.dp, RoundedCornerShape(8.dp))
                .clickable { onBookClick(item.bookId) },
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(3.dp, Color.White),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.bookImageUrl)
                    .crossfade(true)
                    .error(R.drawable.cat_resenas_icon)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Card(
            modifier = Modifier.weight(1f).fillMaxHeight().padding(vertical = 4.dp),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 2.dp),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp).fillMaxSize()) {
                val actionText = when(item.type) {
                    ActivityType.REVIEW -> "escribió una reseña"
                    ActivityType.FAVORITE -> "guardó en favoritos"
                    ActivityType.COMMENT -> "comentó en"
                    ActivityType.REPLY -> "respondió a ${item.replyToName}"
                }

                Text(
                    text = "${item.friendName} $actionText",
                    fontSize = 12.sp,
                    color = ColorArcDarkBrown,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.bookTitle,
                    fontSize = 11.sp,
                    color = Color(0xFF8D6E63),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // BURBUJA DE RESPUESTA (CITA)
                if (item.type == ActivityType.REPLY && item.replyToContent != null) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = item.replyToName ?: "Usuario",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Text(
                                text = item.replyToContent,
                                fontSize = 11.sp,
                                color = Color.Gray,
                                maxLines = 2,
                                fontStyle = FontStyle.Italic,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Text(
                    text = item.content,
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    lineHeight = 15.sp,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SmallAvatar(item.friendPhotoUrl)
                    if (item.type == ActivityType.REVIEW && item.rating > 0) {
                        Row {
                            Icon(Icons.Default.Star, null, tint = Color(0xFFFFD54F), modifier = Modifier.size(12.dp))
                            Text("${item.rating}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (item.type == ActivityType.FAVORITE) {
                        Icon(Icons.Default.Favorite, null, tint = Color(0xFFE57373), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SmallAvatar(photoUrl: String) {
    val modifier = Modifier.size(24.dp).clip(CircleShape).border(1.dp, Color.White, CircleShape).background(Color.White)
    if (photoUrl.startsWith("http")) {
        AsyncImage(model = photoUrl, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    } else {
        Image(painter = painterResource(AvatarHelper.getDrawableId(photoUrl)), contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    }
}

@Composable
fun EmptySocialMessage(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(painter = painterResource(R.drawable.social), contentDescription = null, tint = ColorArcMediumBrown.copy(alpha = 0.4f), modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Todo está muy tranquilo...", fontWeight = FontWeight.Bold, color = Color.Gray)
        Text("Tus amigos aún no han realizado actividades.", fontSize = 12.sp, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}