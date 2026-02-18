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
fun FriendsActivityScreen(
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    viewModel: FriendsActivityViewModel = viewModel()
) {
    val state by viewModel.activityState.collectAsState()

    Scaffold(
        containerColor = ColorBackGroundGeneral,
        topBar = { TopBar(onBackClick = onBackClick) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                text = "Favoritos de tus amigos",
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
                        EmptyActivityMessage(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            items(resource.data) { item ->
                                FriendActivityCard(item = item, onBookClick = onBookClick)
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    EmptyActivityMessage(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                else -> {}
            }
        }
    }
}

@Composable
fun FriendActivityCard(item: FriendActivityItem, onBookClick: (String) -> Unit) {
    // Definimos el color según el tipo (opcional, para dar variedad como en tu imagen)
    val bubbleColor = if (item.rating >= 4) ColorHeaderBeige.copy(alpha = 0.9f) else ColorArcMediumBrown.copy(alpha = 0.2f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // IZQUIERDA: CARÁTULA CON MARCO BLANCO (Como en tu imagen)
        Card(
            modifier = Modifier
                .width(100.dp)
                .height(150.dp)
                .shadow(8.dp, RoundedCornerShape(8.dp))
                .clickable { onBookClick(item.bookId) },
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(4.dp, Color.White),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.bookImageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // DERECHA: BURBUJA DE COMENTARIO
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = bubbleColor)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Cabecera: Nombre + "comentó:"
                Text(
                    text = "${item.friendName} comentó:",
                    fontSize = 13.sp,
                    color = ColorArcDarkBrown,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Texto
                Text(
                    text = item.content,
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    lineHeight = 16.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // FOOTER: AMIGO (Abajo Izq) + ESTRELLAS (Abajo Der)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Avatar e info del amigo (TU PETICIÓN: mejor que mejor!)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val avatarModifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color.White, CircleShape)

                        if (item.friendPhotoUrl.startsWith("http")) {
                            AsyncImage(
                                model = item.friendPhotoUrl,
                                contentDescription = null,
                                modifier = avatarModifier,
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(AvatarHelper.getDrawableId(item.friendPhotoUrl)),
                                contentDescription = null,
                                modifier = avatarModifier,
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.friendName.split(" ").first(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorArcDarkBrown
                        )
                    }

                    // VALORACIÓN (Estrellas abajo a la derecha)
                    if (item.rating > 0) {
                        Row {
                            repeat(5) { index ->
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (index < item.rating) Color(0xFFFFD54F) else Color.LightGray.copy(alpha = 0.5f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyActivityMessage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.social),
            contentDescription = null,
            tint = ColorArcMediumBrown.copy(alpha = 0.4f),
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Aún no hay actividad", fontWeight = FontWeight.Bold, color = Color.Gray)
        Text("Sigue a más amigos para ver sus favoritos.", fontSize = 12.sp, color = Color.Gray)
    }
}