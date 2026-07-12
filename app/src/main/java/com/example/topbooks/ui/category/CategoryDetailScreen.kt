package com.example.topbooks.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.topbooks.data.model.Book
import com.example.topbooks.ui.components.BookItem
import com.example.topbooks.ui.theme.*
import com.example.topbooks.utils.Resource
import com.example.topbooks.ui.components.SearchBarCustom

@Composable
fun CategoryDetailScreen(
    categoryName: String,
    query: String,
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    onScanClick: () -> Unit,
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = 280f
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent
        ) { paddingValues ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TopBooks",
                        fontFamily = GuardianCity,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                SearchBarCustom(onBookClick = onBookClick, onScanClick = onScanClick)

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = categoryName,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 32.sp,
                    fontFamily = GuardianCity,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Descubre los mejores libros de esta categoría",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (state) {
                        is Resource.Loading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)

                        is Resource.Success -> {
                            val books = state.data

                            if (books.isEmpty()) {
                                Text(
                                    "No hay libros disponibles.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 16.sp
                                )
                            } else {
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

                        is Resource.Error -> Text(
                            text = "Error: ${state.exception.message}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp
                        )

                        else -> {}
                    }
                }
            }
        }
    }
}