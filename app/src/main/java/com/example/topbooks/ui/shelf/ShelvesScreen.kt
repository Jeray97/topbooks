package com.example.topbooks.ui.shelf

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toAndroidRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.topbooks.data.model.Shelf
import com.example.topbooks.ui.theme.CenturyGotic
import com.example.topbooks.ui.theme.ColorArcDarkBrown
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import com.example.topbooks.ui.theme.ColorArcMediumBrown
import com.example.topbooks.ui.theme.ColorBackGroundGeneral
import com.example.topbooks.ui.theme.ColorTituloTopBooks
import com.example.topbooks.ui.theme.GuardianCity
import kotlin.math.abs

val SHELF_COLORS = listOf(
    0xFF8D5B4CL, 0xFFC89B8CL, 0xFFB9836BL, 0xFFD9AD9AL,
    0xFF6B8E23L, 0xFFCD853FL, 0xFF8B4513L, 0xFFA0522DL,
    0xFFDEB887L, 0xFFD2691EL, 0xFFBC8F8FL, 0xFFF4A460L,
    0xFF6B4226L, 0xFF4A708BL, 0xFF556B2FL, 0xFF8B668BL,
    0xFF7B3F00L, 0xFF2F4F4FL, 0xFF800000L, 0xFF483D8BL
)

