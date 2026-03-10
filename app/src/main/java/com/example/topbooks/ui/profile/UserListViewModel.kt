package com.example.topbooks.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Comment
import com.example.topbooks.data.model.Journal
import com.example.topbooks.data.model.Review
import com.example.topbooks.data.repository.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Modelos simplificados para la representación visual en listas genéricas */
data class SimpleUser(val uid: String = "", val name: String = "", val photo: String = "")
data class SimpleBook(val id: String = "", val title: String = "", val imageUrl: String = "")
data class BookmarkUI(val id: String = "", val bookId: String = "", val bookTitle: String = "", val quote: String = "", val chapter: String = "", val page: String = "", val isPublic: Boolean = true)

/** * Estado global de la pantalla de listados.
 * Contiene todas las colecciones posibles que el usuario puede consultar.
 */
data class UserListState(
    val friends: List<SimpleUser> = emptyList(),
    val readBooks: List<SimpleBook> = emptyList(),
    val reviews: List<Review> = emptyList(),
    val pendingBooks: List<SimpleBook> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val journals: List<Journal> = emptyList(),
    val bookmarks: List<BookmarkUI> = emptyList(),
    val favorites: List<SimpleBook> = emptyList(),
    val isLoading: Boolean = false
)

/**
 * ViewModel encargado de gestionar y cargar diversas listas de datos del usuario.
 * Actúa como un orquestador entre múltiples repositorios para centralizar la lógica de consulta.
 */
