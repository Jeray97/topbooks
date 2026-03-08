package com.example.topbooks.ui.book

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.model.Review
import com.example.topbooks.data.repository.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookDetailState(
    val book: Book? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFavorite: Boolean = false,
    val savedInList: String? = null,
    val reviews: List<Review> = emptyList()
)

class BookDetailViewModel(
    private val booksRepository: BooksRepository = BooksRepository(),
    private val progressRepository: ProgressRepository = ProgressRepositoryImpl(),
    private val userRepository: UserRepository = UserRepositoryImpl(),
    private val authRepository: AuthRepository = AuthRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookDetailState())
    val uiState: StateFlow<BookDetailState> = _uiState.asStateFlow()

    fun loadBook(bookId: String) {
        viewModelScope.launch {

            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = booksRepository.getBookDetail(bookId)

            if (result.isSuccess) {

                val book = result.getOrNull()

                _uiState.update {
                    it.copy(book = book, isLoading = false)
                }

                checkUserLists(bookId)

            } else {
                _uiState.update {
                    it.copy(error = result.exceptionOrNull()?.message, isLoading = false)
                }
            }
        }
    }

    private fun checkUserLists(bookId: String) {
        val uid = userRepository.getCurrentUserId() ?: return

        viewModelScope.launch {
            try {

                val readDeferred = async { progressRepository.getReadBooks(uid).getOrDefault(emptyList()) }
                val favsDeferred = async { userRepository.getFavoriteIds(uid).getOrDefault(emptyList()) }
                val marksDeferred = async { progressRepository.getBookmarks(uid).getOrDefault(emptyList()) }

                val read = readDeferred.await()
                val favs = favsDeferred.await()
                val marks = marksDeferred.await()

                val isFav = favs.contains(bookId)

                val list = when {
                    read.any { it.id == bookId } -> "Leídos"
                    marks.any { it.bookId == bookId } -> "Pendientes"
                    else -> null
                }

                _uiState.update { it.copy(isFavorite = isFav, savedInList = list) }

            } catch (e: Exception) {
                Log.e("BookDetailVM", "Error comprobando listas: ${e.message}")
            }
        }
    }

    fun toggleFavorite(book: Book) {

        val currentState = _uiState.value.isFavorite
        val newState = !currentState

        _uiState.update { it.copy(isFavorite = newState) }

        viewModelScope.launch {

            try {

                // 🔥 Guardamos el libro si no existe
                booksRepository.ensureBookExists(book)

                if (newState) {
                    progressRepository.toggleFavorite(book, true)
                } else {
                    progressRepository.deleteUserSubdocument("favorites", book.id)
                }

            } catch (e: Exception) {

                Log.e("BookDetailVM", "Error al cambiar favorito: ${e.message}")
                _uiState.update { it.copy(isFavorite = currentState) }
            }
        }
    }

    fun checkEmailVerification(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            authRepository.reloadUser()
            onResult(authRepository.isEmailVerified())
        }
    }

    fun addToList(book: Book, listName: String) {

        viewModelScope.launch {

            try {

                // 🔥 Guardamos el libro
                booksRepository.ensureBookExists(book)

                if (listName == "Leídos") {

                    progressRepository.deleteUserSubdocument("bookmarks", book.id)
                    progressRepository.markAsRead(book)

                    _uiState.update { it.copy(savedInList = "Leídos") }

                } else if (listName == "Pendientes") {

                    progressRepository.deleteUserSubdocument("read_books", book.id)
                    progressRepository.saveBookmark(book, "", "", "", false)

                    _uiState.update { it.copy(savedInList = "Pendientes") }
                }

            } catch (e: Exception) {
                Log.e("BookDetailVM", "Error añadiendo a lista: ${e.message}")
            }
        }
    }

    fun removeFromList(bookId: String, listName: String) {

        viewModelScope.launch {

            val collection = when (listName) {
                "Leídos" -> "read_books"
                "Pendientes" -> "bookmarks"
                else -> return@launch
            }

            progressRepository.deleteUserSubdocument(collection, bookId)

            _uiState.update { it.copy(savedInList = null) }
        }
    }

    fun saveReview(book: Book, rating: Int, text: String, onSuccess: () -> Unit) {

        viewModelScope.launch {

            booksRepository.ensureBookExists(book)

            progressRepository.saveReview(book, rating, text)
                .onSuccess { onSuccess() }
        }
    }

    fun saveComment(book: Book, text: String, chapter: String, onSuccess: () -> Unit) {

        viewModelScope.launch {

            booksRepository.ensureBookExists(book)

            progressRepository.saveComment(book, text, chapter)
                .onSuccess { onSuccess() }
        }
    }

    fun saveBookmark(
        book: Book,
        page: String,
        quote: String,
        chapter: String,
        isPublic: Boolean,
        onSuccess: () -> Unit
    ) {

        viewModelScope.launch {

            booksRepository.ensureBookExists(book)

            progressRepository.deleteUserSubdocument("read_books", book.id)

            progressRepository.saveBookmark(book, quote, chapter, page, isPublic)
                .onSuccess {

                    _uiState.update { it.copy(savedInList = "Pendientes") }
                    onSuccess()
                }
        }
    }

    fun editSeries(newName: String, newIndex: Int, onSuccess: () -> Unit) {
        val currentBook = _uiState.value.book ?: return
        val currentUser = authRepository.currentUser ?: return

        viewModelScope.launch {
            // Obtenemos los datos bonitos del usuario desde tu UserRepository
            val userProfile = userRepository.getUserProfile(currentUser.uid).getOrNull()
            val editorName = userProfile?.displayName ?: "Usuario"
            val editorAvatar = userProfile?.photoURL ?: "capibara_1"

            booksRepository.updateBookSeries(
                book = currentBook,
                newName = newName,
                newIndex = newIndex,
                editorUid = currentUser.uid,
                editorName = editorName,
                editorAvatar = editorAvatar
            ).onSuccess {
                // Actualizamos la UI localmente para no tener que recargar de internet
                _uiState.update {
                    it.copy(book = currentBook.copy(
                        seriesName = newName, seriesIndex = newIndex,
                        seriesEditorName = editorName, seriesEditorAvatar = editorAvatar,
                        seriesEditDate = System.currentTimeMillis(),
                        seriesUpvotes = 0, seriesDownvotes = 0
                    ))
                }
                onSuccess()
            }
        }
    }

    fun voteSeriesEdit(isUpvote: Boolean) {
        val currentBook = _uiState.value.book ?: return
        val uid = authRepository.currentUser?.uid ?: return

        // Si ya votó, no hacemos nada
        if (currentBook.seriesVoters.contains(uid)) return

        viewModelScope.launch {
            // Actualización optimista en la UI
            val newUpvotes = if (isUpvote) currentBook.seriesUpvotes + 1 else currentBook.seriesUpvotes
            val newDownvotes = if (!isUpvote) currentBook.seriesDownvotes + 1 else currentBook.seriesDownvotes
            val newVoters = currentBook.seriesVoters + uid

            _uiState.update { it.copy(book = currentBook.copy(
                seriesUpvotes = newUpvotes, seriesDownvotes = newDownvotes, seriesVoters = newVoters
            ))}

            booksRepository.voteSeriesEdit(currentBook.id, uid, isUpvote)
        }
    }
}