private val WOOD_COLOR_LIGHT = Color(0xFFD4A574)
private val WOOD_COLOR_MEDIUM = Color(0xFFB8845A)
private val WOOD_COLOR_DARK = Color(0xFF8B5E3C)
private val WOOD_COLOR_SHADOW = Color(0xFF5C3A1E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelvesScreen(
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    viewModel: ShelfViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var shelfPositions by remember { mutableStateOf(mapOf<String, androidx.compose.ui.geometry.Rect>()) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.setContext(context)
    }

    Scaffold(
        containerColor = ColorBackGroundGeneral,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mis Estanterías",
                        fontFamily = GuardianCity,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTituloTopBooks
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = ColorArcDarkBrown)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            imageVector = if (state.viewMode == ViewMode.SPINES) Icons.Default.GridView else Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = "Cambiar vista",
                            tint = ColorArcDarkBrown
                        )
                    }
                    var showSortMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Ordenar", tint = ColorArcDarkBrown)
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Personalizado", fontFamily = CenturyGotic) },
                                onClick = { viewModel.updateSortBy(SortOption.CUSTOM); showSortMenu = false },
                                leadingIcon = {
                                    if (state.sortBy == SortOption.CUSTOM) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ColorArcMediumBrown)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Por título", fontFamily = CenturyGotic) },
                                onClick = { viewModel.updateSortBy(SortOption.TITLE); showSortMenu = false },
                                leadingIcon = {
                                    if (state.sortBy == SortOption.TITLE) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ColorArcMediumBrown)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Por autor", fontFamily = CenturyGotic) },
                                onClick = { viewModel.updateSortBy(SortOption.AUTHOR); showSortMenu = false },
                                leadingIcon = {
                                    if (state.sortBy == SortOption.AUTHOR) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ColorArcMediumBrown)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Por páginas", fontFamily = CenturyGotic) },
                                onClick = { viewModel.updateSortBy(SortOption.PAGES); showSortMenu = false },
                                leadingIcon = {
                                    if (state.sortBy == SortOption.PAGES) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ColorArcMediumBrown)
                                    }
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorBackGroundGeneral)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateDialog() },
                containerColor = ColorArcMediumBrown,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear estantería")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar en mis estanterías...", fontFamily = CenturyGotic) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ColorArcMediumBrown) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ColorArcMediumBrown,
                    unfocusedBorderColor = ColorArcMediumBrown.copy(alpha = 0.5f)
                )
            )

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ColorArcMediumBrown)
                }
            } else if (state.shelves.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📚", fontSize = 64.sp)
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = "Aún no tienes estanterías",
                            fontFamily = GuardianCity,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorArcDarkBrown
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Pulsa + para crear tu primera estantería",
                            fontFamily = CenturyGotic,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 20.dp)
                ) {
                    items(state.shelves.size) { index ->
                        val shelf = state.shelves[index]
                        val allBooks = state.shelfBooks[shelf.id] ?: emptyList()
                        val filteredBooks = viewModel.getFilteredBooks(allBooks)
                        val totalPages = viewModel.getTotalPages(filteredBooks)
                        val isDropTarget = state.dropTargetShelfId == shelf.id

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
                                .onGloballyPositioned { coordinates ->
                                    val position = coordinates.positionInWindow()
                                    val size = coordinates.size
                                    val rect = androidx.compose.ui.geometry.Rect(
                                        left = position.x,
                                        top = position.y,
                                        right = position.x + size.width,
                                        bottom = position.y + size.height
                                    )
                                    shelfPositions = shelfPositions + (shelf.id to rect)
                                }
                                .then(
                                    if (isDropTarget) {
                                        Modifier.background(ColorArcMediumBrown.copy(alpha = 0.2f))
                                    } else Modifier
                                )
                        ) {
                            ShelfRow(
                                shelf = shelf,
                                books = filteredBooks,
                                totalPages = totalPages,
                                viewMode = state.viewMode,
                                recentlyAddedBookId = state.recentlyAddedBookId,
                                draggingBook = state.draggingBook,
                                onBookClick = onBookClick,
                                onAddBook = { viewModel.showAddBookDialog(shelf.id) },
                                onEditShelf = { viewModel.showEditDialog(shelf) },
                                onDeleteShelf = { viewModel.deleteShelf(shelf.id) },
                                onRemoveBook = { bookId -> viewModel.removeBookFromShelf(shelf.id, bookId) },
                                onDragStart = { book -> viewModel.startDrag(book, shelf.id) },
                                onDragEnd = { viewModel.endDrag() },
                                onShareShelf = { viewModel.shareShelf(shelf, filteredBooks) },
                                onToggleVisibility = { isPublic -> viewModel.toggleShelfVisibility(shelf.id, isPublic) }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        if (state.draggingBook != null) {
            DragOverlay(
                book = state.draggingBook!!,
                shelfPositions = shelfPositions,
                onDropTargetChanged = { shelfId -> viewModel.updateDropTarget(shelfId) },
                onDrop = { viewModel.endDrag() },
                onCancel = { viewModel.cancelDrag() }
            )
        }
    }

    if (state.showCreateDialog) {
        CreateEditShelfDialog(
            existingShelf = state.editingShelf,
            onDismiss = { viewModel.hideCreateDialog() },
            onConfirm = { name, color -> viewModel.createShelf(name, color) }
        )
    }

    if (state.showAddBookDialog && state.selectedShelfId != null) {
        val shelfId = state.selectedShelfId!!
        val existingBookIds = state.shelves.find { it.id == shelfId }?.bookIds ?: emptyList()
        val availableBooks = state.allBooks.filter { it.id !in existingBookIds }

        MultiSelectAddBookDialog(
            availableBooks = availableBooks,
            onDismiss = { viewModel.hideAddBookDialog() },
            onAddBooks = { bookIds -> viewModel.addBooksToShelf(shelfId, bookIds) }
        )
    }
}

@Composable
fun ShelfRow(
    shelf: Shelf,
    books: List<ShelfBook>,
    totalPages: Int,
    viewMode: ViewMode,
    recentlyAddedBookId: String?,
    draggingBook: ShelfBook?,
    onBookClick: (String) -> Unit,
    onAddBook: () -> Unit,
    onEditShelf: () -> Unit,
    onDeleteShelf: () -> Unit,
    onRemoveBook: (String) -> Unit,
    onDragStart: (ShelfBook) -> Unit,
    onDragEnd: () -> Unit,
    onShareShelf: () -> Unit,
    onToggleVisibility: (Boolean) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var previewBook by remember { mutableStateOf<ShelfBook?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = shelf.name,
                        fontFamily = GuardianCity,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorArcDarkBrown
                    )
                    if (shelf.isPublic) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Pública",
                            tint = ColorArcMediumBrown,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
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
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = ColorArcDarkBrown)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Añadir libros", fontFamily = CenturyGotic) },
                        onClick = { showMenu = false; onAddBook() },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Editar estantería", fontFamily = CenturyGotic) },
                        onClick = { showMenu = false; onEditShelf() },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text(if (shelf.isPublic) "Hacer privada" else "Hacer pública", fontFamily = CenturyGotic) },
                        onClick = { showMenu = false; onToggleVisibility(!shelf.isPublic) },
                        leadingIcon = { Icon(Icons.Default.Public, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Compartir", fontFamily = CenturyGotic) },
                        onClick = { showMenu = false; onShareShelf() },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar", fontFamily = CenturyGotic, color = Color.Red) },
                        onClick = { showMenu = false; onDeleteShelf() },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (viewMode == ViewMode.SPINES) {
            WoodenShelf(
                books = books,
                recentlyAddedBookId = recentlyAddedBookId,
                draggingBook = draggingBook,
                onBookTap = { book -> previewBook = book },
                onRemoveBook = onRemoveBook,
                onDragStart = onDragStart,
                onDragEnd = onDragEnd
            )
        } else {
            GridShelf(
                books = books,
                onBookTap = { book -> previewBook = book }
            )
        }
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

@Composable
fun BookCoverPopup(
    book: ShelfBook,
    onDismiss: () -> Unit,
    onNavigate: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            var popupVisible by remember { mutableStateOf(false) }
            val popupAlpha by animateFloatAsState(
                targetValue = if (popupVisible) 1f else 0f,
                animationSpec = tween(250),
                label = "popupAlpha"
            )
            val popupScale by animateFloatAsState(
                targetValue = if (popupVisible) 1f else 0.8f,
                animationSpec = tween(250),
                label = "popupScale"
            )
            LaunchedEffect(Unit) { popupVisible = true }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .graphicsLayer {
                        this.alpha = popupAlpha
                        this.scaleX = popupScale
                        this.scaleY = popupScale
                    }
                    .clickable { onNavigate() }
                    .padding(24.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(16.dp),
                    modifier = Modifier.size(160.dp, 240.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(book.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = book.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = book.title,
                    fontFamily = GuardianCity,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                if (book.authors.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = book.authors.first(),
                        fontFamily = CenturyGotic,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }

                if (book.pageCount > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${book.pageCount} páginas",
                        fontFamily = CenturyGotic,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Toca para ver detalles",
                    fontFamily = CenturyGotic,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun WoodenShelf(
    books: List<ShelfBook>,
    recentlyAddedBookId: String?,
    draggingBook: ShelfBook?,
    onBookTap: (ShelfBook) -> Unit,
    onRemoveBook: (String) -> Unit,
    onDragStart: (ShelfBook) -> Unit,
    onDragEnd: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            if (books.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Estantería vacía — añade libros",
                        fontFamily = CenturyGotic,
                        fontSize = 14.sp,
                        color = Color.Gray.copy(alpha = 0.7f)
                    )
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    verticalAlignment = Alignment.Bottom,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                ) {
                    items(books.size) { index ->
                        val book = books[index]
                        val isRecentlyAdded = book.id == recentlyAddedBookId
                        val isDragging = draggingBook?.id == book.id

                        var bookVisible by remember { mutableStateOf(false) }
                        val bookAlpha by animateFloatAsState(
                            targetValue = if (bookVisible) 1f else 0f,
                            animationSpec = tween(durationMillis = 300, delayMillis = index * 50),
                            label = "bookAlpha"
                        )
                        val bookOffsetY by animateFloatAsState(
                            targetValue = if (bookVisible) 0f else 30f,
                            animationSpec = tween(durationMillis = 300, delayMillis = index * 50),
                            label = "bookOffset"
                        )
                        LaunchedEffect(Unit) { bookVisible = true }

                        val bounceScale by animateFloatAsState(
                            targetValue = if (isRecentlyAdded) 1f else 1.1f,
                            animationSpec = if (isRecentlyAdded) tween(600) else tween(200),
                            label = "bounceScale"
                        )

                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    this.alpha = if (isDragging) 0.3f else bookAlpha
                                    this.translationY = bookOffsetY
                                    this.scaleX = bounceScale
                                    this.scaleY = bounceScale
                                }
                        ) {
                            RealisticBookSpine(
                                book = book,
                                onClick = { onBookTap(book) },
                                onLongPressRemove = { onRemoveBook(book.id) },
                                onDragStart = { onDragStart(book) },
                                onDragEnd = onDragEnd
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .drawBehind {
                    val woodGradient = Brush.verticalGradient(
                        colors = listOf(
                            WOOD_COLOR_LIGHT,
                            WOOD_COLOR_MEDIUM,
                            WOOD_COLOR_DARK
                        ),
                        startY = 0f,
                        endY = size.height
                    )
                    drawRect(woodGradient)

                    drawRect(
                        color = WOOD_COLOR_SHADOW.copy(alpha = 0.3f),
                        topLeft = Offset(0f, 0f),
                        size = Size(size.width, 2f)
                    )

                    drawRect(
                        color = Color.White.copy(alpha = 0.15f),
                        topLeft = Offset(0f, 2f),
                        size = Size(size.width, 1f)
                    )

                    for (i in 0..3) {
                        val y = size.height * (0.2f + i * 0.2f)
                        drawRect(
                            color = WOOD_COLOR_DARK.copy(alpha = 0.08f),
                            topLeft = Offset(0f, y),
                            size = Size(size.width, 1f)
                        )
                    }
                }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                WOOD_COLOR_SHADOW.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
                }
        )
    }
}

@Composable
fun GridShelf(
    books: List<ShelfBook>,
    onBookTap: (ShelfBook) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        if (books.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Estantería vacía — añade libros",
                    fontFamily = CenturyGotic,
                    fontSize = 14.sp,
                    color = Color.Gray.copy(alpha = 0.7f)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)
            ) {
                items(books) { book ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clickable { onBookTap(book) },
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(book.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = book.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RealisticBookSpine(
    book: ShelfBook,
    onClick: () -> Unit,
    onLongPressRemove: () -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit
) {
    val baseColor = Color(book.spineColor)
    val darkerColor = baseColor.copy(
        red = (baseColor.red * 0.6f).coerceIn(0f, 1f),
        green = (baseColor.green * 0.6f).coerceIn(0f, 1f),
        blue = (baseColor.blue * 0.6f).coerceIn(0f, 1f)
    )
    val lighterColor = baseColor.copy(
        red = (baseColor.red * 1.3f).coerceIn(0f, 1f),
        green = (baseColor.green * 1.3f).coerceIn(0f, 1f),
        blue = (baseColor.blue * 1.3f).coerceIn(0f, 1f)
    )

    val spineWidth = when {
        book.pageCount > 600 -> 44.dp
        book.pageCount > 400 -> 38.dp
        book.pageCount > 250 -> 32.dp
        book.pageCount > 100 -> 26.dp
        else -> 22.dp
    }

    val spineHeight = when {
        book.pageCount > 500 -> 170.dp
        book.pageCount > 300 -> 160.dp
        book.pageCount > 150 -> 150.dp
        else -> 140.dp
    }

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "scale"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (isPressed) 0f else -4f,
        label = "offsetY"
    )

    var showRemoveDialog by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }

    val hasGoldBand = abs(book.id.hashCode()) % 3 == 0
    val hasDoubleBand = abs(book.id.hashCode()) % 5 == 0
    val bandColor = if (abs(book.id.hashCode()) % 2 == 0) Color(0xFFD4AF37) else Color(0xFFC0C0C0)

    Box(
        modifier = Modifier
            .width(spineWidth)
            .height(spineHeight)
            .offset(y = offsetY.dp)
            .scale(scale)
            .shadow(4.dp, RoundedCornerShape(2.dp))
            .drawBehind {
                val spineGradient = Brush.horizontalGradient(
                    colors = listOf(
                        darkerColor,
                        baseColor.copy(alpha = 0.9f),
                        lighterColor,
                        baseColor,
                        darkerColor
                    ),
                    startX = 0f,
                    endX = size.width
                )
                drawRect(spineGradient)

                drawRect(
                    color = Color.Black.copy(alpha = 0.2f),
                    topLeft = Offset(0f, 0f),
                    size = Size(2f, size.height)
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.15f),
                    topLeft = Offset(size.width - 2f, 0f),
                    size = Size(2f, size.height)
                )

                drawRect(
                    color = Color.White.copy(alpha = 0.1f),
                    topLeft = Offset(size.width * 0.3f, 0f),
                    size = Size(size.width * 0.15f, size.height)
                )

                if (hasGoldBand) {
                    val bandY = size.height * 0.12f
                    drawRect(
                        color = bandColor.copy(alpha = 0.7f),
                        topLeft = Offset(3f, bandY),
                        size = Size(size.width - 6f, 2f)
                    )
                    drawRect(
                        color = bandColor.copy(alpha = 0.5f),
                        topLeft = Offset(3f, bandY + 3f),
                        size = Size(size.width - 6f, 1f)
                    )
                }

                if (hasDoubleBand) {
                    val bandY2 = size.height * 0.88f
                    drawRect(
                        color = bandColor.copy(alpha = 0.7f),
                        topLeft = Offset(3f, bandY2),
                        size = Size(size.width - 6f, 2f)
                    )
                    drawRect(
                        color = bandColor.copy(alpha = 0.5f),
                        topLeft = Offset(3f, bandY2 - 3f),
                        size = Size(size.width - 6f, 1f)
                    )
                }

                val topCapY = size.height * 0.06f
                drawRect(
                    color = darkerColor.copy(alpha = 0.6f),
                    topLeft = Offset(0f, topCapY),
                    size = Size(size.width, 1f)
                )
                val bottomCapY = size.height * 0.94f
                drawRect(
                    color = darkerColor.copy(alpha = 0.6f),
                    topLeft = Offset(0f, bottomCapY),
                    size = Size(size.width, 1f)
                )
            }
            .clickable { onClick() }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        isDragging = true
                        onDragStart()
                    },
                    onDrag = { change, _ ->
                        change.consume()
                    },
                    onDragEnd = {
                        if (isDragging) {
                            isDragging = false
                            onDragEnd()
                        } else {
                            showRemoveDialog = true
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = book.title,
                color = Color.White,
                fontSize = if (spineWidth > 30.dp) 10.sp else 8.sp,
                fontFamily = CenturyGotic,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.rotate(-90f)
            )
        }
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("¿Quitar de la estantería?", fontFamily = GuardianCity, fontWeight = FontWeight.Bold) },
            text = { Text("¿Quieres quitar \"${book.title}\" de esta estantería?", fontFamily = CenturyGotic) },
            confirmButton = {
                TextButton(onClick = { showRemoveDialog = false; onLongPressRemove() }) {
                    Text("Quitar", color = Color.Red, fontFamily = CenturyGotic)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) { Text("Cancelar", fontFamily = CenturyGotic) }
            }
        )
    }
}

@Composable
fun CreateEditShelfDialog(
    existingShelf: Shelf?,
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit
) {
    var name by remember { mutableStateOf(existingShelf?.name ?: "") }
    var selectedColorIndex by remember { mutableIntStateOf(
        if (existingShelf != null) SHELF_COLORS.indexOf(existingShelf.color).coerceAtLeast(0) else 0
    ) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingShelf != null) "Editar estantería" else "Nueva estantería",
                fontFamily = GuardianCity,
                fontWeight = FontWeight.Bold,
                color = ColorArcDarkBrown
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre", fontFamily = CenturyGotic) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorArcMediumBrown,
                        focusedLabelColor = ColorArcMediumBrown
                    )
                )
                Spacer(Modifier.height(20.dp))
                Text("Color del estante", fontFamily = CenturyGotic, fontWeight = FontWeight.Bold, color = ColorArcDarkBrown)
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(SHELF_COLORS.size) { index ->
                        val color = Color(SHELF_COLORS[index])
                        val isSelected = index == selectedColorIndex
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 42.dp else 36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (isSelected) Modifier.border(3.dp, Color.White, CircleShape)
                                    else Modifier
                                )
                                .clickable { selectedColorIndex = index },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Text("✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), SHELF_COLORS[selectedColorIndex]) },
                colors = ButtonDefaults.buttonColors(containerColor = ColorArcMediumBrown),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (existingShelf != null) "Guardar" else "Crear", fontFamily = CenturyGotic, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", fontFamily = CenturyGotic) }
        }
    )
}

