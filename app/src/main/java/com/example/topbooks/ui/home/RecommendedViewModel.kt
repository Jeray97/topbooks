package com.example.topbooks.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.repository.BooksRepository
import com.example.topbooks.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RecommendedViewModel(
    private val repository: BooksRepository = BooksRepository()
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Estados
    private val _popularBooks = MutableStateFlow<Resource<List<Book>>>(Resource.Loading)
    val popularBooks: StateFlow<Resource<List<Book>>> = _popularBooks.asStateFlow()

    private val _tastesBooks = MutableStateFlow<Resource<List<Book>>>(Resource.Loading)
    val tastesBooks: StateFlow<Resource<List<Book>>> = _tastesBooks.asStateFlow()

    private val _friendsBooks = MutableStateFlow<Resource<List<Book>>>(Resource.Loading)
    val friendsBooks: StateFlow<Resource<List<Book>>> = _friendsBooks.asStateFlow()

    var usedGenreForTastes: String = "General"
        private set

    init {
        fetchPopularBooks()
        fetchBooksByTastes()
        fetchFriendsFavorites()
    }

    private fun fetchPopularBooks() {
        viewModelScope.launch {
            _popularBooks.value = Resource.Loading
            val result = repository.getBooks("subject:thriller", orderBy = "newest")
            if (result.isSuccess) {
                _popularBooks.value = Resource.Success(result.getOrDefault(emptyList()))
            } else {
                _popularBooks.value = Resource.Error(result.exceptionOrNull() ?: Exception("Error"))
            }
        }
    }

    // --- CORRECCIÓN AQUÍ ---
    private fun fetchBooksByTastes() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _tastesBooks.value = Resource.Success(emptyList())
            return
        }

        viewModelScope.launch {
            _tastesBooks.value = Resource.Loading
            try {
                val userDoc = db.collection("users").document(uid).get().await()

                // Leemos la lista como List<String>
                val genres = userDoc.get("favoriteGenres") as? List<String> ?: emptyList()

                Log.d("RecommendedVM", "Géneros recuperados de Firestore: $genres")

                // Si la lista está vacía, usamos un fallback aleatorio en vez de siempre "Fantasía"
                val fallbackGenres = listOf("Fantasía", "Ciencia Ficción", "Misterio", "Romance", "Terror", "Historia")
                val queryGenre = if (genres.isNotEmpty()) genres.random() else fallbackGenres.random()

                usedGenreForTastes = queryGenre
                Log.d("RecommendedVM", "Usando género para búsqueda: $queryGenre")

                val result = repository.getBooks("subject:$queryGenre", orderBy = "newest")

                if (result.isSuccess) {
                    _tastesBooks.value = Resource.Success(result.getOrDefault(emptyList()))
                } else {
                    _tastesBooks.value = Resource.Error(result.exceptionOrNull() ?: Exception("Error"))
                }
            } catch (e: Exception) {
                Log.e("RecommendedVM", "Error obteniendo gustos: ${e.message}")
                _tastesBooks.value = Resource.Error(e)
            }
        }
    }

    private fun fetchFriendsFavorites() {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _friendsBooks.value = Resource.Loading
            try {
                val friendsSnapshot = db.collection("users").document(uid)
                    .collection("friends").get().await()

                if (friendsSnapshot.isEmpty) {
                    _friendsBooks.value = Resource.Success(emptyList())
                    return@launch
                }

                val friendIds = friendsSnapshot.documents.map { it.id }

                val booksIdsDeferred = friendIds.map { friendId ->
                    async {
                        val favs = db.collection("users").document(friendId)
                            .collection("favorites").limit(5).get().await()
                        favs.documents.mapNotNull { it.getString("bookId") }
                    }
                }

                val allBookIds = booksIdsDeferred.awaitAll().flatten().distinct().take(15)

                if (allBookIds.isEmpty()) {
                    _friendsBooks.value = Resource.Success(emptyList())
                    return@launch
                }

                val booksDetailsDeferred = allBookIds.map { bookId ->
                    async { repository.getBookDetail(bookId).getOrNull() }
                }

                val unsortedBooks = booksDetailsDeferred.awaitAll().filterNotNull()

                val sortedBooks = unsortedBooks.sortedByDescending { book ->
                    Regex("\\d{4}").find(book.lanzamiento)?.value?.toIntOrNull() ?: 0
                }

                _friendsBooks.value = Resource.Success(sortedBooks)

            } catch (e: Exception) {
                _friendsBooks.value = Resource.Error(e)
            }
        }
    }
}