package com.example.topbooks.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.topbooks.ui.profile.SimpleBook
import com.example.topbooks.ui.theme.*

@Composable
fun ProgressScreen(
    onNavigateToList: (String) -> Unit,
    onBookClick: (String) -> Unit,
    onAddJournalClick: () -> Unit,
    onJournalClick: (String) -> Unit,
    viewModel: ProgressViewModel = viewModel()
) {
    // 🟢 ESCUCHAMOS EL ESTADO
    val state by viewModel.uiState.collectAsState()

    // Para que se recargue siempre que entremos en la pestaña
    LaunchedEffect(Unit) {
        viewModel.loadProgressData()
    }

    Scaffold(containerColor = ColorBackGroundGeneral) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Mi Progreso",
                fontFamily = GuardianCity,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTituloTopBooks,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ColorArcMediumBrown)
                }
            } else {
                // Usamos LazyColumn como en tu diseño recuperado para un mejor rendimiento de scroll
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        ProgressSection(
                            title = "Mis Diarios",
                            books = state.journals,
                            onSeeAllClick = { onNavigateToList("journals") },
                            onBookClick = onJournalClick,
                            onAddClick = onAddJournalClick // Este sí lleva "+"
                        )
                    }
                    item {
                        ProgressSection(
                            title = "Mis Favoritos",
                            books = state.favorites,
                            onSeeAllClick = { onNavigateToList("favorites") },
                            onBookClick = onBookClick
                        )
                    }
                    item {
                        ProgressSection(
                            title = "Mis Pendientes",
                            books = state.pending,
                            onSeeAllClick = { onNavigateToList("bookmarks") },
                            onBookClick = onBookClick
                        )
                    }
                    item {
                        ProgressSection(
                            title = "Mis Leídos",
                            books = state.read,
                            onSeeAllClick = { onNavigateToList("read") },
                            onBookClick = onBookClick
                        )
                    }
                    // Espacio extra al final para que no tape el bottom nav
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun ProgressSection(
    title: String,
    books: List<SimpleBook>,
    onSeeAllClick: () -> Unit,
    onBookClick: (String) -> Unit,
    onAddClick: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp), // Padding en el header
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontFamily = CenturyGotic,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ColorArcDarkBrown,
                modifier = Modifier.weight(1f)
            )

            // Si le pasamos la función onAddClick, dibuja el icono "+"
            if (onAddClick != null) {
                IconButton(onClick = onAddClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir", tint = ColorArcDarkBrown)
                }
            }

            // Flecha para ver la lista completa
            IconButton(onClick = onSeeAllClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Ver todo", tint = ColorArcDarkBrown)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (books.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(130.dp)
                    .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay libros aquí aún.", color = Color.Gray, fontSize = 12.sp, fontFamily = CenturyGotic)
            }
        } else {
            // Ponemos el padding aquí para que al hacer scroll horizontal los elementos no se corten bruscamente
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(books) { book ->
                    ProgressBookItem(book = book, onClick = { onBookClick(book.id) })
                }
            }
        }
    }
}

@Composable
fun ProgressBookItem(book: SimpleBook, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(90.dp).clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = book.imageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(90.dp, 130.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.5f)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = book.title,
            fontSize = 11.sp,
            fontFamily = CenturyGotic,
            color = Color.DarkGray,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}