class UserListViewModel(
    private val progressRepo: ProgressRepository = ProgressRepositoryImpl(),
    private val feedRepo: SocialFeedRepository = SocialFeedRepositoryImpl(),
    private val communityRepo: CommunityRepository = CommunityRepositoryImpl(),
    private val userRepo: UserRepository = UserRepositoryImpl(),
    private val booksRepo: BooksRepository = BooksRepository(),
    private val journalRepo: JournalRepository = JournalRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserListState())
    val uiState: StateFlow<UserListState> = _uiState.asStateFlow()

    // Referencias para poder refrescar la lista actual tras una eliminación
    private var currentListType: String = ""
    private var currentUserId: String = ""

    /**
     * Punto de entrada principal para cargar cualquier tipo de lista soportada.
     * @param listType El identificador del tipo de lista a cargar.
     * @param userId El ID del usuario propietario de los datos.
     */
    fun loadList(listType: String, userId: String) {
        currentListType = listType
        currentUserId = userId

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (listType) {
                "friends" -> fetchFriends(userId)
                "read" -> fetchReadBooks(userId)
                "pending" -> fetchPendingBooks(userId)
                "reviews" -> fetchReviews(userId)
                "comments" -> fetchComments(userId)
                "bookmarks" -> fetchBookmarks(userId)
                "favorites" -> fetchFavorites(userId)
                "journals" -> fetchJournals(userId)
            }
        }
    }

    /** Carga la lista de amigos obteniendo sus perfiles completos en paralelo. */
    private suspend fun fetchFriends(userId: String) = coroutineScope {
        val ids = communityRepo.getMyFriendsIds().getOrDefault(emptySet())
        val deferred = ids.map { friendId ->
            async { userRepo.getUserProfile(friendId).getOrNull() }
        }
        val users = deferred.awaitAll().filterNotNull().map { user ->
            SimpleUser(user.uid, user.displayName, user.photoURL)
        }
        _uiState.update { it.copy(friends = users, isLoading = false) }
    }

    /** Carga los libros marcados como terminados por el usuario. */
    private suspend fun fetchReadBooks(userId: String) {
        val books = progressRepo.getReadBooks(userId).getOrDefault(emptyList())
        _uiState.update { it.copy(readBooks = books, isLoading = false) }
    }

    /** Carga los libros favoritos cruzando los IDs con sus portadas almacenadas. */
    private suspend fun fetchFavorites(userId: String) {
        val covers = userRepo.getFavoriteCovers(userId, 50).getOrDefault(emptyList())
        val ids = userRepo.getFavoriteIds(userId).getOrDefault(emptySet()).toList()
        val favs = ids.zip(covers).map { SimpleBook(id = it.first, imageUrl = it.second) }
        _uiState.update { it.copy(favorites = favs, isLoading = false) }
    }

    /** Carga y enriquece las reseñas del usuario con datos de la API de libros. */
    private suspend fun fetchReviews(userId: String) {
        val reviewsList = feedRepo.getUserReviews(userId).getOrDefault(emptyList())
        val enriched = reviewsList.map { r ->
            val book = booksRepo.getBookDetail(r.bookId).getOrNull()
            if (book != null) r.copy(bookTitle = book.title, bookImageUrl = book.imageUrl) else r
        }
        _uiState.update { it.copy(reviews = enriched, isLoading = false) }
    }

    /** Carga y enriquece los comentarios del usuario con datos de la API de libros. */
    private suspend fun fetchComments(userId: String) {
        val commentsList = feedRepo.getUserComments(userId).getOrDefault(emptyList())
        val enriched = commentsList.map { c ->
            val book = booksRepo.getBookDetail(c.bookId).getOrNull()
            if (book != null) c.copy(bookTitle = book.title, bookImageUrl = book.imageUrl) else c
        }
        _uiState.update { it.copy(comments = enriched, isLoading = false) }
    }

    /** Carga los diarios de lectura y busca la información visual del libro asociado. */
    private suspend fun fetchJournals(userId: String) {
        val journalsList = journalRepo.getAllJournals(userId).getOrDefault(emptyList())
        val enriched = journalsList.map { j ->
            // Evitamos buscar en la API si el ID no es válido o es local
            if (j.bookId.length > 20) return@map j

            val book = booksRepo.getBookDetail(j.bookId).getOrNull()
            if (book != null) j.copy(bookTitle = book.title, bookImageUrl = book.imageUrl) else j
        }
        _uiState.update { it.copy(journals = enriched, isLoading = false) }
    }

    // MODIFICADO: Ahora filtramos los "marcadores vacíos" que pertenecen a pendientes
    private suspend fun fetchBookmarks(userId: String) {
        val marks = progressRepo.getBookmarks(userId).getOrDefault(emptyList())

        // Solo conservamos los que tengan algún contenido real escrito por el usuario
        val realBookmarks = marks.filter {
            it.quote.isNotBlank() || it.chapter.isNotBlank() || it.page.isNotBlank()
        }

        val enriched = realBookmarks.map { b ->
            val book = booksRepo.getBookDetail(b.bookId).getOrNull()
            if (book != null) b.copy(bookTitle = book.title) else b
        }
        _uiState.update { it.copy(bookmarks = enriched, isLoading = false) }
    }

    /** Carga los libros en la lista de "Pendientes" (marcadores sin contenido). */
    private suspend fun fetchPendingBooks(userId: String) {
        val marks = progressRepo.getBookmarks(userId).getOrDefault(emptyList())
        val pending = marks.map { b ->
            val book = booksRepo.getBookDetail(b.bookId).getOrNull()
            SimpleBook(id = b.bookId, title = book?.title ?: b.bookTitle, imageUrl = book?.imageUrl ?: "")
        }
        _uiState.update { it.copy(pendingBooks = pending, isLoading = false) }
    }

    // --- FUNCIONES DE ELIMINACIÓN Y ACTUALIZACIÓN ---

    fun deleteJournal(bookId: String) {
        viewModelScope.launch {
            journalRepo.deleteJournal(bookId)
            loadList(currentListType, currentUserId)
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            progressRepo.deleteDocument("comments", commentId)
            loadList(currentListType, currentUserId)
        }
    }

    fun deleteReview(reviewId: String) {
        viewModelScope.launch {
            progressRepo.deleteDocument("reviews", reviewId)
            loadList(currentListType, currentUserId)
        }
    }

    fun removeBookmark(bookId: String) {
        viewModelScope.launch {
            progressRepo.deleteUserSubdocument("bookmarks", bookId)
            loadList(currentListType, currentUserId)
        }
    }

    fun removeFavorite(bookId: String) {
        viewModelScope.launch {
            progressRepo.deleteUserSubdocument("favorites", bookId)
            loadList(currentListType, currentUserId)
        }
    }

    fun removeReadBook(bookId: String) {
        viewModelScope.launch {
            progressRepo.deleteUserSubdocument("read_books", bookId)
            loadList(currentListType, currentUserId)
        }
    }

    /** Actualiza los datos de un marcador existente en el repositorio. */
    fun updateBookmark(bookmark: BookmarkUI) {
        viewModelScope.launch {
            try {
                val updatedData = mapOf(
                    "page" to bookmark.page,
                    "chapter" to bookmark.chapter,
                    "quote" to bookmark.quote,
                    "isPublic" to bookmark.isPublic
                )
                progressRepo.updateUserSubdocument("bookmarks", bookmark.bookId, updatedData)
                loadList(currentListType, currentUserId)
            } catch (e: Exception) {
                Log.e("UserListVM", "Error al actualizar marcador: ${e.message}")
            }
        }
    }
}