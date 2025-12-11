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
import com.example.topbooks.ui.components.TopBar

@Composable
fun CategoryDetailScreen(
    categoryName: String,
    query: String,
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    onScanClick: () -> Unit, // <--- NUEVO
    viewModel: CategoryDetailViewModel = viewModel()
) {
    LaunchedEffect(key1 = query) {
        viewModel.fetchBooksByCategory(query)
    }
    val state by viewModel.booksState.collectAsState()

    CategoryDetailContent(
        categoryName = categoryName,
        state = state,
        onBackClick = onBackClick,
        onBookClick = onBookClick,
        onScanClick = onScanClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailContent(
    categoryName: String,
    state: Resource<List<Book>>,
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    onScanClick: () -> Unit
) {
    CategoryDetailContentBackgroundShape {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { TopBar(onBackClick) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(26.dp))

                // Pasamos el evento
                SearchBarCustom(onBookClick = onBookClick, onScanClick = onScanClick)

                Spacer(modifier = Modifier.height(40.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(categoryName,
                        color = ColorTituloCategoriaDetalle,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center,
                        fontFamily = CenturyGotic,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (state) {
                        is Resource.Loading -> CircularProgressIndicator(color = ColorTextPrimary)
                        is Resource.Success -> {
                            val books = state.data
                            if (books.isEmpty()) {
                                Text("No hay libros disponibles.", color = ColorTextPrimary, fontFamily = CenturyGotic)
                            } else {
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
                        is Resource.Error -> Text(text = "Error: ${state.exception.message}", color = Color.Red, fontFamily = CenturyGotic)
                        else -> {}
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CategoryDetailScreenPreview() {
    val librosFalsos = listOf(
        Book("1", "Libro 1", listOf("Autor"), "Desc", "", "2025"),
        Book("2", "Libro 2", listOf("Autor"), "Desc", "", "2025")
    )
    MaterialTheme {
        CategoryDetailContent(
            categoryName = "Romance",
            state = Resource.Success(librosFalsos),
            onBackClick = {},
            onBookClick = {},
            onScanClick = {}
        )
    }
}