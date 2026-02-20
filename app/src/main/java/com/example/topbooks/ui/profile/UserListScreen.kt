package com.example.topbooks.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.ui.theme.*

@Composable
fun UserListScreen(
    type: String,
    userId: String,
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: UserListViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(type, userId) {
        viewModel.loadData(type, userId)
    }

    // 1. AÑADIDOS LOS NUEVOS TÍTULOS
    val title = when(type) {
        "friends" -> "Amigos"
        "reviews" -> "Reseñas"
        "read" -> "Leídos"
        "journals" -> "Mis Diarios"
        "bookmarks" -> "Marcadores"
        "comments" -> "Comentarios"
        else -> "Lista"
    }

    Scaffold(
        containerColor = ColorBackGroundGeneral,
        topBar = { TopBar(onBackClick = onBackClick) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                text = title,
                fontFamily = CenturyGotic,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTituloTopBooks
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = ColorArcMediumBrown)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // 2. AÑADIDAS LAS NUEVAS RUTAS A LOS ITEMS
                    when(type) {
                        "friends" -> items(state.friends) { FriendItem(it, onUserClick) }
                        "read", "bookmarks" -> items(state.readBooks) { BookItem(it, onBookClick) }
                        "reviews", "journals", "comments" -> items(state.reviews) { ReviewListItem(it, onBookClick) }
                    }

                    // 3. AÑADIDAS LAS NUEVAS RUTAS A LA LÓGICA DE VACÍO
                    val isEmpty = when(type) {
                        "friends" -> state.friends.isEmpty()
                        "read", "bookmarks" -> state.readBooks.isEmpty()
                        "reviews", "journals", "comments" -> state.reviews.isEmpty()
                        else -> true
                    }

                    if (isEmpty) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(top = 40.dp), Alignment.Center) {
                                Text("No hay elementos para mostrar.", color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FriendItem(user: SimpleUser, onClick: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { if(user.uid.isNotEmpty()) onClick(user.uid) },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            com.example.topbooks.ui.friends.UserAvatarItem(user.photo, size = 50.dp)
            Spacer(Modifier.width(12.dp))
            Text(user.name, fontWeight = FontWeight.Bold, color = ColorArcDarkBrown, fontSize = 16.sp)
        }
    }
}

@Composable
fun BookItem(book: SimpleBook, onClick: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { if(book.id.isNotEmpty()) onClick(book.id) },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = book.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(45.dp, 65.dp).clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Text(book.title, fontWeight = FontWeight.Bold, color = ColorArcDarkBrown, fontSize = 15.sp)
        }
    }
}

@Composable
fun ReviewListItem(review: com.example.topbooks.data.model.Comment, onBookClick: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ColorArcMediumBrown),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // PORTADA DEL LIBRO
            AsyncImage(
                model = review.bookImageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp, 75.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { if(review.bookId.isNotEmpty()) onBookClick(review.bookId) },
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(12.dp))

            // CONTENIDO DE LA RESEÑA
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = review.bookTitle.ifEmpty { "Libro: ${review.bookId}" },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.clickable { if(review.bookId.isNotEmpty()) onBookClick(review.bookId) }
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = review.text,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                )

                Spacer(Modifier.height(8.dp))

                Row {
                    repeat(5) { i ->
                        val color = if(i < review.rating) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.3f)
                        Icon(Icons.Default.Star, null, tint = color, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}