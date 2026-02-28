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
    // 🔥 Separamos Favoritos de las listas de lectura
    val isFavorite: Boolean = false,
    val savedInList: String? = null, // Solo será "Leídos" o "Pendientes"
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
                _uiState.update { it.copy(book = result.getOrNull(), isLoading = false) }
                checkUserLists(bookId)
            } else {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message, isLoading = false) }
            }
        }
    }

    // 🔥 Comprobamos todas las listas de forma concurrente para mayor velocidad
    private fun checkUserLists(bookId: String) {
        val uid = userRepository.getCurrentUserId() ?: return

        viewModelScope.launch {
            try {
                // Lanzamos las 3 consultas a la vez
                val readDeferred = async { progressRepository.getReadBooks(uid).getOrDefault(emptyList()) }
                val favsDeferred = async { userRepository.getFavoriteIds(uid).getOrDefault(emptyList()) }
                val marksDeferred = async { progressRepository.getBookmarks(uid).getOrDefault(emptyList()) }

                // Esperamos los resultados
                val read = readDeferred.await()
                val favs = favsDeferred.await()
                val marks = marksDeferred.await()

                // Evaluamos Favoritos de forma independiente
                val isFav = favs.contains(bookId)

                // Evaluamos Leídos vs Pendientes (son mutuamente excluyentes)
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

    // 🔥 Nueva función exclusiva para favoritos con actualización optimista
    fun toggleFavorite(book: Book) {
        val currentState = _uiState.value.isFavorite
        val newState = !currentState

        // 1. Actualización optimista de la UI (parece instantáneo)
        _uiState.update { it.copy(isFavorite = newState) }

        // 2. Operación real en Firebase
        viewModelScope.launch {
            try {
                if (newState) {
                    progressRepository.toggleFavorite(book, true)
                } else {
                    progressRepository.deleteUserSubdocument("favorites", book.id)
                }
            } catch (e: Exception) {
                Log.e("BookDetailVM", "Error al cambiar favorito: ${e.message}")
                // Si falla, revertimos al estado anterior
                _uiState.update { it.copy(isFavorite = currentState) }
            }
        }
    }

    fun checkEmailVerification(onResult: (Boolean) -> Unit) {
        onResult(authRepository.isEmailVerified())
    }

    fun addToList(book: Book, listName: String) {
        viewModelScope.launch {
            // "Pendientes" no está aquí porque se añade mediante "saveBookmark"
            if (listName == "Leídos") {
                progressRepository.markAsRead(book)
                _uiState.update { it.copy(savedInList = "Leídos") }
            }
        }
    }

    fun removeFromList(bookId: String, listName: String) {
        viewModelScope.launch {
            val collection = when(listName) {
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
            progressRepository.saveReview(book, rating, text).onSuccess { onSuccess() }
        }
    }

    fun saveComment(book: Book, text: String, chapter: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            progressRepository.saveComment(book, text, chapter).onSuccess { onSuccess() }
        }
    }

    fun saveBookmark(book: Book, page: String, quote: String, chapter: String, isPublic: Boolean, onSuccess: () -> Unit) {
        viewModelScope.launch {
            progressRepository.saveBookmark(book, quote, chapter, page, isPublic).onSuccess {
                _uiState.update { it.copy(savedInList = "Pendientes") }
                onSuccess()
            }
        }
    }
}