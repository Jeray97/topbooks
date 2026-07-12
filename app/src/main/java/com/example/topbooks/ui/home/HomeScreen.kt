package com.example.topbooks.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.topbooks.R
import com.example.topbooks.data.model.Book
import com.example.topbooks.ui.components.BookItem
import com.example.topbooks.ui.components.SearchBarCustom
import com.example.topbooks.ui.theme.*
import com.example.topbooks.utils.CategoryProvider
import com.example.topbooks.utils.Resource

/**
 * PANTALLA DE INICIO (HOME).
 * Es el centro de navegación de la aplicación donde se presentan:
 * 1. Un buscador con escáner de barras.
 * 2. Un carrusel de categorías rápidas.
 * 3. Una sección de recomendaciones personalizadas basadas en IA/Algoritmo.
 * 4. Un feed de lo que están leyendo o marcando como favorito los amigos.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onCategoryClick: (String, String) -> Unit, // (Nombre UI, Query API)
    onBookClick: (String) -> Unit,
    onScanClick: () -> Unit,
    onSeeAllCategoriesClick: () -> Unit,
    onRecommendedClick: () -> Unit,
    onFriendsActivityClick: () -> Unit
) {
    // Observamos los flujos de datos del ViewModel
    val recommendedState by viewModel.recommendedBooks.collectAsState()
    val friendsState by viewModel.friendsBooks.collectAsState()

    // Internacionalización: Obtenemos los términos de búsqueda dinámicamente según el idioma del dispositivo
    val categoryQuery = stringResource(R.string.query_best_fiction)
    val fantasyData = CategoryProvider.getCategoryResources("FANTASY")
    val recommendedQuery = if (fantasyData.nameRes != null) stringResource(fantasyData.nameRes) else "Fantasía"

    // Disparamos la carga de datos al entrar en la pantalla
    LaunchedEffect(Unit) {
        viewModel.loadData(categoryQuery, recommendedQuery)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
                // Permitimos scroll vertical para dispositivos con pantallas pequeñas
                .verticalScroll(rememberScrollState())
        ) {
            // Cabecera con tipografía corporativa
            Text(
                text = stringResource(id = R.string.app_name),
                fontFamily = GuardianCity,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Componente de búsqueda que flota sobre el contenido
            SearchBarCustom(
                onBookClick = onBookClick,
                onScanClick = onScanClick
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- 1. SECCIÓN CATEGORÍAS ---
            SectionContainer(
                title = stringResource(id = R.string.section_categories),
                backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow,
                onArrowClick = onSeeAllCategoriesClick
            ) {
                CategoryRow(onCategoryClick = onCategoryClick)
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 2. SECCIÓN RECOMENDADOS ---
            SectionContainer(
                title = stringResource(id = R.string.section_recommended),
                backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow,
                onArrowClick = onRecommendedClick
            ) {
                BookListRow(resource = recommendedState, onBookClick = onBookClick)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 3. SECCIÓN FAVORITOS DE AMIGOS ---
            SectionContainer(
                title = stringResource(id = R.string.section_friends_favorites),
                backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow,
                onArrowClick = onFriendsActivityClick
            ) {
                when (val state = friendsState) {
                    is Resource.Success -> {
                        if (state.data.isEmpty()) {
                            EmptyFriendsMessage() // Estado vacío (Placeholder con icono social)
                        } else {
                            val books = state.data.map { it.book }
                            BookListRowContent(books = books, onBookClick = onBookClick)
                        }
                    }
                    is Resource.Loading -> {
                        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                    is Resource.Error -> {
                        EmptyFriendsMessage()
                    }
                    else -> {}
                }
            }

            // Espaciado extra para que el contenido no quede debajo del BottomNav
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

/**
 * Componente Placeholder que se muestra cuando el usuario aún no tiene amigos o estos no tienen actividad.
 */
@Composable
fun EmptyFriendsMessage() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.social),
            contentDescription = null,
            modifier = Modifier.size(60.dp).padding(bottom = 8.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        )
        Text(
            text = stringResource(id = R.string.home_friends_empty_title),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = CenturyGotic
        )
        Text(
            text = stringResource(id = R.string.home_friends_empty_desc),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontSize = 10.sp,
            fontFamily = CenturyGotic
        )
    }
}

/**
 * Renderiza una fila horizontal de libros. (Stateless)
 */
@Composable
fun BookListRowContent(books: List<Book>, onBookClick: (String) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(books) { book ->
            BookItem(book = book, onClick = { onBookClick(book.id) })
        }
    }
}

/**
 * Gestiona el estado de carga y error para las filas de libros.
 */
@Composable
fun BookListRow(
    resource: Resource<List<Book>>,
    onBookClick: (String) -> Unit
) {
    when (resource) {
        is Resource.Loading -> BookPlaceholderRow()
        is Resource.Success -> {
            if (resource.data.isEmpty()) {
                Text(stringResource(id = R.string.home_books_empty), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
            } else {
                BookListRowContent(resource.data, onBookClick)
            }
        }
        is Resource.Error -> Text(stringResource(id = R.string.home_books_error), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
        else -> {}
    }
}

/**
 * Contenedor estilizado con tarjeta redondeada que agrupa una sección de la Home.
 * @param title Título de la sección.
 * @param backgroundColor Color de fondo de la tarjeta.
 * @param onArrowClick Si se provee, muestra una flecha de navegación a la derecha.
 * @param content El contenido Composable que irá dentro de la sección.
 */
@Composable
fun SectionContainer(
    title: String,
    backgroundColor: Color,
    onArrowClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontFamily = GuardianCity,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(
                    onClick = { onArrowClick?.invoke() },
                    enabled = onArrowClick != null
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = if (onArrowClick != null) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            content()
        }
    }
}

/**
 * Fila horizontal que muestra los accesos directos a las categorías principales.
 * Utiliza Triples para agrupar (NombreRes, IconoRes, QueryRes).
 */
@Composable
fun CategoryRow(
    onCategoryClick: (String, String) -> Unit
) {
    val categories = listOf(
        Triple(R.string.cat_romance_text, R.drawable.cat_romance_icon, R.string.cat_romance_text),
        Triple(R.string.cat_misterio_text, R.drawable.cat_misterio_icon, R.string.cat_misterio_text),
        Triple(R.string.cat_horror_text, R.drawable.cat_horror_icon, R.string.cat_horror_text),
        Triple(R.string.cat_fantasia_text, R.drawable.cat_fantasia_icon, R.string.cat_fantasia_text)
    )

    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(categories) { (nameRes, iconResId, queryRes) ->
            val categoryName = stringResource(id = nameRes)
            val apiQuery = stringResource(id = queryRes)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onCategoryClick(categoryName, apiQuery) }
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconResId),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(9999.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = categoryName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Skeleton loading visual para dar feedback inmediato al usuario mientras se descargan los libros.
 */
@Composable
fun BookPlaceholderRow() {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(5) {
            Box(
                modifier = Modifier
                    .size(140.dp, 210.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            )
        }
    }
}