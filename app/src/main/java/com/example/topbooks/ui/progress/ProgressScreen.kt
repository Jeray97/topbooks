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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.topbooks.R
import com.example.topbooks.ui.profile.SimpleBook
import com.example.topbooks.ui.theme.*

/**
 * PANTALLA DE PROGRESO Y BIBLIOTECA (Stateful Composable).
 * Centraliza el seguimiento de lectura del usuario, dividiendo el contenido en diarios,
 * favoritos, libros pendientes y libros leídos.
 *
 * @param onNavigateToList Acción para abrir la vista completa de una categoría específica.
 * @param onBookClick Acción para ver los detalles de un libro.
 * @param onAddJournalClick Acción para crear una nueva entrada en el diario de lectura.
 * @param onJournalClick Acción para visualizar un diario existente.
 * @param viewModel Maneja la lógica de obtención de datos desde los distintos repositorios.
 */
@Composable
fun ProgressScreen(
    onNavigateToList: (String) -> Unit,
    onBookClick: (String) -> Unit,
    onAddJournalClick: () -> Unit,
    onJournalClick: (String) -> Unit,
    viewModel: ProgressViewModel = viewModel()
) {
    // Escuchamos el flujo de estado del ViewModel
    val state by viewModel.uiState.collectAsState()

    // Recarga los datos de progreso cada vez que el componente entra en la composición (se visita la pestaña)
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

            // Título de la sección con tipografía corporativa
            Text(
                text = stringResource(R.string.progress_title),
                fontFamily = GuardianCity,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTituloTopBooks,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.isLoading) {
                // Estado de carga centralizado
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ColorArcMediumBrown)
                }
            } else {
                // Lista vertical que contiene las diferentes secciones horizontales
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Sección: Diarios de Lectura
                    item {
                        ProgressSection(
                            title = stringResource(R.string.progress_section_journals),
                            books = state.journals,
                            onSeeAllClick = { onNavigateToList("journals") },
                            onBookClick = onJournalClick,
                            onAddClick = onAddJournalClick // Esta sección permite añadir contenido nuevo
                        )
                    }

                    // Sección: Favoritos
                    item {
                        ProgressSection(
                            title = stringResource(R.string.progress_section_favorites),
                            books = state.favorites,
                            onSeeAllClick = { onNavigateToList("favorites") },
                            onBookClick = onBookClick
                        )
                    }

                    // Sección: Pendientes
                    item {
                        ProgressSection(
                            title = stringResource(R.string.progress_section_pending),
                            books = state.pending,
                            onSeeAllClick = { onNavigateToList("pending") },
                            onBookClick = onBookClick
                        )
                    }

                    // Sección: Leídos
                    item {
                        ProgressSection(
                            title = stringResource(R.string.progress_section_read),
                            books = state.read,
                            onSeeAllClick = { onNavigateToList("read") },
                            onBookClick = onBookClick
                        )
                    }

                    // Espacio de seguridad final para evitar solapamiento con la barra de navegación
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

/**
 * Componente genérico para representar una fila de libros bajo un título. (Stateless)
 * * @param title Nombre de la sección.
 * @param books Lista de libros a mostrar.
 * @param onSeeAllClick Acción para ver la lista completa.
 * @param onBookClick Acción al pulsar un libro.
 * @param onAddClick Si se provee, muestra un botón con el icono "+" para añadir elementos.
 */
@Composable
fun ProgressSection(
    title: String,
    books: List<SimpleBook>,
    onSeeAllClick: () -> Unit,
    onBookClick: (String) -> Unit,
    onAddClick: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Cabecera de la sección
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
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

            // Botón opcional de creación
            if (onAddClick != null) {
                IconButton(onClick = onAddClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.progress_desc_add), tint = ColorArcDarkBrown)
                }
            }

            // Botón de navegación a la lista completa
            IconButton(onClick = onSeeAllClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.progress_desc_see_all), tint = ColorArcDarkBrown)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Gestión de estado vacío para la sección
        if (books.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(130.dp)
                    .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.progress_empty_message),
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontFamily = CenturyGotic
                )
            }
        } else {
            // Carrusel horizontal de libros
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

/**
 * Representación visual compacta de un libro para la pantalla de progreso.
 * Muestra la portada con bordes redondeados y el título centrado debajo.
 */
@Composable
fun ProgressBookItem(book: SimpleBook, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(90.dp)
            .clickable { onClick() },
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