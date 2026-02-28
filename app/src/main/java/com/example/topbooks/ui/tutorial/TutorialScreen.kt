package com.example.topbooks.ui.tutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.topbooks.R
import com.example.topbooks.ui.theme.*
import com.example.topbooks.utils.CategoryProvider
import kotlinx.coroutines.launch

@Composable
fun TutorialScreen(
    viewModel: TutorialViewModel = viewModel(),
    onFinished: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Scaffold(containerColor = ColorBackGroundGeneral) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Barra de progreso superior
            LinearProgressIndicator(
                progress = { (pagerState.currentPage + 1) / 3f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = ColorArcMediumBrown,
                trackColor = Color.LightGray.copy(alpha = 0.3f)
            )

            // Contenido Paginado
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = false // Bloqueamos swipe manual para forzar botones
            ) { page ->
                when (page) {
                    0 -> WelcomePage()
                    1 -> GenresPage(
                        selected = uiState.selectedGenres,
                        // 🔥 Usamos los géneros del provider si no están en el ViewModel
                        available = CategoryProvider.allCategories,
                        onGenreClick = { viewModel.toggleGenre(it) }
                    )
                    2 -> BooksPage(
                        books = uiState.suggestedBooks,
                        selectedIds = uiState.selectedBookIds,
                        isLoading = uiState.isLoadingBooks,
                        onBookClick = { viewModel.toggleBookSelection(it) }
                    )
                }
            }

            // Barra de navegación inferior
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón Atrás (Invisible en la primera página)
                if (pagerState.currentPage > 0) {
                    TextButton(onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    }) {
                        Text(stringResource(R.string.tutorial_btn_back), color = ColorArcDarkBrown, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Spacer(modifier = Modifier.width(60.dp))
                }

                // Botón Siguiente / Comenzar
                Button(
                    onClick = {
                        if (pagerState.currentPage < 2) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            viewModel.finishOnboarding(onFinished)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorArcMediumBrown),
                    shape = RoundedCornerShape(12.dp),
                    // Deshabilitar si está en pág. 2 y no eligió géneros o si está guardando
                    enabled = (pagerState.currentPage != 1 || uiState.selectedGenres.isNotEmpty()) && !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(if (pagerState.currentPage < 2) stringResource(R.string.tutorial_btn_next) else stringResource(R.string.tutorial_btn_start))
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomePage() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(140.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Star, null, Modifier.size(80.dp), ColorArcMediumBrown)
            }
        }
        Spacer(Modifier.height(32.dp))
        Text(
            stringResource(R.string.tutorial_welcome_title),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = ColorArcDarkBrown,
            textAlign = TextAlign.Center,
            fontFamily = GuardianCity
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.tutorial_welcome_subtitle),
            textAlign = TextAlign.Center,
            color = Color.Gray,
            fontFamily = CenturyGotic,
            lineHeight = 22.sp
        )
    }
}

@Composable
fun GenresPage(selected: Set<String>, available: List<String>, onGenreClick: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.tutorial_genres_title), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ColorArcDarkBrown, fontFamily = GuardianCity)
        Text(stringResource(R.string.tutorial_genres_subtitle), fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))

        // Grid ajustado
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f) // Ocupa el espacio disponible central
        ) {
            items(available) { genre ->
                val isSel = selected.contains(genre)
                // 🔥 OBTENEMOS DATOS DEL PROVIDER
                val catData = CategoryProvider.getCategoryResources(genre)
                val displayName = if (catData.nameRes != null) stringResource(id = catData.nameRes) else CategoryProvider.formatFallbackName(genre)

                Surface(
                    onClick = { onGenreClick(genre) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSel) ColorArcMediumBrown else Color.White,
                    modifier = Modifier.height(50.dp),
                    shadowElevation = if(isSel) 4.dp else 1.dp,
                    border = if(!isSel) androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(0.4f)) else null
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = displayName,
                            color = if (isSel) Color.White else ColorArcDarkBrown,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BooksPage(
    books: List<com.example.topbooks.data.model.Book>,
    selectedIds: Set<String>,
    isLoading: Boolean,
    onBookClick: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text(stringResource(R.string.tutorial_books_title), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ColorArcDarkBrown, fontFamily = GuardianCity)
        Text(stringResource(R.string.tutorial_books_subtitle), fontSize = 14.sp, color = Color.Gray)
        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = ColorArcMediumBrown) }
        } else if (books.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { Text(stringResource(R.string.tutorial_books_empty), color = Color.Gray) }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(books) { book ->
                    val isSel = selectedIds.contains(book.id)
                    Column(
                        modifier = Modifier
                            .clickable { onBookClick(book.id) }
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box {
                            AsyncImage(
                                model = book.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .height(110.dp)
                                    .width(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(if (isSel) 3.dp else 0.dp, ColorArcMediumBrown, RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            if (isSel) {
                                Icon(
                                    Icons.Default.Check, null,
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .background(ColorArcMediumBrown, CircleShape)
                                        .padding(2.dp)
                                        .size(12.dp),
                                    Color.White
                                )
                            }
                        }
                        Text(
                            book.title,
                            fontSize = 11.sp,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp),
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }
    }
}