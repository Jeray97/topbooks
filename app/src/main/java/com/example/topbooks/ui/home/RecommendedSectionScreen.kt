package com.example.topbooks.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.topbooks.data.model.Book
import com.example.topbooks.ui.components.BookItem
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.ui.theme.CenturyGotic
import com.example.topbooks.ui.theme.ColorBackGroundRecommendedSection
import com.example.topbooks.utils.Resource

@Composable
fun RecommendedSectionScreen(
    sectionType: String,
    genre: String,
    colorArgb: Int,
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    viewModel: RecommendedSectionViewModel = viewModel()
) {
    val backgroundColor = Color(colorArgb)

    LaunchedEffect(sectionType, genre) {
        viewModel.loadSectionData(sectionType, genre)
    }

    val state by viewModel.booksState.collectAsState()

    val title = when(sectionType) {
        "POPULAR" -> "Populares y Nuevos"
        "TASTES" -> "Para ti: $genre"
        "FRIENDS" -> "Actividad de Amigos"
        else -> "Libros"
    }

    RecommendedSectionContent(
        title = title,
        state = state,
        backgroundColor = backgroundColor,
        onBackClick = onBackClick,
        onBookClick = onBookClick,
        onLoadMore = { viewModel.loadNextPage() }
    )
}

@Composable
fun RecommendedSectionContent(
    title: String,
    state: Resource<List<Book>>,
    backgroundColor: Color,
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    onLoadMore: () -> Unit
) {
    Scaffold(
        containerColor = backgroundColor,
        topBar = { TopBar(onBackClick = onBackClick) }
    ) { paddingValues ->

        // CAMBIO 1: Usamos Column para apilar Título y Tarjeta verticalmente
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // CAMBIO 2: Título fuera del Card
            Text(
                text = title,
                fontFamily = CenturyGotic,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, start = 4.dp)
            )

            // TARJETA PRINCIPAL
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    // CAMBIO 3: Usamos weight(1f) para que ocupe todo el espacio restante
                    .weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.15f)
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // (El título ya no está aquí dentro)

                    when (state) {
                        is Resource.Loading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }
                        is Resource.Error -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Error al cargar contenido.", color = Color.White)
                            }
                        }
                        is Resource.Success -> {
                            if (state.data.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No se encontraron libros.", color = Color.White)
                                }
                            } else {
                                val gridState = rememberLazyGridState()

                                // Scroll Infinito
                                val reachedBottom by remember {
                                    derivedStateOf {
                                        val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()
                                        lastVisible?.index != 0 && lastVisible?.index == state.data.lastIndex
                                    }
                                }

                                LaunchedEffect(reachedBottom) {
                                    if (reachedBottom) onLoadMore()
                                }

                                // GRID DE LIBROS
                                LazyVerticalGrid(
                                    state = gridState,
                                    columns = GridCells.Fixed(2),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(state.data) { book ->
                                        BookItem(book = book, onClick = { onBookClick(book.id) })
                                    }

                                    // Espacio extra abajo
                                    item(span = { GridItemSpan(2) }) {
                                        Spacer(Modifier.height(40.dp))
                                    }
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
fun RecommendedSectionPreview() {
    val books = List(6) {
        Book("$it", "Libro Demo $it", listOf("Autor"), "", "", "2025")
    }

    RecommendedSectionContent(
        title = "Vista Previa",
        state = Resource.Success(books),
        backgroundColor = ColorBackGroundRecommendedSection,
        onBackClick = {},
        onBookClick = {},
        onLoadMore = {}
    )
}