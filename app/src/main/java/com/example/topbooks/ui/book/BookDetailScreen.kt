package com.example.topbooks.ui.book

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.model.Review
import com.example.topbooks.ui.components.TopBar // TU TOPBAR
import com.example.topbooks.ui.theme.*
import com.example.topbooks.utils.AvatarHelper
import kotlinx.coroutines.launch

@Composable
fun BookDetailScreen(
    bookId: String,
    onBackClick: () -> Unit,
    viewModel: BookDetailViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var isFabExpanded by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }

    // Estado para el diálogo de confirmación de borrado
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(bookId) {
        viewModel.getBook(bookId)
    }

    // DIÁLOGO: Escribir Reseña
    if (showReviewDialog && state.book != null) {
        ReviewDialog(
            onDismiss = { showReviewDialog = false },
            onSubmit = { rating, text ->
                viewModel.saveReview(state.book!!, rating, text) {
                    showReviewDialog = false
                }
            }
        )
    }

    // DIÁLOGO: Confirmar Borrado
    if (showDeleteDialog && state.book != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Eliminar de tu biblioteca?") },
            text = { Text("Este libro se eliminará de tu lista '${state.savedInList}'.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeFromList(state.book!!.id)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            },
            containerColor = Color.White
        )
    }

    Scaffold(
        containerColor = ColorBackGroundGeneral,
        topBar = {
            // USAMOS TU COMPONENTE PERSONALIZADO
            TopBar(onBackClick = onBackClick)
        },
        floatingActionButton = {
            if (state.book != null) {
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedVisibility(
                        visible = isFabExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        SmallFabItem(Icons.Default.Edit, "Escribir reseña") {
                            isFabExpanded = false
                            showReviewDialog = true
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    AnimatedVisibility(
                        visible = isFabExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        SmallFabItem(Icons.Default.Search, "Ver opiniones") {
                            isFabExpanded = false
                            coroutineScope.launch { listState.animateScrollToItem(2) }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    val rotation by animateFloatAsState(if (isFabExpanded) 45f else 0f, label = "fab")
                    FloatingActionButton(
                        onClick = { isFabExpanded = !isFabExpanded },
                        containerColor = ColorArcMediumBrown,
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Add, "Menú", modifier = Modifier.rotate(rotation))
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = ColorArcMediumBrown)
            } else if (state.error != null) {
                Text("Error: ${state.error}", color = Color.Red, modifier = Modifier.align(Alignment.Center))
            } else if (state.book != null) {
                val book = state.book!!

                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 100.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 1. HEADER (Portada, Título, Autor, Botonera Listas)
                    item {
                        BookHeaderSection(
                            book = book,
                            savedInList = state.savedInList, // Pasamos en qué lista está
                            onListAction = { targetList ->
                                if (state.savedInList == targetList) {
                                    // Si ya está en esa lista, preguntamos para borrar
                                    showDeleteDialog = true
                                } else {
                                    // Si no, lo movemos/añadimos a la nueva lista
                                    viewModel.addToList(book, targetList)
                                }
                            }
                        )
                    }

                    // 2. SINOPSIS
                    item { SynopsisSection(book.description) }

                    // 3. CABECERA RESEÑAS
                    item {
                        Text(
                            text = "Opiniones de la comunidad (${state.reviews.size})",
                            fontFamily = CenturyGotic,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorTituloTopBooks,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                        )
                    }

                    // 4. LISTA RESEÑAS
                    if (state.reviews.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Nadie ha opinado todavía. ¡Sé el primero!", color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            }
                        }
                    } else {
                        items(state.reviews) { review ->
                            ReviewItem(review)
                        }
                    }
                }
            }
        }
    }
}

// --- COMPONENTES UI ---

@Composable
fun BookHeaderSection(
    book: Book,
    savedInList: String?, // "Favoritos", "Leídos", "Pendientes" o null
    onListAction: (String) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(10.dp), modifier = Modifier.width(170.dp).height(260.dp)) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(book.imageUrl).crossfade(true).error(com.example.topbooks.R.drawable.icon_codigodebarras).build(),
                contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(book.title, fontSize = 24.sp, fontFamily = GuardianCity, fontWeight = FontWeight.Bold, color = ColorTituloTopBooks, textAlign = TextAlign.Center, lineHeight = 28.sp, modifier = Modifier.padding(horizontal = 24.dp))
        Text(book.authors.joinToString(", "), fontSize = 16.sp, fontFamily = CenturyGotic, color = ColorArcDarkBrown, modifier = Modifier.padding(top = 8.dp))

        Spacer(modifier = Modifier.height(24.dp))

        // --- BOTONERA DE ESTADO (3 Listas) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 1. Favoritos
            StatusButton(
                label = "Favoritos",
                isActive = savedInList == "Favoritos",
                activeIcon = Icons.Default.Favorite,
                inactiveIcon = Icons.Default.FavoriteBorder,
                onClick = { onListAction("Favoritos") }
            )

            // 2. Leídos
            StatusButton(
                label = "Leídos",
                isActive = savedInList == "Leídos",
                activeIcon = Icons.Default.CheckCircle,
                inactiveIcon = Icons.Outlined.CheckCircle,
                onClick = { onListAction("Leídos") }
            )

            // 3. Pendientes
            StatusButton(
                label = "Pendientes",
                isActive = savedInList == "Pendientes",
                activeIcon = Icons.Default.Info,
                inactiveIcon = Icons.Default.Call,
                onClick = { onListAction("Pendientes") }
            )
        }
    }
}

