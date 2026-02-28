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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.topbooks.R
import com.example.topbooks.data.model.Book
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.ui.theme.*
import com.example.topbooks.utils.Resource

@Composable
fun RecommendedScreen(
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    onSectionClick: (String, String, Int) -> Unit, // Callback para navegar
    viewModel: RecommendedViewModel = viewModel()
) {
    val popularState by viewModel.popularBooks.collectAsState()
    val tastesState by viewModel.tastesBooks.collectAsState()
    val friendsState by viewModel.friendsBooks.collectAsState()

    // Usamos el género por defecto que tienes configurado en el ViewModel
    val genreUsed = stringResource(R.string.recommended_default_genre)

    RecommendedContent(
        popularState = popularState,
        tastesState = tastesState,
        friendsState = friendsState,
        genreForTastes = genreUsed,
        onBackClick = onBackClick,
        onBookClick = onBookClick,
        onSectionClick = onSectionClick
    )
}

@Composable
fun RecommendedContent(
    popularState: Resource<List<Book>>,
    tastesState: Resource<List<Book>>,
    friendsState: Resource<List<Book>>,
    genreForTastes: String,
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    onSectionClick: (String, String, Int) -> Unit
) {
    Scaffold(
        containerColor = ColorBackGroundGeneral,
        topBar = { TopBar(onBackClick = onBackClick) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.recommended_title),
                fontFamily = CenturyGotic,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTituloTopBooks,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 1. POPULARES
            BrownCardSection(
                title = stringResource(R.string.recommended_section_popular),
                resource = popularState,
                onBookClick = onBookClick,
                backgroundColor = ColorBackGroundCategorySection,
                onArrowClick = {
                    onSectionClick("popular", "General", ColorBackGroundCategorySection.toArgb())
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. GUSTOS
            BrownCardSection(
                title = stringResource(R.string.recommended_section_tastes, genreForTastes),
                resource = tastesState,
                onBookClick = onBookClick,
                backgroundColor = ColorBackGroundRecommendedSection,
                onArrowClick = {
                    onSectionClick("tastes", genreForTastes, ColorBackGroundRecommendedSection.toArgb())
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3. AMIGOS
            BrownCardSection(
                title = stringResource(R.string.recommended_section_friends),
                resource = friendsState,
                onBookClick = onBookClick,
                isEmptyMessage = stringResource(R.string.recommended_friends_empty),
                backgroundColor = ColorBackGroundFavoritesSection,
                onArrowClick = {
                    onSectionClick("friends", "General", ColorBackGroundFavoritesSection.toArgb())
                }
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun BrownCardSection(
    title: String,
    resource: Resource<List<Book>>,
    onBookClick: (String) -> Unit,
    isEmptyMessage: String = stringResource(R.string.recommended_empty_default),
    backgroundColor: Color = ColorArcDarkBrown,
    onArrowClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            // Cabecera clicable
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { onArrowClick() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontFamily = CenturyGotic,
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            when (resource) {
                is Resource.Loading -> {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(3) {
                            Box(
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                            )
                        }
                    }
                }
                is Resource.Success -> {
                    if (resource.data.isEmpty()) {
                        Text(
                            text = isEmptyMessage,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
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
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
fun WhiteBookItem(book: Book, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(150.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    color = ColorArcDarkBrown,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
fun RecommendedScreenPreview() {
    val dummyBooks = listOf(
        Book("1", "El Nombre del Viento", listOf("P. Rothfuss"), "", "", "2007"),
        Book("2", "Dune", listOf("F. Herbert"), "", "", "1965")
    )

    RecommendedContent(
        popularState = Resource.Success(dummyBooks),
        tastesState = Resource.Success(dummyBooks),
        friendsState = Resource.Success(emptyList()),
        genreForTastes = "Fantasía Épica",
        onBackClick = {},
        onBookClick = {},
        onSectionClick = { _, _, _ -> }
    )
}