@Composable
fun MultiSelectAddBookDialog(
    availableBooks: List<ShelfBook>,
    onDismiss: () -> Unit,
    onAddBooks: (List<String>) -> Unit
) {
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Añadir libros",
                    fontFamily = GuardianCity,
                    fontWeight = FontWeight.Bold,
                    color = ColorArcDarkBrown,
                    modifier = Modifier.weight(1f)
                )
                if (selectedIds.isNotEmpty()) {
                    Text(
                        text = "${selectedIds.size} seleccionados",
                        fontFamily = CenturyGotic,
                        fontSize = 12.sp,
                        color = ColorArcMediumBrown
                    )
                }
            }
        },
        text = {
            if (availableBooks.isEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "📖", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "No tienes libros disponibles",
                        fontFamily = CenturyGotic,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Marca libros como leídos o pendientes primero.",
                        fontFamily = CenturyGotic,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { selectedIds = availableBooks.map { it.id }.toSet() },
                            enabled = selectedIds.size < availableBooks.size
                        ) {
                            Text("Seleccionar todos", fontFamily = CenturyGotic, fontSize = 12.sp)
                        }
                        TextButton(
                            onClick = { selectedIds = emptySet() },
                            enabled = selectedIds.isNotEmpty()
                        ) {
                            Text("Deseleccionar", fontFamily = CenturyGotic, fontSize = 12.sp)
                        }
                    }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(availableBooks) { book ->
                            val isSelected = book.id in selectedIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) ColorArcMediumBrown.copy(alpha = 0.15f)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        selectedIds = if (isSelected) {
                                            selectedIds - book.id
                                        } else {
                                            selectedIds + book.id
                                        }
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSelected) ColorArcMediumBrown else Color.Gray,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .size(28.dp, 40.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .drawBehind {
                                            val baseColor = Color(book.spineColor)
                                            val darker = baseColor.copy(
                                                red = (baseColor.red * 0.6f).coerceIn(0f, 1f),
                                                green = (baseColor.green * 0.6f).coerceIn(0f, 1f),
                                                blue = (baseColor.blue * 0.6f).coerceIn(0f, 1f)
                                            )
                                            val lighter = baseColor.copy(
                                                red = (baseColor.red * 1.3f).coerceIn(0f, 1f),
                                                green = (baseColor.green * 1.3f).coerceIn(0f, 1f),
                                                blue = (baseColor.blue * 1.3f).coerceIn(0f, 1f)
                                            )
                                            drawRect(
                                                brush = Brush.horizontalGradient(
                                                    colors = listOf(darker, baseColor, lighter, baseColor, darker)
                                                )
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = book.title.take(1),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = book.title,
                                        fontFamily = CenturyGotic,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = ColorArcDarkBrown
                                    )
                                    if (book.authors.isNotEmpty()) {
                                        Text(
                                            text = book.authors.first(),
                                            fontFamily = CenturyGotic,
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (selectedIds.isNotEmpty()) onAddBooks(selectedIds.toList()) },
                enabled = selectedIds.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = ColorArcMediumBrown),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (selectedIds.isEmpty()) "Selecciona libros" else "Añadir ${selectedIds.size}",
                    fontFamily = CenturyGotic,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", fontFamily = CenturyGotic) }
        }
    )
}

