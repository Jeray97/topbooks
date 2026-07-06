package com.example.topbooks.ui.shelf

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.topbooks.ui.theme.CenturyGotic
import com.example.topbooks.ui.theme.ColorArcDarkBrown
import com.example.topbooks.ui.theme.ColorArcMediumBrown
import com.example.topbooks.ui.theme.ColorBackGroundGeneral
import com.example.topbooks.ui.theme.GuardianCity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendShelvesScreen(
    friendId: String,
    friendName: String,
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    viewModel: FriendShelvesViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(friendId) {
        viewModel.loadFriendShelves(friendId)
    }

    Scaffold(
        containerColor = ColorBackGroundGeneral,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Estanterías de",
                            fontFamily = CenturyGotic,
                            fontSize = 14.sp,
                            color = ColorArcDarkBrown
                        )
                        Text(
                            text = friendName,
                            fontFamily = GuardianCity,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorArcDarkBrown
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = ColorArcDarkBrown)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorBackGroundGeneral)
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ColorArcMediumBrown)
            }
        } else if (state.shelves.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = ColorArcMediumBrown,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Sin estanterías públicas",
                        fontFamily = GuardianCity,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorArcDarkBrown
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "$friendName no tiene estanterías públicas",
                        fontFamily = CenturyGotic,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
            ) {
                items(state.shelves.size) { index ->
                    val shelf = state.shelves[index]
                    val books = state.shelfBooks[shelf.id] ?: emptyList()
                    val totalPages = books.sumOf { it.pageCount }

                    var visible by remember { mutableStateOf(false) }
                    val alpha by animateFloatAsState(
                        targetValue = if (visible) 1f else 0f,
                        animationSpec = tween(durationMillis = 400, delayMillis = index * 100),
                        label = "shelfAlpha"
                    )
                    val offsetY by animateFloatAsState(
                        targetValue = if (visible) 0f else 60f,
                        animationSpec = tween(durationMillis = 400, delayMillis = index * 100),
                        label = "shelfOffset"
                    )
                    LaunchedEffect(Unit) { visible = true }

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                this.alpha = alpha
                                this.translationY = offsetY
                            }
                    ) {
                        FriendShelfRow(
                            shelfName = shelf.name,
                            books = books,
                            totalPages = totalPages,
                            onBookClick = onBookClick
                        )
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun FriendShelfRow(
    shelfName: String,
    books: List<ShelfBook>,
    totalPages: Int,
    onBookClick: (String) -> Unit
) {
    var previewBook by remember { mutableStateOf<ShelfBook?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Public,
                contentDescription = "Pública",
                tint = ColorArcMediumBrown,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.size(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shelfName,
                    fontFamily = GuardianCity,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorArcDarkBrown
                )
                Row {
                    Text(
                        text = "${books.size} ${if (books.size == 1) "libro" else "libros"}",
                        fontFamily = CenturyGotic,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    if (totalPages > 0) {
                        Text(
                            text = " · $totalPages págs",
                            fontFamily = CenturyGotic,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        WoodenShelf(
            books = books,
            viewMode = ViewMode.SPINES,
            perspectiveMode = PerspectiveMode.ORTHO,
            recentlyAddedBookId = null,
            draggingBook = null,
            onBookTap = { book -> previewBook = book },
            onRemoveBook = {},
            onDragStart = {},
            onDragEnd = {}
        )
    }

    if (previewBook != null) {
        BookCoverPopup(
            book = previewBook!!,
            onDismiss = { previewBook = null },
            onNavigate = {
                onBookClick(previewBook!!.id)
                previewBook = null
            }
        )
    }
}
