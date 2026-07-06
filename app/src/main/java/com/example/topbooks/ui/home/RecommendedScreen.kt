package com.example.topbooks.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.topbooks.R
import com.example.topbooks.data.model.Book
import com.example.topbooks.ui.theme.*
import com.example.topbooks.utils.Resource

/**
 * PANTALLA DE RECOMENDADOS (Stateful Composable).
 * Gestiona la carga de datos y la observación de estados para la vista de recomendaciones.
 * * @param onBackClick Acción para regresar a la pantalla anterior.
 * @param onBookClick Acción al seleccionar un libro específico.
 * @param onSectionClick Acción al pulsar en la cabecera de una sección para ver más resultados.
 * @param viewModel ViewModel que provee los flujos de datos para populares, gustos y amigos.
 */
@Composable
fun RecommendedScreen(
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    onSectionClick: (String, String, Int) -> Unit,
    viewModel: RecommendedViewModel = viewModel()
) {
    // Observamos los estados reactivos del ViewModel
    val popularState by viewModel.popularBooks.collectAsState()
    val tastesState by viewModel.tastesBooks.collectAsState()
    val friendsState by viewModel.friendsBooks.collectAsState()

    // Internacionalización: Obtenemos los términos de búsqueda según el idioma actual
    val genreUsed = stringResource(R.string.recommended_default_genre)
    val popularQuery = stringResource(R.string.query_popular_bestsellers)

    // Disparamos la carga inicial al entrar en la pantalla con los parámetros traducidos
    LaunchedEffect(Unit) {
        viewModel.loadData(popularQuery, genreUsed)
    }

    // Delegamos la renderización al componente visual (Stateless)
    RecommendedContent(
        popularState = popularState,
        tastesState = tastesState,
        friendsState = friendsState,
        genreForTastes = genreUsed,
        popularQuery = popularQuery,
        onBackClick = onBackClick,
        onBookClick = onBookClick,
        onSectionClick = onSectionClick
    )
}

/**
 * INTERFAZ VISUAL DE RECOMENDADOS (Stateless Composable).
 * Define la estructura y el layout de la pantalla sin manejar estados de negocio.
 */
@Composable
fun RecommendedContent(
    popularState: Resource<List<Book>>,
    tastesState: Resource<List<Book>>,
    friendsState: Resource<List<Book>>,
    genreForTastes: String,
    popularQuery: String,
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    onSectionClick: (String, String, Int) -> Unit
) {
    Scaffold(
        containerColor = LoginColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = LoginColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    fontFamily = GuardianCity,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = LoginColors.Primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.recommended_title),
                fontFamily = GuardianCity,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = LoginColors.Primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Descubre nuevas lecturas",
                fontSize = 16.sp,
                color = LoginColors.OnSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // --- 1. SECCIÓN: LIBROS POPULARES ---
            BrownCardSection(
                title = stringResource(R.string.recommended_section_popular),
                resource = popularState,
                onBookClick = onBookClick,
                backgroundColor = LoginColors.SurfaceContainerHigh,
                onArrowClick = {
                    onSectionClick("popular", popularQuery, LoginColors.SurfaceContainerHigh.toArgb())
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- 2. SECCIÓN: SEGÚN TUS GUSTOS ---
            BrownCardSection(
                title = stringResource(R.string.recommended_section_tastes, genreForTastes),
                resource = tastesState,
                onBookClick = onBookClick,
                backgroundColor = LoginColors.SurfaceContainerHigh,
                onArrowClick = {
                    onSectionClick("tastes", genreForTastes, LoginColors.SurfaceContainerHigh.toArgb())
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- 3. SECCIÓN: FAVORITOS DE AMIGOS ---
            BrownCardSection(
                title = stringResource(R.string.recommended_section_friends),
                resource = friendsState,
                onBookClick = onBookClick,
                isEmptyMessage = stringResource(R.string.recommended_friends_empty),
                backgroundColor = LoginColors.SurfaceContainerHigh,
                onArrowClick = {
                    onSectionClick("friends", "General", LoginColors.SurfaceContainerHigh.toArgb())
                }
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

/**
 * COMPONENTE DE SECCIÓN ESTILIZADO.
 * Contenedor genérico para listas de libros con gestión automática de estados (Resource).
 * * @param title Título de la tarjeta.
 * @param resource Estado de los datos (Loading, Success, Error).
 * @param onBookClick Callback al pulsar un libro.
 * @param isEmptyMessage Texto a mostrar si la lista está vacía.
 * @param backgroundColor Color de fondo de la tarjeta.
 * @param onArrowClick Acción al pulsar la cabecera.
 */
@Composable
fun BrownCardSection(
    title: String,
    resource: Resource<List<Book>>,
    onBookClick: (String) -> Unit,
    isEmptyMessage: String = stringResource(R.string.recommended_empty_default),
    backgroundColor: Color = LoginColors.SurfaceContainerHigh,
    onArrowClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 20.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clickable { onArrowClick() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontFamily = GuardianCity,
                    fontSize = 20.sp,
                    color = LoginColors.OnSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = LoginColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            when (resource) {
                is Resource.Loading -> {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        items(3) {
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LoginColors.OutlineVariant.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
                is Resource.Success -> {
                    if (resource.data.isEmpty()) {
                        Text(
                            text = isEmptyMessage,
                            color = LoginColors.OnSurfaceVariant,
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp)
                        ) {
                            items(resource.data) { book ->
                                WhiteBookItem(book = book, onClick = { onBookClick(book.id) })
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    Text(
                        text = stringResource(R.string.recommended_error_loading),
                        color = LoginColors.OnSurfaceVariant,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
                else -> {}
            }
        }
    }
}

/**
 * ELEMENTO DE LIBRO ESPECIALIZADO PARA RECOMENDADOS.
 * Tarjeta blanca minimalista para resaltar las portadas de los libros.
 * Incluye lógica para mostrar el título si la imagen no está disponible.
 */
@Composable
fun WhiteBookItem(book: Book, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .height(180.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = LoginColors.Surface),
        elevation = CardDefaults.cardElevation(4.dp),
        onClick = onClick
    ) {
        if (book.imageUrl.isNotEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(book.imageUrl)
                    .crossfade(true)
                    .error(R.drawable.icon_codigodebarras)
                    .build(),
                contentDescription = book.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = book.title,
                    color = LoginColors.Primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    lineHeight = 14.sp
                )
            }
        }
    }
}