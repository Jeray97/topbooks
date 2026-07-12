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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.topbooks.R
import com.example.topbooks.data.model.Book
import com.example.topbooks.ui.components.BookItem
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.ui.theme.CenturyGotic
import com.example.topbooks.utils.Resource

/**
 * PANTALLA DE SECCIÓN RECOMENDADA AMPLIADA (Stateful Composable).
 * Se encarga de mostrar en una cuadrícula completa los libros de una categoría específica (Populares, Gustos o Amigos).
 *
 * @param sectionType Identificador de la sección ("popular", "tastes", "friends").
 * @param genre Género literario a filtrar (si aplica).
 * @param colorArgb Color de fondo en formato entero (ARGB) para mantener coherencia visual con la pantalla anterior.
 * @param onBackClick Navegación hacia atrás.
 * @param onBookClick Acción al seleccionar un libro.
 * @param viewModel ViewModel que gestiona la paginación y el estado de los libros.
 */
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
        "popular" -> stringResource(R.string.recommended_section_title_popular)
        "tastes" -> stringResource(R.string.recommended_section_title_tastes, genre)
        "friends" -> stringResource(R.string.recommended_section_title_friends)
        else -> stringResource(R.string.recommended_section_title_default)
    }

    RecommendedSectionContent(
        title = title,
        state = state,
        backgroundColor = backgroundColor,
        onBackClick = onBackClick,
        onBookClick = onBookClick,
        onLoadMore = { viewModel.loadMore() }
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
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

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.15f)
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (state) {
                        is Resource.Loading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }
                        is Resource.Error -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.recommended_section_error), color = Color.White)
                            }
                        }
                        is Resource.Success -> {
                            if (state.data.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(stringResource(R.string.recommended_section_empty), color = Color.White)
                                }
                            } else {
                                val gridState = rememberLazyGridState()

                                // --- LÓGICA DE SCROLL INFINITO MEJORADA (Con Threshold/Margen) ---
                                val reachedBottom by remember {
                                    derivedStateOf {
                                        val layoutInfo = gridState.layoutInfo
                                        val totalItems = layoutInfo.totalItemsCount
                                        val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

                                        // Disparamos cuando el usuario llega a los últimos 4 elementos
                                        // Esto da tiempo a internet para cargar la siguiente página sin que se note el corte
                                        totalItems > 0 && lastVisibleItem >= totalItems - 4
                                    }
                                }

                                LaunchedEffect(reachedBottom) {
                                    if (reachedBottom) onLoadMore()
                                }

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

                                    // Nuestro famoso espaciador "fantasma" que ya no romperá el cálculo
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