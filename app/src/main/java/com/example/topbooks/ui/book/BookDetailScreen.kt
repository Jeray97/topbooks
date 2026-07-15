package com.example.topbooks.ui.book

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.topbooks.R
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.model.Review
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.ui.theme.*
import com.example.topbooks.utils.AvatarHelper
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * PANTALLA PRINCIPAL DE DETALLES DEL LIBRO (Stateful Composable)
 * * Es la vista más completa de la aplicación. Gestiona la información del libro,
 * las interacciones del usuario (Favoritos, Listas) y los modales emergentes.
 *
 * @param bookId ID del libro que debe cargar el ViewModel.
 * @param onBackClick Acción al pulsar la flecha hacia atrás en la TopBar.
 * @param onNavigateToJournal Acción que dirige a la pantalla del Diario de Lectura, pasando los datos básicos del libro por ruta.
 * @param viewModel ViewModel asociado que proporciona el estado y la lógica.
 */
@Composable
fun BookDetailScreen(
    bookId: String,
    onBackClick: () -> Unit,
    onNavigateToJournal: (String, String, String, String, String) -> Unit,
    onNavigateToReviews: (String) -> Unit = {},
    onNavigateToCreatePost: (String, String) -> Unit = { _, _ -> },
    viewModel: BookDetailViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState() // Para controlar el scroll de la página
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // --- CONTROL DE VISIBILIDAD DE MENÚS Y MODALES ---
    var isFabExpanded by remember { mutableStateOf(false) } // Expansión del botón flotante (FAB)
    var showReviewDialog by remember { mutableStateOf(false) }
    var showCommentDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBookmarkDialog by remember { mutableStateOf(false) }

    // NUEVOS ESTADOS PARA EL SISTEMA DE SAGAS COMUNITARIO
    var showEditSeriesDialog by remember { mutableStateOf(false) }
    var showSeriesInfoDialog by remember { mutableStateOf(false) }

    // Cargamos el libro al iniciar la pantalla asegurando que solo ocurra cuando cambia el ID
    LaunchedEffect(bookId) {
        viewModel.loadBook(bookId)
        viewModel.loadReviews(bookId)
    }

    // --- RENDERIZADO DE FORMULARIOS (Dialogs) ---
    // Solo se muestran si la variable booleana es 'true' y ya tenemos datos del libro.

    if (showReviewDialog && state.book != null) {
        PremiumReviewDialog(
            onDismiss = { showReviewDialog = false },
            onSubmit = { r, t -> viewModel.saveReview(state.book!!, r, t) { showReviewDialog = false } }
        )
    }

    if (showCommentDialog && state.book != null) {
        PremiumCommentDialog(
            onDismiss = { showCommentDialog = false },
            onSubmit = { text, chapter -> viewModel.saveComment(state.book!!, text, chapter) { showCommentDialog = false } }
        )
    }

    if (showBookmarkDialog && state.book != null) {
        PremiumAddBookmarkDialog(
            onDismiss = { showBookmarkDialog = false },
            onConfirm = { p, q, c, pub ->
                viewModel.saveBookmark(state.book!!, p, q, c, pub) { showBookmarkDialog = false }
            }
        )
    }

    if (showDeleteDialog && state.book != null) {
        PremiumDeleteDialog(
            listName = state.savedInList ?: "biblioteca",
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                viewModel.removeFromList(state.book!!.id, state.savedInList ?: "")
                showDeleteDialog = false
            }
        )
    }

    // FORMULARIO: Editar Saga
    if (showEditSeriesDialog && state.book != null) {
        var editName by remember { mutableStateOf(state.book!!.seriesName) }
        var editIndex by remember { mutableStateOf(if (state.book!!.seriesIndex > 0) state.book!!.seriesIndex.toString() else "") }
        var isAutoconclusivo by remember { mutableStateOf(!state.book!!.isSaga) }

        AlertDialog(
            onDismissRequest = { showEditSeriesDialog = false },
            title = { Text("Editar Información", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            text = {
                Column {
                    Text("Ayuda a la comunidad a mantener los datos correctos.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isAutoconclusivo,
                            onCheckedChange = { isAutoconclusivo = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Text("Es un libro autoconclusivo")
                    }

                    if (!isAutoconclusivo) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Nombre de la Saga") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                cursorColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editIndex,
                            onValueChange = { editIndex = it.filter { char -> char.isDigit() } },
                            label = { Text("Número del libro") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                cursorColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalName = if (isAutoconclusivo) "" else editName
                        val finalIndex = if (isAutoconclusivo) 0 else editIndex.toIntOrNull() ?: 0
                        viewModel.editSeries(finalName, finalIndex) {
                            showEditSeriesDialog = false
                            Toast.makeText(context, "¡Gracias por tu contribución!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("Guardar", color = MaterialTheme.colorScheme.onPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showEditSeriesDialog = false }) { Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // FORMULARIO: Información de la Saga (Antitrolls y Sistema de Votos)
    if (showSeriesInfoDialog && state.book != null) {
        val book = state.book!!
        AlertDialog(
            onDismissRequest = { showSeriesInfoDialog = false },
            title = { Text("Sobre este dato", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            text = {
                Column {
                    Text(
                        "Esta información es extraída automáticamente de internet y puede contener errores. Si detectas un fallo, toca el nombre para corregirlo.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!book.seriesEditorName.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Última edición por:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(AvatarHelper.getDrawableId(book.seriesEditorAvatar)),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(book.seriesEditorName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("¿Es correcta esta edición?", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            IconButton(onClick = {
                                viewModel.voteSeriesEdit(true)
                                Toast.makeText(context, "Voto registrado", Toast.LENGTH_SHORT).show()
                            }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.ThumbUp, contentDescription = "Bien", tint = Color(0xFF4CAF50))
                                    Text("${book.seriesUpvotes}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = {
                                viewModel.voteSeriesEdit(false)
                                Toast.makeText(context, "Voto registrado", Toast.LENGTH_SHORT).show()
                            }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.ThumbDown, contentDescription = "Mal", tint = Color(0xFFE57373))
                                    Text("${book.seriesDownvotes}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSeriesInfoDialog = false }) {
                    Text("Entendido", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // --- ESTRUCTURA VISUAL PRINCIPAL (Scaffold) ---
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
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
        },

        // --- FLOATING ACTION BUTTON (MENÚ EXPANDIBLE) ---
        floatingActionButton = {
            if (state.book != null) {
                Column(horizontalAlignment = Alignment.End) {
                    // Animamos la aparición de las opciones secundarias desde abajo (expandVertically)
                    AnimatedVisibility(visible = isFabExpanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                        SmallFabItem(Icons.Default.Book, stringResource(R.string.bookdetail_fab_journal)) {
                            isFabExpanded = false

                            // TÉCNICA: Codificamos los datos para que Compose Navigation no se confunda con espacios o barras (/).
                            val safeTitle = URLEncoder.encode(state.book!!.title.ifEmpty { "Sin Título" }, StandardCharsets.UTF_8.toString())
                            val safeAuthor = URLEncoder.encode(state.book!!.authors.joinToString(", ").ifEmpty { "Desconocido" }, StandardCharsets.UTF_8.toString())
                            val safeImage = URLEncoder.encode(state.book!!.imageUrl.ifEmpty { "empty" }, StandardCharsets.UTF_8.toString())

                            onNavigateToJournal(
                                state.book!!.id,
                                safeTitle,
                                safeAuthor,
                                safeImage,
                                state.book!!.pageCount.toString()
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    AnimatedVisibility(visible = isFabExpanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) { SmallFabItem(Icons.Default.Bookmark, stringResource(R.string.bookdetail_fab_bookmark)) { isFabExpanded = false; showBookmarkDialog = true } }
                    Spacer(Modifier.height(8.dp))

                    AnimatedVisibility(visible = isFabExpanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                        SmallFabItem(Icons.Default.Edit, stringResource(R.string.bookdetail_fab_review)) {
                            isFabExpanded = false
                            onNavigateToCreatePost(bookId, "REVIEW")
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    AnimatedVisibility(visible = isFabExpanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                        SmallFabItem(Icons.AutoMirrored.Filled.Send, stringResource(R.string.bookdetail_fab_comment)) {
                            isFabExpanded = false
                            onNavigateToCreatePost(bookId, "QUOTE")
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    AnimatedVisibility(visible = isFabExpanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                        SmallFabItem(Icons.Default.Search, stringResource(R.string.bookdetail_fab_see_reviews)) {
                            isFabExpanded = false
                            if (state.reviews.isNotEmpty()) {
                                onNavigateToReviews(bookId)
                            } else {
                                Toast.makeText(context, context.getString(R.string.bookdetail_reviews_empty), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    // Rotación animada del icono "+" principal al desplegarse
                    val rotation by animateFloatAsState(if (isFabExpanded) 45f else 0f)
                    FloatingActionButton(onClick = { isFabExpanded = !isFabExpanded }, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, shape = CircleShape) { Icon(Icons.Default.Add, null, modifier = Modifier.rotate(rotation)) }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), MaterialTheme.colorScheme.primary)
            } else if (state.book != null) {
                val book = state.book!!
                LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 100.dp), modifier = Modifier.fillMaxSize()) {

                    // 1. Cabecera (Portada, Título, Autor, Botones de Estado)
                    item {
                        BookHeaderSection(
                            book = book,
                            savedInList = state.savedInList,
                            isFavorite = state.isFavorite, // Lee directamente del estado del ViewModel
                            onFavoriteClick = {
                                viewModel.toggleFavorite(book) // Llama a la función del ViewModel
                            },
                            onListAction = {
                                // Pendientes y Leídos siguen siendo excluyentes. Los textos de la lógica no se traducen.
                                if (state.savedInList == it) showDeleteDialog = true
                                else viewModel.addToList(book, it)
                            },
                            // PASAMOS LAS LAMBDAS AL HEADER PARA EDITAR SAGAS
                            onEditSagaClick = { showEditSeriesDialog = true },
                            onInfoSagaClick = { showSeriesInfoDialog = true }
                        )
                    }

                    // 2. Sección de compra (enlaces de afiliado)
                    item { BookPurchaseSection(book, context) }

                    // 3. Sinopsis
                    item { SynopsisSection(book.description) }

                    // 3. Título de Sección de Reseñas
                    item { Text(stringResource(R.string.bookdetail_reviews_title), fontFamily = GuardianCity, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(24.dp, 16.dp)) }

                    // 4. Lista de Reseñas
                    if (state.reviews.isEmpty()) {
                        item { Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { Text(stringResource(R.string.bookdetail_reviews_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    } else {
                        items(state.reviews) { ReviewItem(it) }
                    }
                }
            }
        }
    }
}

// =========================================================================================
// --- COMPONENTES ORIGINALES (STATELESS) ---
// =========================================================================================

/**
 * Sección superior de la pantalla. Muestra la portada, título, y la botonera de interacción.
 */
@Composable
fun BookHeaderSection(
    book: Book,
    savedInList: String?,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onListAction: (String) -> Unit,
    onEditSagaClick: () -> Unit,
    onInfoSagaClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
        Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(8.dp), modifier = Modifier.width(170.dp).height(260.dp)) {
            AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(book.imageUrl).crossfade(true).error(
                R.drawable.icon_codigodebarras).build(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(book.title, fontSize = 24.sp, fontFamily = GuardianCity, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center, lineHeight = 28.sp, modifier = Modifier.padding(horizontal = 24.dp))
        Text(book.authors.joinToString(", "), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))

        Spacer(modifier = Modifier.height(12.dp))

        val seriesText = if (book.isSaga) stringResource(R.string.bookdetail_badge_saga) + " [${book.seriesName} #${book.seriesIndex}]" else stringResource(R.string.bookdetail_badge_standalone)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(9999.dp),
                modifier = Modifier.clickable { onEditSagaClick() }
            ) {
                Text(
                    text = seriesText,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { onInfoSagaClick() },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Información de la Saga",
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatusButton(label = stringResource(R.string.bookdetail_status_favorites), isActive = isFavorite, activeIcon = Icons.Default.Favorite, inactiveIcon = Icons.Default.FavoriteBorder, onClick = onFavoriteClick)
            StatusButton(label = stringResource(R.string.bookdetail_status_read), isActive = savedInList == "Leídos", activeIcon = Icons.Default.CheckCircle, inactiveIcon = Icons.Outlined.CheckCircle, onClick = { onListAction("Leídos") })
            StatusButton(label = stringResource(R.string.bookdetail_status_pending), isActive = savedInList == "Pendientes", activeIcon = Icons.Default.AccessTimeFilled, inactiveIcon = Icons.Default.AccessTime, onClick = { onListAction("Pendientes") })
        }
    }
}

/** Componente de botón circular utilizado para marcar libros como Favoritos, Leídos o Pendientes. */
@Composable
fun StatusButton(label: String, isActive: Boolean, activeIcon: ImageVector, inactiveIcon: ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }.padding(8.dp)) {
        Box(modifier = Modifier.size(50.dp).background(color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, shape = CircleShape).border(width = if (isActive) 0.dp else 1.dp, color = MaterialTheme.colorScheme.primary, shape = CircleShape), contentAlignment = Alignment.Center) {
            Icon(imageVector = if (isActive) activeIcon else inactiveIcon, contentDescription = label, tint = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 11.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Sección de compra con enlaces de afiliado a tiendas online.
 * Muestra botones para comprar el libro en Casa del Libro y Fnac.
 */
@Composable
fun BookPurchaseSection(book: Book, context: android.content.Context) {
    // IDs de afiliado - Reemplazar con tus IDs reales
    val CASA_DEL_LIBRO_AFFILIATE_ID = "TU_ID_CASA_DEL_LIBRO"
    val FNAC_AFFILIATE_ID = "TU_ID_FNAC"
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Dónde comprar",
            fontFamily = GuardianCity,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Botón Casa del Libro
        OutlinedButton(
            onClick = {
                val encodedTitle = URLEncoder.encode(book.title, StandardCharsets.UTF_8.toString())
                val url = "https://www.casadellibro.com/libros?query=$encodedTitle&affiliate=$CASA_DEL_LIBRO_AFFILIATE_ID"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Casa del Libro",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        // Botón Fnac
        OutlinedButton(
            onClick = {
                val encodedTitle = URLEncoder.encode(book.title, StandardCharsets.UTF_8.toString())
                val url = "https://www.fnac.es/SearchResult/ResultList.aspx?Search=$encodedTitle&affiliate=$FNAC_AFFILIATE_ID"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Store,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Fnac",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        // Nota informativa
        Text(
            text = "Los enlaces pueden contener afiliados. Al comprar a través de estos enlaces, apoyas el desarrollo de la app.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}

/** Componente individual para las opciones desplegables del Floating Action Button. */
@Composable
fun SmallFabItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp), shadowElevation = 2.dp, modifier = Modifier.padding(end = 8.dp)) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
        SmallFloatingActionButton(onClick = onClick, containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary) { Icon(icon, contentDescription = label) }
    }
}

@Composable
fun SynopsisSection(description: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text(stringResource(R.string.bookdetail_synopsis_title), fontSize = 20.sp, fontFamily = GuardianCity, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(description.ifBlank { stringResource(R.string.bookdetail_synopsis_empty) }, fontSize = 14.sp, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Justify)
    }
}

/** Muestra una reseña escrita por la comunidad, incluyendo el avatar del usuario y las estrellas. */
@Composable
fun ReviewItem(review: Review) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val avatarModifier = Modifier.size(40.dp).clip(CircleShape).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                if (review.userPhotoUrl.isNotEmpty() && review.userPhotoUrl.startsWith("http")) { AsyncImage(model = review.userPhotoUrl, contentDescription = null, modifier = avatarModifier, contentScale = ContentScale.Crop) }
                else { Image(painter = painterResource(AvatarHelper.getDrawableId(review.userPhotoUrl)), contentDescription = null, modifier = avatarModifier, contentScale = ContentScale.Crop) }
                Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(review.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface) }
                Row { repeat(review.rating) { Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(16.dp)) } }
            }
            if (review.text.isNotEmpty()) { Spacer(Modifier.height(8.dp)); Text(review.text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp) }
        }
    }
}

/** Cuadro de diálogo interactivo donde el usuario elige entre 1 y 5 estrellas y escribe su reseña. */
@Composable
fun PremiumReviewDialog(onDismiss: () -> Unit, onSubmit: (Int, String) -> Unit) {
    var rating by remember { mutableIntStateOf(0) }; var reviewText by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.bookdetail_review_dialog_title), fontFamily = GuardianCity, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    for (i in 1..5) {
                        val isSel = i <= rating; val color by animateColorAsState(if (isSel) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        IconButton(onClick = { rating = i }) { Icon(if (isSel) Icons.Default.Star else Icons.Outlined.Star, null, tint = color, modifier = Modifier.size(38.dp)) }
                    }
                }
                OutlinedTextField(value = reviewText, onValueChange = { reviewText = it }, label = { Text(stringResource(R.string.bookdetail_review_label)) }, modifier = Modifier.fillMaxWidth(), minLines = 4, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary))
            }
        },
        confirmButton = { Button(onClick = { onSubmit(rating, reviewText) }, enabled = rating > 0 && reviewText.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), shape = RoundedCornerShape(12.dp)) { Text(stringResource(R.string.bookdetail_action_publish), color = MaterialTheme.colorScheme.onPrimary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.bookdetail_action_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant) } }, containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp))
}

/** Cuadro de diálogo para crear un Marcador (Cita textual + Capítulo y Página). */
@Composable
fun PremiumAddBookmarkDialog(onDismiss: () -> Unit, onConfirm: (String, String, String, Boolean) -> Unit) {
    var p by remember { mutableStateOf("") }; var q by remember { mutableStateOf("") }; var c by remember { mutableStateOf("") }; var pub by remember { mutableStateOf(true) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.bookdetail_bookmark_dialog_title), fontFamily = GuardianCity, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = p, onValueChange = { if(it.all { char -> char.isDigit() }) p = it }, label = { Text(stringResource(R.string.bookdetail_bookmark_page)) }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary))
                    OutlinedTextField(value = c, onValueChange = { c = it }, label = { Text(stringResource(R.string.bookdetail_bookmark_chapter)) }, modifier = Modifier.weight(2f), singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary))
                }
                OutlinedTextField(value = q, onValueChange = { q = it }, label = { Text(stringResource(R.string.bookdetail_bookmark_quote)) }, modifier = Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary))
                PrivacyToggleButton(isPublic = pub, onToggle = { pub = it })
            }
        },
        confirmButton = { Button(onClick = { onConfirm(p, q, c, pub) }, enabled = p.isNotEmpty() && q.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), shape = RoundedCornerShape(12.dp)) { Text(stringResource(R.string.bookdetail_action_save), color = MaterialTheme.colorScheme.onPrimary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.bookdetail_action_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant) } }, containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp))
}

@Composable
fun PremiumDeleteDialog(listName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.bookdetail_delete_dialog_title), fontFamily = GuardianCity, color = Color(0xFFBA1A1A)) },
        text = { Text(stringResource(R.string.bookdetail_delete_dialog_body, listName)) },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)), shape = RoundedCornerShape(12.dp)) { Text(stringResource(R.string.bookdetail_action_delete), color = MaterialTheme.colorScheme.onPrimary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.bookdetail_action_keep), color = MaterialTheme.colorScheme.onSurfaceVariant) } }, containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp))
}