@Composable
fun DragOverlay(
    book: ShelfBook,
    shelfPositions: Map<String, androidx.compose.ui.geometry.Rect>,
    onDropTargetChanged: (String?) -> Unit,
    onDrop: () -> Unit,
    onCancel: () -> Unit
) {
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    val baseColor = Color(book.spineColor)
    val darkerColor = baseColor.copy(
        red = (baseColor.red * 0.6f).coerceIn(0f, 1f),
        green = (baseColor.green * 0.6f).coerceIn(0f, 1f),
        blue = (baseColor.blue * 0.6f).coerceIn(0f, 1f)
    )
    val lighterColor = baseColor.copy(
        red = (baseColor.red * 1.3f).coerceIn(0f, 1f),
        green = (baseColor.green * 1.3f).coerceIn(0f, 1f),
        blue = (baseColor.blue * 1.3f).coerceIn(0f, 1f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        dragPosition = startOffset
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragPosition += dragAmount

                        val x = dragPosition.x
                        val y = dragPosition.y
                        val targetShelf = shelfPositions.entries.find { entry ->
                            val rect = entry.value
                            x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom
                        }?.key

                        onDropTargetChanged(targetShelf)
                    },
                    onDragEnd = {
                        onDrop()
                    },
                    onDragCancel = {
                        onCancel()
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(dragPosition.x.toInt() - 15, dragPosition.y.toInt() - 70) }
                .width(30.dp)
                .height(140.dp)
                .shadow(12.dp, RoundedCornerShape(2.dp))
                .drawBehind {
                    val spineGradient = Brush.horizontalGradient(
                        colors = listOf(
                            darkerColor,
                            baseColor.copy(alpha = 0.9f),
                            lighterColor,
                            baseColor,
                            darkerColor
                        ),
                        startX = 0f,
                        endX = size.width
                    )
                    drawRect(spineGradient)
                }
                .rotate(-5f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = book.title,
                color = Color.White,
                fontSize = 9.sp,
                fontFamily = CenturyGotic,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.rotate(-90f)
            )
        }
    }
}
