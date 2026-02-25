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
    onNavigateToJournal: (String, String, String, String, String) -> Unit, // Recibe 5 parámetros
    viewModel: BookDetailViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var isFabExpanded by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }
    var showCommentDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBookmarkDialog by remember { mutableStateOf(false) }

    LaunchedEffect(bookId) { viewModel.getBook(bookId) }

    // --- FORMULARIOS PREMIUM ---
    if (showReviewDialog && state.book != null) {
        PremiumReviewDialog(onDismiss = { showReviewDialog = false }, onSubmit = { r, t -> viewModel.saveReview(state.book!!, r, t) { showReviewDialog = false } })
    }

    if (showCommentDialog && state.book != null) {
        PremiumCommentDialog(
            onDismiss = { showCommentDialog = false },
            onSubmit = { text, chapter ->
                viewModel.saveComment(state.book!!, text, chapter) { showCommentDialog = false }
            }
        )
    }

    if (showBookmarkDialog) {
        PremiumAddBookmarkDialog(onDismiss = { showBookmarkDialog = false }, onConfirm = { p, q, c, pub -> viewModel.addBookmark(bookId, p, q, c, pub); showBookmarkDialog = false })
    }

    if (showDeleteDialog && state.book != null) {
        PremiumDeleteDialog(listName = state.savedInList ?: "biblioteca", onDismiss = { showDeleteDialog = false }, onConfirm = { viewModel.removeFromList(state.book!!.id); showDeleteDialog = false })
    }

    Scaffold(
        containerColor = ColorBackGroundGeneral,
        topBar = { TopBar(onBackClick = onBackClick) },
        floatingActionButton = {
            if (state.book != null) {
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedVisibility(visible = isFabExpanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                        SmallFabItem(Icons.Default.Call, "Diario de lectura") {
                            isFabExpanded = false
                            // ENVIAMOS TODA LA INFO INCLUYENDO PÁGINAS
                            onNavigateToJournal(
                                state.book!!.id,
                                state.book!!.title,
                                state.book!!.authors.joinToString(", "),
                                state.book!!.imageUrl,
                                state.book!!.pageCount.toString()
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    AnimatedVisibility(visible = isFabExpanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) { SmallFabItem(Icons.Default.Call, "Añadir marcador") { isFabExpanded = false; showBookmarkDialog = true } }
                    Spacer(Modifier.height(8.dp))
                    AnimatedVisibility(visible = isFabExpanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) { SmallFabItem(Icons.Default.Edit, "Escribir reseña") { isFabExpanded = false; showReviewDialog = true } }
                    Spacer(Modifier.height(8.dp))

                    AnimatedVisibility(visible = isFabExpanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                        SmallFabItem(Icons.Default.Send, "Escribir comentario") { isFabExpanded = false; showCommentDialog = true }
                    }
                    Spacer(Modifier.height(8.dp))

                    AnimatedVisibility(visible = isFabExpanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) { SmallFabItem(Icons.Default.Search, "Ver opiniones") { isFabExpanded = false; coroutineScope.launch { listState.animateScrollToItem(2) } } }
                    Spacer(Modifier.height(16.dp))
                    val rotation by animateFloatAsState(if (isFabExpanded) 45f else 0f)
                    FloatingActionButton(onClick = { isFabExpanded = !isFabExpanded }, containerColor = ColorArcMediumBrown, contentColor = Color.White, shape = CircleShape) { Icon(Icons.Default.Add, null, modifier = Modifier.rotate(rotation)) }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) { CircularProgressIndicator(Modifier.align(Alignment.Center), ColorArcMediumBrown) }
            else if (state.book != null) {
                val book = state.book!!
                LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 100.dp), modifier = Modifier.fillMaxSize()) {
                    item { BookHeaderSection(book = book, savedInList = state.savedInList, onListAction = { if (state.savedInList == it) showDeleteDialog = true else viewModel.addToList(book, it) }) }
                    item { SynopsisSection(book.description) }
                    item { Text("Opiniones de la comunidad", fontFamily = CenturyGotic, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ColorTituloTopBooks, modifier = Modifier.padding(24.dp, 16.dp)) }
                    if (state.reviews.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { Text("Nadie ha opinado todavía.", color = Color.Gray) } }
                    else items(state.reviews) { ReviewItem(it) }
                }
            }
        }
    }
}

// --- COMPONENTES ORIGINALES ---

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
        Text(if (description.isNotBlank()) description else "No hay descripción disponible.", fontSize = 14.sp, lineHeight = 22.sp, color = Color.DarkGray, textAlign = TextAlign.Justify)
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

// --- FORMULARIOS PREMIUM ---