/** Componente animado que funciona como interruptor (Público / Privado) para un diario o marcador. */
@Composable
fun PrivacyToggleButton(isPublic: Boolean, onToggle: (Boolean) -> Unit) {
    val pubCol by animateColorAsState(if (isPublic) MaterialTheme.colorScheme.primary else Color.Transparent)
    val privCol by animateColorAsState(if (!isPublic) MaterialTheme.colorScheme.primary else Color.Transparent)
    val pubText by animateColorAsState(if (isPublic) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
    val privText by animateColorAsState(if (!isPublic) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
    Surface(modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.3f))) {
        Row {
            Box(Modifier.weight(1f).fillMaxHeight().clip(CircleShape).background(pubCol).clickable { onToggle(true) }, Alignment.Center) {
                Row { Icon(Icons.Default.Call, null, tint = pubText, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.bookdetail_privacy_public), color = pubText, fontWeight = FontWeight.Bold) }
            }
            Box(Modifier.weight(1f).fillMaxHeight().clip(CircleShape).background(privCol).clickable { onToggle(false) }, Alignment.Center) {
                Row { Icon(Icons.Default.Lock, null, tint = privText, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.bookdetail_privacy_private), color = privText, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

/** Cuadro de diálogo para iniciar un hilo de comentarios en la comunidad (asociado a un capítulo). */
@Composable
fun PremiumCommentDialog(onDismiss: () -> Unit, onSubmit: (String, String) -> Unit) {
    var commentText by remember { mutableStateOf("") }
    var chapterText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.bookdetail_comment_dialog_title),
                    fontFamily = GuardianCity,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
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
                    text = stringResource(R.string.bookdetail_comment_dialog_desc),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )

                OutlinedTextField(
                    value = chapterText,
                    onValueChange = { chapterText = it },
                    label = { Text(stringResource(R.string.bookdetail_comment_chapter_optional)) },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )

                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    label = { Text(stringResource(R.string.bookdetail_comment_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 6,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(commentText, chapterText) },
                enabled = commentText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
            ) { Text(stringResource(R.string.bookdetail_action_publish), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.bookdetail_action_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 10.dp
    )
}