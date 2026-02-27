package com.example.topbooks.ui.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.model.Review
import com.example.topbooks.data.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookDetailState(
    val book: Book? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isBookSaved: Boolean = false,
    val savedInList: String? = null,
    val reviews: List<Review> = emptyList()
)

class BookDetailViewModel(
    private val booksRepository: BooksRepository = BooksRepository(),
    private val progressRepository: ProgressRepository = ProgressRepositoryImpl(),
    private val feedRepository: SocialFeedRepository = SocialFeedRepositoryImpl(),
    private val userRepository: UserRepository = UserRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookDetailState())
    val uiState: StateFlow<BookDetailState> = _uiState.asStateFlow()

    fun loadBook(bookId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = booksRepository.getBookDetail(bookId)

            if (result.isSuccess) {
                _uiState.update { it.copy(book = result.getOrNull(), isLoading = false) }
                checkIfBookIsSaved(bookId)
            } else {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message, isLoading = false) }
            }
        }
    }

    private fun checkIfBookIsSaved(bookId: String) {
        val uid = userRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            val read = progressRepository.getReadBooks(uid).getOrDefault(emptyList())
            if (read.any { it.id == bookId }) {
                _uiState.update { it.copy(isBookSaved = true, savedInList = "Leídos") }
                return@launch
            }
            val favs = userRepository.getFavoriteIds(uid).getOrDefault(emptyList())
            if (favs.contains(bookId)) {
                _uiState.update { it.copy(isBookSaved = true, savedInList = "Favoritos") }
                return@launch
            }
            val marks = progressRepository.getBookmarks(uid).getOrDefault(emptyList())
            if (marks.any { it.bookId == bookId }) {
                _uiState.update { it.copy(isBookSaved = true, savedInList = "Pendientes") }
                return@launch
            }
            _uiState.update { it.copy(isBookSaved = false, savedInList = null) }
        }
    }

    fun checkEmailVerification(onResult: (Boolean) -> Unit) {
        onResult(userRepository.isEmailVerified())
    }

    fun addToList(book: Book, listName: String) {
        viewModelScope.launch {
            when (listName) {
                "Favoritos" -> progressRepository.toggleFavorite(book, true)
                "Leídos" -> progressRepository.markAsRead(book)
                // "Pendientes" se gestiona al guardar el marcador (saveBookmark)
            }
            _uiState.update { it.copy(savedInList = listName, isBookSaved = true) }
        }
    }

    fun removeFromList(bookId: String, listName: String) {
        viewModelScope.launch {
            val collection = when(listName) {
                "Favoritos" -> "favorites"
                "Leídos" -> "read_books"
                "Pendientes" -> "bookmarks"
                else -> return@launch
            }
            progressRepository.deleteUserSubdocument(collection, bookId)
            _uiState.update { it.copy(savedInList = null, isBookSaved = false) }
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
                _uiState.update { it.copy(savedInList = "Pendientes", isBookSaved = true) }
                onSuccess()
            }
        }
    }
}