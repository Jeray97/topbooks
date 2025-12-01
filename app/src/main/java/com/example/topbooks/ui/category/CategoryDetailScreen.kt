package com.example.topbooks.ui.category

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.topbooks.data.model.Book
import com.example.topbooks.ui.components.BookItem
import com.example.topbooks.ui.theme.*
import com.example.topbooks.utils.Resource
import com.example.topbooks.ui.components.CategoryDetailContentBackgroundShape
import com.example.topbooks.ui.components.SearchBarCustom

// --- 1. LÓGICA (ViewModel) ---
@Composable
fun CategoryDetailScreen(
    categoryName: String,
    query: String,
    onBackClick: () -> Unit,
    viewModel: CategoryDetailViewModel = viewModel()
) {
    LaunchedEffect(key1 = query) {
        viewModel.fetchBooksByCategory(query)
    }

    val state by viewModel.booksState.collectAsState()

    CategoryDetailContent(
        categoryName = categoryName,
        state = state,
        onBackClick = onBackClick
    )
}

// --- 2. DISEÑO ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailContent(
    categoryName: String,
    state: Resource<List<Book>>,
    onBackClick: () -> Unit
) {
    CategoryDetailContentBackgroundShape {
        Scaffold(
            // 1. Usamos color de fondo general
            containerColor = Color.Transparent,

            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = categoryName,
                            // 2. Usamos fuente y color de texto
                            fontFamily = GuardianCity, // O CenturyGotic, la que prefieras de título
                            color = ColorTextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = ColorTextPrimary // La flecha del color de tu texto
                            )
                        }
                    },
                    // Hacemos la barra transparente para que se vea el fondo general
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues) // Respetamos el espacio de la TopBar
                    .padding(horizontal = 16.dp) // Margen lateral general
            ) {

                SearchBarCustom()

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    when (state) {
                        is Resource.Loading -> {
                            // Indicador de carga blanco o de tu color primario
                            CircularProgressIndicator(color = ColorTextPrimary)
                        }

                        is Resource.Success -> {
                            val books = state.data
                            if (books.isEmpty()) {
                                Text(
                                    "No hay libros disponibles.",
                                    color = ColorTextPrimary,
                                    fontFamily = CenturyGotic
                                )
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    contentPadding = PaddingValues(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(books) { book ->
                                        // Aquí usa tu BookItem, que ya tiene su propio diseño
                                        BookItem(
                                            book = book,
                                            onClick = { /* TODO Navegar a detalle */ }
                                        )
                                    }
                                }
                            }
                        }

                        is Resource.Error -> {
                            Text(
                                text = "Error: ${state.exception.message}",
                                color = Color.Red,
                                fontFamily = CenturyGotic
                            )
                        }

                        else -> {}
                    }
                }
            }
        }
    }
}

// --- 3. PREVIEW ---
@Preview(showBackground = true)
@Composable
fun CategoryDetailScreenPreview() {
    val librosFalsos = listOf(
        Book("1", "Libro 1", listOf("Autor"), "Desc", "", "2025"),
        Book("2", "Libro 2", listOf("Autor"), "Desc", "", "2025"),
        Book("3", "Libro 3", listOf("Autor"), "Desc", "", "2025"),
        Book("4", "Libro 4", listOf("Autor"), "Desc", "", "2025")
    )

    MaterialTheme {
        CategoryDetailContent(
            categoryName = "Romance",
            state = Resource.Success(librosFalsos),
            onBackClick = {}
        )
    }
}