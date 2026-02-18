package com.example.topbooks.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.repository.BooksRepository
import com.example.topbooks.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RecommendedSectionViewModel(
    private val repository: BooksRepository = BooksRepository()
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _booksState = MutableStateFlow<Resource<List<Book>>>(Resource.Loading)
    val booksState: StateFlow<Resource<List<Book>>> = _booksState.asStateFlow()

    // Paginación
    private var currentPage = 1
    private var currentType = ""
    private var currentGenre = ""
    private var isLastPage = false
    private var isLoadingMore = false
    private val loadedBooks = mutableListOf<Book>()

    fun loadSectionData(type: String, genre: String) {
        if (currentType != type || currentGenre != genre) {
            currentType = type
            currentGenre = genre
            currentPage = 1
            isLastPage = false
            loadedBooks.clear()
            _booksState.value = Resource.Loading
            loadNextPage()
        }
    }

    fun loadNextPage() {
        if (isLoadingMore || isLastPage) return
        isLoadingMore = true

        viewModelScope.launch {
            if (currentPage == 1 && _booksState.value is Resource.Error) {
                _booksState.value = Resource.Loading
            }

            try {
                when (currentType) {
                    "POPULAR" -> fetchBooksGeneric("subject:thriller", "newest")
                    "TASTES" -> {
                        val q = if (currentGenre.isNotEmpty() && currentGenre != "General") currentGenre else "Fantasía"
                        fetchBooksGeneric("subject:$q", "newest")
                    }
                    "FRIENDS" -> fetchFriendsFavorites()
                }
            } catch (e: Exception) {
                if (currentPage == 1) _booksState.value = Resource.Error(e)
                isLoadingMore = false
            }
        }
    }

    private suspend fun fetchBooksGeneric(query: String, sort: String) {
        // SOLUCIÓN "POCOS LIBROS": Pedimos 30 de golpe para asegurar que llenamos la pantalla
        val result = repository.getBooks(query, orderBy = sort, page = currentPage, limit = 30)

        isLoadingMore = false

        if (result.isSuccess) {
            val newBooks = result.getOrDefault(emptyList())

            if (newBooks.isEmpty()) {
                isLastPage = true
            } else {
                loadedBooks.addAll(newBooks)
                currentPage++
            }
            _booksState.value = Resource.Success(loadedBooks.toList())
        } else {
            if (currentPage == 1) {
                _booksState.value = Resource.Error(result.exceptionOrNull() ?: Exception("Error"))
            }
        }
    }

    // CORRECCIÓN "ASYNC ERROR": Usamos coroutineScope {}
    private suspend fun fetchFriendsFavorites() = coroutineScope {
        if (currentPage > 1) {
            isLoadingMore = false
            return@coroutineScope
        }

        try {
            val uid = auth.currentUser?.uid ?: run {
                _booksState.value = Resource.Success(emptyList())
                isLoadingMore = false
                return@coroutineScope
            }

            val friendsSnapshot = db.collection("users").document(uid)
                .collection("friends").get().await()

            if (friendsSnapshot.isEmpty) {
                _booksState.value = Resource.Success(emptyList())
                isLoadingMore = false
                return@coroutineScope
            }

            val friendIds = friendsSnapshot.documents.map { it.id }

            // Async funciona aquí porque estamos dentro de coroutineScope
            val booksIdsDeferred = friendIds.map { friendId ->
                async {
                    val favs = db.collection("users").document(friendId)
                        .collection("favorites").limit(5).get().await()
                    favs.documents.mapNotNull { it.getString("bookId") }
                }
            }

            // Aumentamos límite de amigos para traer más libros
            val allBookIds = booksIdsDeferred.awaitAll().flatten().distinct().take(30)

            if (allBookIds.isEmpty()) {
                _booksState.value = Resource.Success(emptyList())
                isLoadingMore = false
                return@coroutineScope
            }

            val booksDetailsDeferred = allBookIds.map { bookId ->
                async { repository.getBookDetail(bookId).getOrNull() }
            }

            val unsorted = booksDetailsDeferred.awaitAll().filterNotNull()

            val sorted = unsorted.sortedByDescending { book ->
                Regex("\\d{4}").find(book.lanzamiento)?.value?.toIntOrNull() ?: 0
            }

            loadedBooks.addAll(sorted)
            _booksState.value = Resource.Success(loadedBooks.toList())
            isLastPage = true

        } catch (e: Exception) {
            _booksState.value = Resource.Error(e)
        } finally {
            isLoadingMore = false
        }
    }
}