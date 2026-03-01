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
    // 🔥 Favoritos es independiente
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

    private fun checkUserLists(bookId: String) {
        val uid = userRepository.getCurrentUserId() ?: return

        viewModelScope.launch {
            try {
                // Lanzamos las consultas a la vez para mayor velocidad
                val readDeferred = async { progressRepository.getReadBooks(uid).getOrDefault(emptyList()) }
                val favsDeferred = async { userRepository.getFavoriteIds(uid).getOrDefault(emptyList()) }
                val marksDeferred = async { progressRepository.getBookmarks(uid).getOrDefault(emptyList()) }

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

    fun toggleFavorite(book: Book) {
        val currentState = _uiState.value.isFavorite
        val newState = !currentState

        // Actualización optimista de la UI (parece instantáneo)
        _uiState.update { it.copy(isFavorite = newState) }

        viewModelScope.launch {
            try {
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
            //Forzamos la recarga del usuario desde el servidor de Firebase
            authRepository.reloadUser()
            // Ahora sí comprobamos el estado real actualizado
            onResult(authRepository.isEmailVerified())
        }
    }

    // 🔥 AHORA SÍ MANEJA "PENDIENTES" Y GARANTIZA EXCLUSIVIDAD EN BASE DE DATOS
    fun addToList(book: Book, listName: String) {
        viewModelScope.launch {
            if (listName == "Leídos") {
                // Si lo marco como leído, lo borro de pendientes
                progressRepository.deleteUserSubdocument("bookmarks", book.id)
                progressRepository.markAsRead(book)
                _uiState.update { it.copy(savedInList = "Leídos") }

            } else if (listName == "Pendientes") {
                // Si lo marco como pendiente, lo borro de leídos
                progressRepository.deleteUserSubdocument("read_books", book.id)
                // Guardamos un marcador básico/vacío en la base de datos
                progressRepository.saveBookmark(book, "", "", "", false)
                _uiState.update { it.copy(savedInList = "Pendientes") }
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
            // 🔥 Si crean un marcador con el FAB, garantizamos exclusividad quitándolo de leídos
            progressRepository.deleteUserSubdocument("read_books", book.id)
            progressRepository.saveBookmark(book, quote, chapter, page, isPublic).onSuccess {
                _uiState.update { it.copy(savedInList = "Pendientes") }
                onSuccess()
            }
        }
    }
}