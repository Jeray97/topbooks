package com.example.topbooks.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel // Importante para inyectar el ViewModel
import com.example.topbooks.R
import com.example.topbooks.data.model.Book
import com.example.topbooks.ui.components.BookItem // Tu componente de la carta con foto
import com.example.topbooks.ui.theme.*
import com.example.topbooks.utils.Resource

@Composable
fun HomeScreen(
    // Inyectamos el ViewModel automáticamente
    viewModel: HomeViewModel = viewModel(),
    onCategoryClick: (String, String) -> Unit
) {
    // 1. Observamos (escuchamos) los 2 canales de datos
    val recommendedState by viewModel.recommendedBooks.collectAsState()
    val friendsState by viewModel.friendsBooks.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackGroundGeneral)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // HEADER
        Text(
            text = stringResource(id = R.string.welcome_title),
            fontFamily = GuardianCity,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = ColorTituloTopBooks,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        SearchBarCustom()

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECCIÓN 1: CATEGORÍAS ---
        SectionContainer(
            title = stringResource(id = R.string.section_categories),
            backgroundColor = ColorBackGroundCategorySection
        ) {
            // Fila de botones redondos
            CategoryRow(
                onCategoryClick = onCategoryClick
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECCIÓN 2: RECOMENDADOS ---
        SectionContainer(
            title = stringResource(id = R.string.section_recommended),
            backgroundColor = ColorBackGroundRecommendedSection
        ) {
            BookListRow(resource = recommendedState)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECCIÓN 3: FAVORITOS AMIGOS ---
        SectionContainer(
            title = stringResource(id = R.string.section_friends_favorites),
            backgroundColor = ColorBackGroundFavoritesSection
        ) {
            BookListRow(resource = friendsState)
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

// --- COMPONENTE INTELIGENTE: Lista de Libros ---
// Decide qué pintar según el estado (Cargando, Error o Lista)
@Composable
fun BookListRow(
    resource: Resource<List<Book>>
) {
    when (resource) {
        is Resource.Loading -> {
            // Mientras carga, mostramos tus círculos blancos
            BookPlaceholderRow()
        }
        is Resource.Success -> {
            val books = resource.data
            if (books.isEmpty()) {
                Text("No se encontraron libros.", color = Color.White, fontSize = 12.sp)
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(books) { book ->
                        // Aquí usamos tu componente BookItem (el de la portada real)
                        BookItem(
                            book = book,
                            onClick = { /* Aquí navegaremos al detalle en la Fase 3 */ }
                        )
                    }
                }
            }
        }
        is Resource.Error -> {
            // CAMBIO: Ahora mostramos el mensaje real de la excepción para saber qué pasa
            Text(
                text = "Error: ${resource.exception.message}",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.padding(8.dp)
            )
        }
        else -> {} // Idle
    }
}

@Composable
fun SearchBarCustom() {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(stringResource(id = R.string.search_hint), color = Color.Gray) },
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .background(Color.White, RoundedCornerShape(8.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            trailingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.Gray)
            }
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Color.White, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.icon_codigodebarras),
                contentDescription = stringResource(id = R.string.desc_scan_icon),
                modifier = Modifier.size(24.dp),
                tint = Color.Gray
            )
        }
    }
}

@Composable
fun SectionContainer(
    title: String,
    backgroundColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontFamily = CenturyGotic,
                    color = Color.White,
                    fontSize = 20.sp
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(id = R.string.desc_arrow_forward),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun CategoryRow(
    onCategoryClick: (String, String) -> Unit // Callback para avisar al padre
) {
    // Definimos: (String Resource, Icono, Texto para la API)
    val categories = listOf(
        Triple(R.string.category_romance, R.drawable.cat_romance_icon, "romance"),
        Triple(R.string.category_mystery, R.drawable.cat_misterio_icon, "mystery"),
        Triple(R.string.category_horror, R.drawable.cat_horror_icon, "horror"),
        Triple(R.string.category_fantasy, R.drawable.cat_fantasia_icon, "fantasy")
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(categories) { (nameRes, iconResId, apiQuery) ->

            val categoryName = stringResource(id = nameRes)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                // Al hacer click, enviamos el 'apiQuery' (ej: "mystery")
                modifier = Modifier.clickable { onCategoryClick(categoryName, apiQuery) }
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconResId),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = categoryName,
                        fontSize = 12.sp,
                        color = ColorBackGroundCategorySection,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BookPlaceholderRow() {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(5) {
            Box(
                modifier = Modifier
                    .size(90.dp) // Tamaño similar al de las cartas
                    .height(130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.5f))
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    // Nota: La preview puede fallar si intenta inyectar el ViewModel real.
    // Para previews complejas, se suele crear un ViewModel falso, pero
    // por ahora puedes comentar los parámetros del HomeScreen para ver el diseño básico.
    MaterialTheme {
        HomeScreen(
            onCategoryClick = { _, _ -> }
        )
    }
}