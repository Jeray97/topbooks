package com.example.topbooks.ui.book

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.*
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
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.ui.theme.*
import com.example.topbooks.utils.AvatarHelper
import kotlinx.coroutines.launch

@Composable
fun BookDetailScreen(
    bookId: String,
    onBackClick: () -> Unit,
    onNavigateToJournal: (String) -> Unit,
    viewModel: BookDetailViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var isFabExpanded by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBookmarkDialog by remember { mutableStateOf(false) }

    LaunchedEffect(bookId) {
        viewModel.getBook(bookId)
    }

    // --- DIÁLOGOS REDISEÑADOS ---

    if (showReviewDialog && state.book != null) {
        PremiumReviewDialog(
            onDismiss = { showReviewDialog = false },
            onSubmit = { rating, text ->
                viewModel.saveReview(state.book!!, rating, text) { showReviewDialog = false }
            }
        )
    }

    if (showBookmarkDialog) {
        PremiumAddBookmarkDialog(
            onDismiss = { showBookmarkDialog = false },
            onConfirm = { p, q, c, pub ->
                viewModel.addBookmark(bookId, p, q, c, pub)
                showBookmarkDialog = false
            }
        )
    }

    if (showDeleteDialog && state.book != null) {
        PremiumDeleteDialog(
            listName = state.savedInList ?: "biblioteca",
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                viewModel.removeFromList(state.book!!.id)
                showDeleteDialog = false
            }
        )
    }

    Scaffold(
        containerColor = ColorBackGroundGeneral,
        topBar = { TopBar(onBackClick = onBackClick) },
        floatingActionButton = {
            if (state.book != null) {
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedVisibility(visible = isFabExpanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                        SmallFabItem(Icons.Default.Call, "Diario de lectura") { isFabExpanded = false; onNavigateToJournal(bookId) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    AnimatedVisibility(visible = isFabExpanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                        SmallFabItem(Icons.Default.Call, "Añadir marcador") { isFabExpanded = false; showBookmarkDialog = true }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    AnimatedVisibility(visible = isFabExpanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                        SmallFabItem(Icons.Default.Edit, "Escribir reseña") { isFabExpanded = false; showReviewDialog = true }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    AnimatedVisibility(visible = isFabExpanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                        SmallFabItem(Icons.Default.Search, "Ver opiniones") { isFabExpanded = false; coroutineScope.launch { listState.animateScrollToItem(2) } }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    val rotation by animateFloatAsState(if (isFabExpanded) 45f else 0f)
                    FloatingActionButton(
                        onClick = { isFabExpanded = !isFabExpanded },
                        containerColor = ColorArcMediumBrown,
                        contentColor = Color.White,
                        shape = CircleShape
                    ) { Icon(Icons.Default.Add, "Menú", modifier = Modifier.rotate(rotation)) }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = ColorArcMediumBrown)
            } else if (state.book != null) {
                val book = state.book!!
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 100.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        BookHeaderSection(
                            book = book,
                            savedInList = state.savedInList,
                            onListAction = { targetList ->
                                if (state.savedInList == targetList) showDeleteDialog = true
                                else viewModel.addToList(book, targetList)
                            }
                        )
                    }
                    item { SynopsisSection(book.description) }
                    item {
                        Text(
                            text = "Opiniones de la comunidad (${state.reviews.size})",
                            fontFamily = CenturyGotic, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                            color = ColorTituloTopBooks, modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                        )
                    }
                    if (state.reviews.isEmpty()) {
                        item { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("Nadie ha opinado todavía.", color = Color.Gray) } }
                    } else {
                        items(state.reviews) { ReviewItem(it) }
                    }
                }
            }
        }
    }
}

// --- FORMULARIOS PREMIUM (UNIFICADOS) ---

