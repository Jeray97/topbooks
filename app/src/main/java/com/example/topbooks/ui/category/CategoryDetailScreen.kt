package com.example.topbooks.ui.category

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.topbooks.R
import com.example.topbooks.data.model.Book
import com.example.topbooks.ui.components.BookItem
import com.example.topbooks.ui.theme.*
import com.example.topbooks.utils.Resource
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
    // Usamos un Box para poner tu imagen vectorial de fondo en toda la pantalla
    Box(modifier = Modifier.fillMaxSize()) {

        // --- IMAGEN VECTORIAL DE FONDO ---
        Image(
            painter = painterResource(id = R.drawable.category_details_background),
            contentDescription = null,
            contentScale = ContentScale.Crop, // Ajusta a Crop o FillBounds según cómo sea tu vector
            modifier = Modifier.fillMaxSize()
        )

        Scaffold(
            containerColor = Color.Transparent, // Importante para que se vea la imagen de fondo
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (state) {
                        is Resource.Loading -> CircularProgressIndicator(color = ColorTextPrimary)

                        is Resource.Success -> {
                            val books = state.data

                            if (books.isEmpty()) {
                                Text("No hay libros disponibles.", color = ColorTextPrimary, fontFamily = CenturyGotic)
                            } else {
                                // Cuadrícula actualizada a 3 columnas
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    contentPadding = PaddingValues(bottom = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(books) { book ->
                                        BookItem(book = book, onClick = { onBookClick(book.id) })
                                    }
                                }
                            }
                        }

                        is Resource.Error -> Text(text = "Error: ${state.exception.message}", color = Color.Red, fontFamily = CenturyGotic)

                        else -> {}
                    }
                }
            }
        }
    }
}