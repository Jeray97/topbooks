package com.example.topbooks.ui.shelf

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Shelf
import com.example.topbooks.data.model.ShelfBookMeta
import com.example.topbooks.data.repository.BooksRepository
import com.example.topbooks.data.repository.ProgressRepository
import com.example.topbooks.data.repository.ProgressRepositoryImpl
import com.example.topbooks.data.repository.ShelfRepository
import com.example.topbooks.data.repository.ShelfRepositoryImpl
import com.example.topbooks.data.repository.UserRepository
import com.example.topbooks.data.repository.UserRepositoryImpl
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class ShelfBook(
    val id: String = "",
    val title: String = "",
    val imageUrl: String = "",
    val pageCount: Int = 0,
    val authors: List<String> = emptyList(),
    val spineColor: Long = 0xFF8D5B4C
)

data class ShelvesState(
    val shelves: List<Shelf> = emptyList(),
    val shelfBooks: Map<String, List<ShelfBook>> = emptyMap(),
    val allBooks: List<ShelfBook> = emptyList(),
    val isLoading: Boolean = true,
    val showCreateDialog: Boolean = false,
    val showAddBookDialog: Boolean = false,
    val selectedShelfId: String? = null,
    val editingShelf: Shelf? = null,
    val searchQuery: String = "",
    val sortBy: SortOption = SortOption.CUSTOM,
    val viewMode: ViewMode = ViewMode.SPINES,
    val perspectiveMode: PerspectiveMode = PerspectiveMode.ORTHO,
    val recentlyAddedBookId: String? = null,
    val draggingBook: ShelfBook? = null,
    val draggingFromShelfId: String? = null,
    val dropTargetShelfId: String? = null
)

enum class SortOption { CUSTOM, TITLE, AUTHOR, PAGES }
enum class ViewMode { SPINES, COVERS, MIXED }
enum class PerspectiveMode { FLAT, ORTHO, ISO }