@Composable
fun PremiumReviewDialog(onDismiss: () -> Unit, onSubmit: (Int, String) -> Unit) {
    var rating by remember { mutableIntStateOf(0) }
    var reviewText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Escribe tu opinión", fontFamily = GuardianCity, fontWeight = FontWeight.Bold, color = ColorArcDarkBrown, fontSize = 20.sp)
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Selector de estrellas Premium
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    for (i in 1..5) {
                        val isSelected = i <= rating
                        val starColor by animateColorAsState(if (isSelected) Color(0xFFFFD54F) else Color.LightGray.copy(alpha = 0.5f))
                        IconButton(onClick = { rating = i }) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.Star else Icons.Outlined.Star,
                                contentDescription = null,
                                tint = starColor,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = reviewText,
                    onValueChange = { reviewText = it },
                    label = { Text("Tu reseña") },
                    placeholder = { Text("¿Qué te pareció esta historia?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ColorArcMediumBrown, focusedLabelColor = ColorArcMediumBrown)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(rating, reviewText) },
                enabled = rating > 0 && reviewText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ColorArcMediumBrown),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Publicar reseña") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) } },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun PremiumAddBookmarkDialog(onDismiss: () -> Unit, onConfirm: (String, String, String, Boolean) -> Unit) {
    var page by remember { mutableStateOf("") }
    var quote by remember { mutableStateOf("") }
    var chapter by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir Marcador", fontFamily = GuardianCity, fontWeight = FontWeight.Bold, color = ColorArcDarkBrown, fontSize = 20.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = page,
                        onValueChange = { if(it.all { c -> c.isDigit() }) page = it },
                        label = { Text("Pág *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ColorArcMediumBrown, focusedLabelColor = ColorArcMediumBrown)
                    )
                    OutlinedTextField(
                        value = chapter,
                        onValueChange = { chapter = it },
                        label = { Text("Capítulo") },
                        modifier = Modifier.weight(2f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ColorArcMediumBrown, focusedLabelColor = ColorArcMediumBrown)
                    )
                }

                OutlinedTextField(
                    value = quote,
                    onValueChange = { quote = it },
                    label = { Text("Frase memorable *") },
                    placeholder = { Text("Escribe una frase del libro...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ColorArcMediumBrown, focusedLabelColor = ColorArcMediumBrown)
                )

                Text("Privacidad", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ColorArcDarkBrown)
                PrivacyToggleButton(isPublic = isPublic, onToggle = { isPublic = it })
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(page, quote, chapter, isPublic) },
                enabled = page.isNotEmpty() && quote.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = ColorArcMediumBrown),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) } },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun PremiumDeleteDialog(listName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("¿Quitar libro?", fontFamily = GuardianCity, fontWeight = FontWeight.Bold, color = Color.Red.copy(alpha = 0.8f))
        },
        text = {
            Text("Se eliminará de tu lista '$listName'. ¿Estás seguro?", fontSize = 14.sp, color = Color.DarkGray)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Eliminar", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Mantener", color = Color.Gray) }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun PrivacyToggleButton(isPublic: Boolean, onToggle: (Boolean) -> Unit) {
    val publicColor by animateColorAsState(if (isPublic) ColorArcMediumBrown else Color.Transparent)
    val privateColor by animateColorAsState(if (!isPublic) ColorArcMediumBrown else Color.Transparent)
    val publicTextColor by animateColorAsState(if (isPublic) Color.White else Color.Gray)
    val privateTextColor by animateColorAsState(if (!isPublic) Color.White else Color.Gray)

    Surface(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(24.dp),
        color = ColorHeaderBeige.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, ColorArcMediumBrown.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(24.dp)).background(publicColor).clickable { onToggle(true) }, contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Call, null, tint = publicTextColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp)); Text("Público", color = publicTextColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(24.dp)).background(privateColor).clickable { onToggle(false) }, contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, tint = privateTextColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp)); Text("Privado", color = privateTextColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

// --- RESTO DE COMPONENTES (HEADER, STATUS, SYNOPSIS, REVIEW ITEM) ---

@Composable
fun BookHeaderSection(book: Book, savedInList: String?, onListAction: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(10.dp), modifier = Modifier.width(170.dp).height(260.dp)) {
            AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(book.imageUrl).crossfade(true).error(com.example.topbooks.R.drawable.icon_codigodebarras).build(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(book.title, fontSize = 24.sp, fontFamily = GuardianCity, fontWeight = FontWeight.Bold, color = ColorTituloTopBooks, textAlign = TextAlign.Center, lineHeight = 28.sp, modifier = Modifier.padding(horizontal = 24.dp))
        Text(book.authors.joinToString(", "), fontSize = 16.sp, fontFamily = CenturyGotic, color = ColorArcDarkBrown, modifier = Modifier.padding(top = 8.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatusButton(label = "Favoritos", isActive = savedInList == "Favoritos", activeIcon = Icons.Default.Favorite, inactiveIcon = Icons.Default.FavoriteBorder, onClick = { onListAction("Favoritos") })
            StatusButton(label = "Leídos", isActive = savedInList == "Leídos", activeIcon = Icons.Default.CheckCircle, inactiveIcon = Icons.Outlined.CheckCircle, onClick = { onListAction("Leídos") })
            StatusButton(label = "Pendientes", isActive = savedInList == "Pendientes", activeIcon = Icons.Default.Info, inactiveIcon = Icons.Default.Info, onClick = { onListAction("Pendientes") })
        }
    }
}

@Composable
fun StatusButton(label: String, isActive: Boolean, activeIcon: ImageVector, inactiveIcon: ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }.padding(8.dp)) {
        Box(modifier = Modifier.size(50.dp).background(color = if (isActive) ColorArcMediumBrown else Color.White, shape = CircleShape).border(width = if (isActive) 0.dp else 1.dp, color = ColorArcMediumBrown, shape = CircleShape), contentAlignment = Alignment.Center) {
            Icon(imageVector = if (isActive) activeIcon else inactiveIcon, contentDescription = label, tint = if (isActive) Color.White else ColorArcMediumBrown, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 11.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, color = if (isActive) ColorArcMediumBrown else Color.Gray)
    }
}

@Composable
fun SmallFabItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
        Surface(color = Color.White, shape = RoundedCornerShape(8.dp), shadowElevation = 2.dp, modifier = Modifier.padding(end = 8.dp)) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ColorArcDarkBrown, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
        SmallFloatingActionButton(onClick = onClick, containerColor = ColorHeaderBeige, contentColor = ColorArcDarkBrown) { Icon(icon, contentDescription = label) }
    }
}