@Composable
fun StatusButton(
    label: String,
    isActive: Boolean,
    activeIcon: ImageVector,
    inactiveIcon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        // Círculo de fondo
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(
                    color = if (isActive) ColorArcMediumBrown else Color.White,
                    shape = CircleShape
                )
                .border(
                    width = if (isActive) 0.dp else 1.dp,
                    color = ColorArcMediumBrown,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isActive) activeIcon else inactiveIcon,
                contentDescription = label,
                tint = if (isActive) Color.White else ColorArcMediumBrown,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) ColorArcMediumBrown else Color.Gray
        )
    }
}

// ... Resto de componentes (SynopsisSection, ReviewItem, SmallFabItem, ReviewDialog) igual que antes ...
@Composable
fun SmallFabItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
        Surface(color = Color.White, shape = RoundedCornerShape(8.dp), shadowElevation = 2.dp, modifier = Modifier.padding(end = 8.dp)) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ColorArcDarkBrown, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
        SmallFloatingActionButton(onClick = onClick, containerColor = ColorHeaderBeige, contentColor = ColorArcDarkBrown) {
            Icon(icon, contentDescription = label)
        }
    }
}

@Composable
fun SynopsisSection(description: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text("Sinopsis", fontSize = 18.sp, fontFamily = CenturyGotic, fontWeight = FontWeight.Bold, color = ColorTituloTopBooks)
        Spacer(modifier = Modifier.height(8.dp))
        Text(if (description.isNotBlank()) description else "No hay descripción disponible.", fontSize = 14.sp, lineHeight = 22.sp, color = Color.DarkGray, textAlign = TextAlign.Justify)
    }
}

@Composable
fun ReviewItem(review: Review) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val avatarModifier = Modifier.size(40.dp).clip(CircleShape).border(1.dp, Color.LightGray, CircleShape)
                if (review.userPhotoUrl.isNotEmpty() && review.userPhotoUrl.startsWith("http")) {
                    AsyncImage(model = review.userPhotoUrl, contentDescription = null, modifier = avatarModifier, contentScale = ContentScale.Crop)
                } else {
                    Image(painter = painterResource(AvatarHelper.getDrawableId(review.userPhotoUrl)), contentDescription = null, modifier = avatarModifier, contentScale = ContentScale.Crop)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(review.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Row {
                    repeat(review.rating) { Icon(Icons.Default.Star, null, tint = Color(0xFFFFD54F), modifier = Modifier.size(16.dp)) }
                }
            }
            if (review.text.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(review.text, fontSize = 14.sp, color = Color.DarkGray, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
fun ReviewDialog(onDismiss: () -> Unit, onSubmit: (Int, String) -> Unit) {
    var rating by remember { mutableIntStateOf(0) }
    var reviewText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Escribe tu reseña", fontWeight = FontWeight.Bold, color = ColorTituloTopBooks, fontFamily = CenturyGotic) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    for (i in 1..5) {
                        IconButton(onClick = { rating = i }) {
                            Icon(if (i <= rating) Icons.Default.Star else Icons.Outlined.Star, "$i estrellas", tint = if (i <= rating) Color(0xFFFFD54F) else Color.LightGray, modifier = Modifier.size(36.dp))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = reviewText, onValueChange = { reviewText = it },
                    label = { Text("Tu opinión") },
                    placeholder = { Text("¿Qué te ha parecido el libro?") },
                    minLines = 4, maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ColorArcMediumBrown, focusedLabelColor = ColorArcMediumBrown),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(rating, reviewText) }, enabled = rating > 0 && reviewText.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = ColorArcMediumBrown)) {
                Text("Publicar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = ColorArcDarkBrown) } },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}