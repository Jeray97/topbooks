package com.example.topbooks.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.R
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

// --- NUEVA CLASE: Vincula un libro con el amigo que lo tiene en favoritos ---
data class FriendBookRecommendation(
    val book: Book,
    val friendName: String,
    val friendPhotoUrl: String
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BooksRepository()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // --- ESTADOS ---
    private val _categoryBooks = MutableStateFlow<Resource<List<Book>>>(Resource.Loading)
    val categoryBooks: StateFlow<Resource<List<Book>>> = _categoryBooks.asStateFlow()

    private val _recommendedBooks = MutableStateFlow<Resource<List<Book>>>(Resource.Loading)
    val recommendedBooks: StateFlow<Resource<List<Book>>> = _recommendedBooks.asStateFlow()

    // CAMBIO IMPORTANTE: Ahora este estado guarda FriendBookRecommendation, no solo Book
    private val _friendsBooks = MutableStateFlow<Resource<List<FriendBookRecommendation>>>(Resource.Loading)
    val friendsBooks: StateFlow<Resource<List<FriendBookRecommendation>>> = _friendsBooks.asStateFlow()

    init {
        loadInitialData()
        fetchRecommendedBooks()
        fetchFriendsBooks()
    }

    private fun loadInitialData() {
        val categoryName = getApplication<Application>().getString(R.string.cat_fantasia_text)
        fetchBooks("subject:$categoryName", _categoryBooks)
    }

    fun onCategorySelected(categoryTerm: String) {
        fetchBooks("subject:$categoryTerm", _categoryBooks)
    }

    private fun fetchRecommendedBooks() {
        viewModelScope.launch {
            _recommendedBooks.value = Resource.Loading
            val baseQuery = "subject:science_fiction"
            val result = repository.getBooks(query = baseQuery, orderBy = "relevance", filterModern = true)

            if (result.isSuccess) {
                _recommendedBooks.value = Resource.Success(result.getOrDefault(emptyList()).shuffled().take(10))
            } else {
                _recommendedBooks.value = Resource.Error(result.exceptionOrNull() ?: Exception("Error"))
            }
        }
    }

    // --- LÓGICA DE AMIGOS ACTUALIZADA ---
    private fun fetchFriendsBooks() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _friendsBooks.value = Resource.Success(emptyList())
            return
        }

        viewModelScope.launch {
            _friendsBooks.value = Resource.Loading
            try {
                // 1. Obtener lista de amigos (con sus datos guardados en la subcolección)
                val friendsSnapshot = db.collection("users").document(uid)
                    .collection("friends").get().await()

                if (friendsSnapshot.isEmpty) {
                    _friendsBooks.value = Resource.Success(emptyList())
                    return@launch
                }

                // Creamos una lista temporal de objetos con (ID, Nombre, Foto)
                val friendsData = friendsSnapshot.documents.map { doc ->
                    Triple(
                        doc.id,
                        doc.getString("displayName") ?: "Amigo",
                        doc.getString("photoURL") ?: ""
                    )
                }

                // 2. Buscar favoritos de cada amigo en paralelo
                val recommendationsDeferred = friendsData.map { (friendId, name, photo) ->
                    async {
                        val favsSnapshot = db.collection("users").document(friendId)
                            .collection("favorites")
                            .limit(3) // Limitamos a 3 por amigo para variedad
                            .get().await()

                        // Retornamos una lista de pares (BookID, DatosAmigo)
                        favsSnapshot.documents.mapNotNull { doc ->
                            val bookId = doc.getString("bookId")
                            if (bookId != null) {
                                bookId to Pair(name, photo)
                            } else null
                        }
                    }
                }

                // Aplanamos la lista: [(BookID, (Name, Photo)), ...]
                val allRecommendationsRaw = recommendationsDeferred.awaitAll().flatten().shuffled().take(15)

                if (allRecommendationsRaw.isEmpty()) {
                    _friendsBooks.value = Resource.Success(emptyList())
                    return@launch
                }

                // 3. Obtener detalles del libro y construir el objeto final
                val finalRecommendationsDeferred = allRecommendationsRaw.map { (bookId, friendInfo) ->
                    async {
                        val bookResult = repository.getBookDetail(bookId).getOrNull()
                        if (bookResult != null) {
                            FriendBookRecommendation(
                                book = bookResult,
                                friendName = friendInfo.first,
                                friendPhotoUrl = friendInfo.second
                            )
                        } else null
                    }
                }

                val finalBooks = finalRecommendationsDeferred.awaitAll().filterNotNull()
                _friendsBooks.value = Resource.Success(finalBooks)

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error cargando favoritos amigos: ${e.message}")
                _friendsBooks.value = Resource.Success(emptyList())
            }
        }
    }

    private fun fetchBooks(query: String, state: MutableStateFlow<Resource<List<Book>>>) {
        viewModelScope.launch {
            state.value = Resource.Loading
            val result = repository.getBooks(query)
            if (result.isSuccess) {
                state.value = Resource.Success(result.getOrDefault(emptyList()))
            } else {
                state.value = Resource.Error(result.exceptionOrNull() ?: Exception("Error"))
            }
        }
    }
}