class ShelfViewModel(
    private val shelfRepo: ShelfRepository = ShelfRepositoryImpl(),
    private val userRepo: UserRepository = UserRepositoryImpl(),
    private val booksRepo: BooksRepository = BooksRepository(),
    private val progressRepo: ProgressRepository = ProgressRepositoryImpl()
) : ViewModel() {

    private var context: Context? = null

    fun setContext(context: Context) {
        this.context = context
    }

    private val _uiState = MutableStateFlow(ShelvesState())
    val uiState: StateFlow<ShelvesState> = _uiState.asStateFlow()

    init {
        loadShelves()
    }

    fun loadShelves() {
        val uid = userRepo.getCurrentUserId() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val shelves = shelfRepo.getShelves(uid).getOrDefault(emptyList())

            val booksMap = mutableMapOf<String, List<ShelfBook>>()
            for (shelf in shelves) {
                booksMap[shelf.id] = shelf.bookIds.mapNotNull { bookId ->
                    val meta = shelf.bookMetadata[bookId] ?: return@mapNotNull null
                    ShelfBook(
                        id = bookId,
                        title = meta.title,
                        imageUrl = meta.imageUrl,
                        pageCount = meta.pageCount,
                        authors = meta.authors,
                        spineColor = generateSpineColor(bookId)
                    )
                }
            }

            _uiState.update {
                it.copy(
                    shelves = shelves,
                    shelfBooks = booksMap,
                    isLoading = false
                )
            }

            val allUserBooks = loadAllUserBooks()
            _uiState.update { it.copy(allBooks = allUserBooks) }
        }
    }

    private suspend fun loadAllUserBooks(): List<ShelfBook> = coroutineScope {
        val uid = userRepo.getCurrentUserId() ?: return@coroutineScope emptyList()

        val readDeferred = async { progressRepo.getReadBooks(uid).getOrDefault(emptyList()) }
        val bookmarksDeferred = async { progressRepo.getBookmarks(uid).getOrDefault(emptyList()) }

        val readBooks = readDeferred.await()
        val bookmarks = bookmarksDeferred.await()

        val seen = mutableSetOf<String>()
        val allSimple: MutableList<Pair<String, String>> = mutableListOf()

        for (book in readBooks) {
            if (seen.add(book.id)) {
                allSimple.add(book.id to book.title)
            }
        }
        for (bookmark in bookmarks) {
            if (seen.add(bookmark.bookId)) {
                allSimple.add(bookmark.bookId to "")
            }
        }

        allSimple.map { pair: Pair<String, String> ->
            async {
                val id: String = pair.first
                val fallbackTitle: String = pair.second
                val detail = booksRepo.getBookDetail(id).getOrNull()
                ShelfBook(
                    id = id,
                    title = detail?.title ?: fallbackTitle,
                    imageUrl = detail?.imageUrl ?: "",
                    pageCount = detail?.pageCount ?: 0,
                    authors = detail?.authors ?: emptyList(),
                    spineColor = generateSpineColor(id)
                )
            }
        }.awaitAll()
    }

    private fun generateSpineColor(bookId: String): Long {
        val hash = bookId.hashCode()
        val colors = listOf(
            0xFF8D5B4CL, 0xFFC89B8CL, 0xFFB9836BL, 0xFFD9AD9AL,
            0xFF6B8E23L, 0xFFCD853FL, 0xFF8B4513L, 0xFFA0522DL,
            0xFFDEB887L, 0xFFD2691EL, 0xFFBC8F8FL, 0xFFF4A460L,
            0xFF6B4226L, 0xFF4A708BL, 0xFF556B2FL, 0xFF8B668BL,
            0xFF7B3F00L, 0xFF2F4F4FL, 0xFF800000L, 0xFF483D8BL
        )
        return colors[Math.abs(hash) % colors.size]
    }

    fun showCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true, editingShelf = null) }
    }

    fun showEditDialog(shelf: Shelf) {
        _uiState.update { it.copy(showCreateDialog = true, editingShelf = shelf) }
    }

    fun hideCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false, editingShelf = null) }
    }

    fun showAddBookDialog(shelfId: String) {
        _uiState.update { it.copy(showAddBookDialog = true, selectedShelfId = shelfId) }
    }

    fun hideAddBookDialog() {
        _uiState.update { it.copy(showAddBookDialog = false, selectedShelfId = null) }
    }

    fun createShelf(name: String, color: Long) {
        viewModelScope.launch {
            val editing = _uiState.value.editingShelf
            if (editing != null) {
                shelfRepo.updateShelf(editing.copy(name = name, color = color))
            } else {
                shelfRepo.createShelf(name, color)
            }
            hideCreateDialog()
            loadShelves()
        }
    }

    fun deleteShelf(shelfId: String) {
        viewModelScope.launch {
            shelfRepo.deleteShelf(shelfId)
            loadShelves()
        }
    }

    fun addBookToShelf(shelfId: String, bookId: String) {
        viewModelScope.launch {
            val book = _uiState.value.allBooks.find { it.id == bookId }
            val meta = ShelfBookMeta(
                title = book?.title ?: "",
                imageUrl = book?.imageUrl ?: "",
                pageCount = book?.pageCount ?: 0,
                authors = book?.authors ?: emptyList()
            )
            shelfRepo.addBookToShelf(shelfId, bookId, meta)
            hideAddBookDialog()
            loadShelves()
        }
    }

    fun addBooksToShelf(shelfId: String, bookIds: List<String>) {
        viewModelScope.launch {
            val allBooks = _uiState.value.allBooks
            for (bookId in bookIds) {
                val book = allBooks.find { it.id == bookId }
                val meta = ShelfBookMeta(
                    title = book?.title ?: "",
                    imageUrl = book?.imageUrl ?: "",
                    pageCount = book?.pageCount ?: 0,
                    authors = book?.authors ?: emptyList()
                )
                shelfRepo.addBookToShelf(shelfId, bookId, meta)
            }
            hideAddBookDialog()
            loadShelves()
            if (bookIds.isNotEmpty()) {
                highlightRecentlyAdded(bookIds.last())
            }
        }
    }

    fun removeBookFromShelf(shelfId: String, bookId: String) {
        viewModelScope.launch {
            shelfRepo.removeBookFromShelf(shelfId, bookId)
            loadShelves()
        }
    }

    fun moveBook(fromShelfId: String, toShelfId: String, bookId: String, toIndex: Int) {
        viewModelScope.launch {
            shelfRepo.moveBook(fromShelfId, toShelfId, bookId, toIndex)
            loadShelves()
        }
    }

    fun reorderBooks(shelfId: String, bookIds: List<String>) {
        viewModelScope.launch {
            shelfRepo.reorderBooks(shelfId, bookIds)
            loadShelves()
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun updateSortBy(sort: SortOption) {
        _uiState.update { it.copy(sortBy = sort) }
    }

    fun toggleViewMode() {
        _uiState.update {
            val next = when (it.viewMode) {
                ViewMode.SPINES -> ViewMode.COVERS
                ViewMode.COVERS -> ViewMode.MIXED
                ViewMode.MIXED -> ViewMode.SPINES
            }
            it.copy(viewMode = next)
        }
    }

    fun setViewMode(mode: ViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun setPerspectiveMode(mode: PerspectiveMode) {
        _uiState.update { it.copy(perspectiveMode = mode) }
    }

    fun cyclePerspective() {
        _uiState.update {
            val next = when (it.perspectiveMode) {
                PerspectiveMode.FLAT -> PerspectiveMode.ORTHO
                PerspectiveMode.ORTHO -> PerspectiveMode.ISO
                PerspectiveMode.ISO -> PerspectiveMode.FLAT
            }
            it.copy(perspectiveMode = next)
        }
    }

    fun getFilteredBooks(books: List<ShelfBook>): List<ShelfBook> {
        val query = _uiState.value.searchQuery.lowercase()
        val filtered = if (query.isBlank()) books else books.filter {
            it.title.lowercase().contains(query) ||
            it.authors.any { author -> author.lowercase().contains(query) }
        }

        return when (_uiState.value.sortBy) {
            SortOption.CUSTOM -> filtered
            SortOption.TITLE -> filtered.sortedBy { it.title }
            SortOption.AUTHOR -> filtered.sortedBy { it.authors.firstOrNull() ?: "" }
            SortOption.PAGES -> filtered.sortedByDescending { it.pageCount }
        }
    }

    fun getTotalPages(books: List<ShelfBook>): Int {
        return books.sumOf { it.pageCount }
    }

    private fun highlightRecentlyAdded(bookId: String) {
        _uiState.update { it.copy(recentlyAddedBookId = bookId) }
        viewModelScope.launch {
            kotlinx.coroutines.delay(1500)
            _uiState.update { it.copy(recentlyAddedBookId = null) }
        }
    }

    fun startDrag(book: ShelfBook, fromShelfId: String) {
        _uiState.update { it.copy(draggingBook = book, draggingFromShelfId = fromShelfId, dropTargetShelfId = null) }
    }

    fun updateDropTarget(shelfId: String?) {
        _uiState.update { it.copy(dropTargetShelfId = shelfId) }
    }

    fun endDrag() {
        val state = _uiState.value
        val book = state.draggingBook
        val fromShelf = state.draggingFromShelfId
        val toShelf = state.dropTargetShelfId

        if (book != null && fromShelf != null && toShelf != null && fromShelf != toShelf) {
            moveBook(fromShelf, toShelf, book.id, 0)
        }

        _uiState.update { it.copy(draggingBook = null, draggingFromShelfId = null, dropTargetShelfId = null) }
    }

    fun cancelDrag() {
        _uiState.update { it.copy(draggingBook = null, draggingFromShelfId = null, dropTargetShelfId = null) }
    }

    fun toggleShelfVisibility(shelfId: String, isPublic: Boolean) {
        viewModelScope.launch {
            shelfRepo.toggleShelfVisibility(shelfId, isPublic)
            loadShelves()
        }
    }

    fun shareShelf(shelf: Shelf, books: List<ShelfBook>) {
        val ctx = context ?: return
        
        viewModelScope.launch {
            val bitmap = generateShelfImage(shelf, books)
            
            val cachePath = File(ctx.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "shelf_${shelf.id}.png")
            
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            
            val uri = FileProvider.getUriForFile(
                ctx,
                "${ctx.packageName}.fileprovider",
                file
            )
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Mi estantería '${shelf.name}' con ${books.size} libros en TopBooks")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            ctx.startActivity(Intent.createChooser(shareIntent, "Compartir estantería").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    private fun generateShelfImage(shelf: Shelf, books: List<ShelfBook>): Bitmap {
        val width = 800
        val height = 400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        canvas.drawColor(android.graphics.Color.parseColor("#F6E6DD"))
        
        val titlePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#8D5B4C")
            textSize = 48f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText(shelf.name, 40f, 60f, titlePaint)
        
        val subtitlePaint = Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 24f
            isAntiAlias = true
        }
        canvas.drawText("${books.size} libros", 40f, 95f, subtitlePaint)
        
        val shelfY = height - 60f
        val woodPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#B8845A")
            isAntiAlias = true
        }
        canvas.drawRect(20f, shelfY, width - 20f, shelfY + 20f, woodPaint)
        
        var currentX = 40f
        val bookBaseY = shelfY - 10f
        
        books.take(20).forEach { book ->
            val bookWidth = when {
                book.pageCount > 400 -> 50f
                book.pageCount > 200 -> 40f
                else -> 35f
            }
            val bookHeight = when {
                book.pageCount > 400 -> 180f
                book.pageCount > 200 -> 160f
                else -> 140f
            }
            
            val color = android.graphics.Color.parseColor(
                String.format("#%06X", 0xFFFFFF and book.spineColor.toInt())
            )
            
            val bookPaint = Paint().apply {
                this.color = color
                isAntiAlias = true
            }
            canvas.drawRect(currentX, bookBaseY - bookHeight, currentX + bookWidth, bookBaseY, bookPaint)
            
            val shadowPaint = Paint().apply {
                this.color = android.graphics.Color.BLACK
                alpha = 50
                isAntiAlias = true
            }
            canvas.drawRect(currentX, bookBaseY - bookHeight, currentX + 3f, bookBaseY, shadowPaint)
            
            if (currentX + bookWidth < width - 60f) {
                val textPaint = Paint().apply {
                    this.color = android.graphics.Color.WHITE
                    textSize = 18f
                    isAntiAlias = true
                }
                
                canvas.save()
                canvas.rotate(-90f, currentX + bookWidth / 2, bookBaseY - bookHeight / 2)
                val title = if (book.title.length > 20) book.title.take(17) + "..." else book.title
                canvas.drawText(title, currentX + bookWidth / 2 - textPaint.measureText(title) / 2, bookBaseY - bookHeight / 2 + 6f, textPaint)
                canvas.restore()
            }
            
            currentX += bookWidth + 5f
        }
        
        return bitmap
    }
}