@Composable
fun SynopsisSection(description: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text("Sinopsis", fontSize = 18.sp, fontFamily = CenturyGotic, fontWeight = FontWeight.Bold, color = ColorTituloTopBooks)
        Spacer(modifier = Modifier.height(8.dp))
        Text(description.ifBlank { "No hay descripción disponible." }, fontSize = 14.sp, lineHeight = 22.sp, color = Color.DarkGray, textAlign = TextAlign.Justify)
    }
}

@Composable
fun ReviewItem(review: Review) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val avatarModifier = Modifier.size(40.dp).clip(CircleShape).border(1.dp, Color.LightGray, CircleShape)
                if (review.userPhotoUrl.isNotEmpty() && review.userPhotoUrl.startsWith("http")) { AsyncImage(model = review.userPhotoUrl, contentDescription = null, modifier = avatarModifier, contentScale = ContentScale.Crop) }
                else { Image(painter = painterResource(AvatarHelper.getDrawableId(review.userPhotoUrl)), contentDescription = null, modifier = avatarModifier, contentScale = ContentScale.Crop) }
                Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(review.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                Row { repeat(review.rating) { Icon(Icons.Default.Star, null, tint = Color(0xFFFFD54F), modifier = Modifier.size(16.dp)) } }
            }
            if (review.text.isNotEmpty()) { Spacer(Modifier.height(8.dp)); Text(review.text, fontSize = 14.sp, color = Color.DarkGray, lineHeight = 20.sp) }
        }
    }
}