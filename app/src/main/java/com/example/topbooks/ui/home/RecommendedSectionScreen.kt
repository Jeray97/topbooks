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
    // Convertimos el ARGB recibido en un objeto Color de Compose
    val backgroundColor = Color(colorArgb)

    // Disparamos la carga de datos inicial basándonos en los parámetros de navegación
    LaunchedEffect(sectionType, genre) {
        viewModel.loadSectionData(sectionType, genre)
    }

    // Observamos el estado reactivo de la lista de libros
    val state by viewModel.booksState.collectAsState()

    // Determinamos el título de la pantalla dinámicamente según el tipo de sección
    val title = when(sectionType) {
        "popular" -> stringResource(R.string.recommended_section_title_popular)
        "tastes" -> stringResource(R.string.recommended_section_title_tastes, genre)
        "friends" -> stringResource(R.string.recommended_section_title_friends)
        else -> stringResource(R.string.recommended_section_title_default)
    }

    // Renderizamos la interfaz visual
    RecommendedSectionContent(
        title = title,
        state = state,
        backgroundColor = backgroundColor,
        onBackClick = onBackClick,
        onBookClick = onBookClick,
        onLoadMore = { viewModel.loadMore() } // Acción para cargar más páginas
    )
}

/**
 * CONTENIDO VISUAL DE LA SECCIÓN (Stateless Composable).
 * Gestiona el layout principal, el grid de libros y la lógica de scroll infinito.
 */
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

        // Estructura principal en columna para apilar el título y el contenedor de libros
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Título destacado de la sección
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

            // TARJETA CONTENEDORA PRINCIPAL
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    // weight(1f) asegura que la tarjeta ocupe todo el espacio disponible hasta abajo
                    .weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    // Usamos un blanco con transparencia para ver el color de fondo de la sección
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
                    when (state) {
                        // 1. CARGANDO: Spinner blanco centrado
                        is Resource.Loading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }
                        // 2. ERROR: Mensaje de aviso
                        is Resource.Error -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.recommended_section_error), color = Color.White)
                            }
                        }
                        // 3. ÉXITO: Lista de libros
                        is Resource.Success -> {
                            if (state.data.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(stringResource(R.string.recommended_section_empty), color = Color.White)
                                }
                            } else {
                                // --- LÓGICA DE SCROLL INFINITO ---
                                val gridState = rememberLazyGridState()

                                // TÉCNICA AVANZADA: Detectamos si el usuario ha llegado al final de la cuadrícula
                                val reachedBottom by remember {
                                    derivedStateOf {
                                        val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()
                                        // Si el índice del último elemento visible es igual al último índice de la lista, hemos llegado al fondo
                                        lastVisible?.index != 0 && lastVisible?.index == state.data.lastIndex
                                    }
                                }

                                // Si llegamos al fondo, disparamos la carga de más datos
                                LaunchedEffect(reachedBottom) {
                                    if (reachedBottom) onLoadMore()
                                }

                                // GRID VERTICAL DE LIBROS (2 columnas)
                                LazyVerticalGrid(
                                    state = gridState,
                                    columns = GridCells.Fixed(2),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(state.data) { book ->
                                        // Reutilizamos el componente BookItem estándar
                                        BookItem(book = book, onClick = { onBookClick(book.id) })
                                    }

                                    // Espacio extra al final para que los últimos libros no queden pegados al borde
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