@Composable
fun PremiumReviewDialog(onDismiss: () -> Unit, onSubmit: (Int, String) -> Unit) {
    var rating by remember { mutableIntStateOf(0) }; var reviewText by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Escribe tu opinión", fontFamily = GuardianCity, fontWeight = FontWeight.Bold, color = ColorArcDarkBrown) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    for (i in 1..5) {
                        val isSel = i <= rating; val color by animateColorAsState(if (isSel) Color(0xFFFFD54F) else Color.LightGray.copy(alpha = 0.5f))
                        IconButton(onClick = { rating = i }) { Icon(if (isSel) Icons.Default.Star else Icons.Outlined.Star, null, tint = color, modifier = Modifier.size(38.dp)) }
                    }
                }
                OutlinedTextField(value = reviewText, onValueChange = { reviewText = it }, label = { Text("Tu reseña") }, modifier = Modifier.fillMaxWidth(), minLines = 4, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ColorArcMediumBrown, focusedLabelColor = ColorArcMediumBrown))
            }
        },
        confirmButton = { Button(onClick = { onSubmit(rating, reviewText) }, enabled = rating > 0 && reviewText.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = ColorArcMediumBrown), shape = RoundedCornerShape(12.dp)) { Text("Publicar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) } }, containerColor = Color.White, shape = RoundedCornerShape(24.dp))
}

@Composable
fun PremiumAddBookmarkDialog(onDismiss: () -> Unit, onConfirm: (String, String, String, Boolean) -> Unit) {
    var p by remember { mutableStateOf("") }; var q by remember { mutableStateOf("") }; var c by remember { mutableStateOf("") }; var pub by remember { mutableStateOf(true) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Añadir Marcador", fontFamily = GuardianCity, fontWeight = FontWeight.Bold, color = ColorArcDarkBrown) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = p, onValueChange = { if(it.all { char -> char.isDigit() }) p = it }, label = { Text("Pág *") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = c, onValueChange = { c = it }, label = { Text("Capítulo") }, modifier = Modifier.weight(2f), singleLine = true, shape = RoundedCornerShape(12.dp))
                }
                OutlinedTextField(value = q, onValueChange = { q = it }, label = { Text("Frase memorable *") }, modifier = Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(12.dp))
                PrivacyToggleButton(isPublic = pub, onToggle = { pub = it })
            }
        },
        confirmButton = { Button(onClick = { onConfirm(p, q, c, pub) }, enabled = p.isNotEmpty() && q.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = ColorArcMediumBrown), shape = RoundedCornerShape(12.dp)) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) } }, containerColor = Color.White, shape = RoundedCornerShape(24.dp))
}

@Composable
fun PremiumDeleteDialog(listName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("¿Quitar libro?", fontFamily = GuardianCity, color = Color.Red.copy(0.7f)) },
        text = { Text("Se eliminará de tu lista '$listName'.") },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(0.7f)), shape = RoundedCornerShape(12.dp)) { Text("Eliminar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Mantener", color = Color.Gray) } }, containerColor = Color.White, shape = RoundedCornerShape(24.dp))
}

@Composable
fun PrivacyToggleButton(isPublic: Boolean, onToggle: (Boolean) -> Unit) {
    val pubCol by animateColorAsState(if (isPublic) ColorArcMediumBrown else Color.Transparent)
    val privCol by animateColorAsState(if (!isPublic) ColorArcMediumBrown else Color.Transparent)
    val pubText by animateColorAsState(if (isPublic) Color.White else Color.Gray)
    val privText by animateColorAsState(if (!isPublic) Color.White else Color.Gray)
    Surface(modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(24.dp), color = ColorHeaderBeige.copy(0.5f), border = androidx.compose.foundation.BorderStroke(1.dp, ColorArcMediumBrown.copy(0.3f))) {
        Row {
            Box(Modifier.weight(1f).fillMaxHeight().clip(CircleShape).background(pubCol).clickable { onToggle(true) }, Alignment.Center) {
                Row { Icon(Icons.Default.Call, null, tint = pubText, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Público", color = pubText, fontWeight = FontWeight.Bold) }
            }
            Box(Modifier.weight(1f).fillMaxHeight().clip(CircleShape).background(privCol).clickable { onToggle(false) }, Alignment.Center) {
                Row { Icon(Icons.Default.Lock, null, tint = privText, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Privado", color = privText, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun PremiumCommentDialog(onDismiss: () -> Unit, onSubmit: (String, String) -> Unit) {
    var commentText by remember { mutableStateOf("") }
    var chapterText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = null,
                    tint = ColorArcMediumBrown,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Unirse a la charla",
                    fontFamily = GuardianCity,
                    fontWeight = FontWeight.Bold,
                    color = ColorArcDarkBrown,
                    fontSize = 22.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Comparte tus pensamientos. Puedes indicar en qué capítulo vas para dar contexto a los demás.",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )

                OutlinedTextField(
                    value = chapterText,
                    onValueChange = { chapterText = it },
                    label = { Text("Capítulo (Opcional)") },
                    leadingIcon = {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = ColorArcMediumBrown)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorArcMediumBrown,
                        focusedLabelColor = ColorArcMediumBrown,
                        cursorColor = ColorArcMediumBrown
                    )
                )

                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    label = { Text("Tu comentario *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 6,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorArcMediumBrown,
                        focusedLabelColor = ColorArcMediumBrown,
                        cursorColor = ColorArcMediumBrown
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(commentText, chapterText) },
                enabled = commentText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ColorArcMediumBrown),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
            ) { Text("Publicar", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 10.dp
    )
}