package com.example.topbooks.ui.category

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.topbooks.data.model.Book
import com.example.topbooks.ui.components.BookItem
import com.example.topbooks.ui.theme.*
import com.example.topbooks.utils.Resource
import com.example.topbooks.ui.components.CategoryDetailContentBackgroundShape
import com.example.topbooks.ui.components.SearchBarCustom
import com.example.topbooks.ui.components.TopBar

/**
 * PANTALLA DE DETALLE DE UNA CATEGORÍA (Stateful Composable)
 * * Es la responsable de gestionar la comunicación con [CategoryDetailViewModel]
 * para solicitar y escuchar la lista de libros de un género específico.
 *
 * @param categoryName Nombre de la categoría formateado (ej. "Ciencia Ficción") para mostrarlo como título.
 * @param query La cadena de búsqueda real que se enviará a las APIs (ej. "subject:science fiction").
 * @param onBackClick Acción para regresar a la pantalla anterior.
 * @param onBookClick Callback que navega a la pantalla de detalles de un libro concreto.
 * @param onScanClick Callback para abrir la cámara e iniciar el escaneo de código de barras.
 * @param viewModel ViewModel instanciado automáticamente que gestiona los estados de la pantalla.
 */
@Composable
fun CategoryDetailScreen(
    categoryName: String,
    query: String,
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    onScanClick: () -> Unit,
    viewModel: CategoryDetailViewModel = viewModel()
) {
    // Al entrar en la pantalla (o si la 'query' cambia), le pedimos al ViewModel que busque los libros
    LaunchedEffect(key1 = query) {
        viewModel.fetchBooksByCategory(query)
    }

    // Observamos el estado de forma reactiva
    val state by viewModel.booksState.collectAsState()

    // Renderizamos la parte puramente visual
    CategoryDetailContent(
        categoryName = categoryName,
        state = state,
        onBackClick = onBackClick,
        onBookClick = onBookClick,
        onScanClick = onScanClick
    )
}

/**
 * INTERFAZ VISUAL DEL DETALLE DE CATEGORÍA (Stateless Composable)
 * * Recibe el estado [Resource] ya procesado y se encarga únicamente de dibujar la UI:
 * el spinner de carga, la cuadrícula de libros o los mensajes de error.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailContent(
    categoryName: String,
    state: Resource<List<Book>>,
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    onScanClick: () -> Unit
) {
    // Envolvemos to-do el contenido dentro del fondo personalizado con forma de ola en la parte superior
    CategoryDetailContentBackgroundShape {

        Scaffold(
            containerColor = Color.Transparent, // Importante para no tapar el fondo personalizado
            topBar = { TopBar(onBackClick) }
        ) { paddingValues ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(26.dp))

                // --- BARRA DE BÚSQUEDA ---
                // Pasamos los eventos de navegación y escaneo hacia arriba
                SearchBarCustom(onBookClick = onBookClick, onScanClick = onScanClick)

                Spacer(modifier = Modifier.height(40.dp))

                // --- TÍTULO DE LA CATEGORÍA ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = categoryName,
                        color = ColorTituloCategoriaDetalle,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center,
                        fontFamily = CenturyGotic,
                        fontWeight = FontWeight.Bold
                    )
                }

                // --- CONTENIDO PRINCIPAL: GESTIÓN DE ESTADOS ---
                Box(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Utiliza la clase [Resource] para pintar la UI correcta en cada momento
                    when (state) {
                        // 1. ESTADO DE CARGA: Muestra el indicador circular
                        is Resource.Loading -> CircularProgressIndicator(color = ColorTextPrimary)

                        // 2. ESTADO DE ÉXITO: Los libros ya se han descargado
                        is Resource.Success -> {
                            val books = state.data

                            // Comprobación de seguridad por si la API no devuelve resultados
                            if (books.isEmpty()) {
                                Text("No hay libros disponibles.", color = ColorTextPrimary, fontFamily = CenturyGotic)
                            } else {
                                // Cuadrícula de 2 columnas para mostrar los libros estilo estantería
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    contentPadding = PaddingValues(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(books) { book ->
                                        BookItem(book = book, onClick = { onBookClick(book.id) })
                                    }
                                }
                            }
                        }

                        // 3. ESTADO DE ERROR: Mostramos el fallo en color rojo para depuración rápida
                        is Resource.Error -> Text(text = "Error: ${state.exception.message}", color = Color.Red, fontFamily = CenturyGotic)

                        // Estado Idle (Inicial) no hace nada
                        else -> {}
                    }
                }
            }
        }
    }
}