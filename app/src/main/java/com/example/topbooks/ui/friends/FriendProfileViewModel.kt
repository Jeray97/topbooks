package com.example.topbooks.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.model.Review
import com.example.topbooks.data.model.User
import com.example.topbooks.data.repository.BooksRepository
import com.example.topbooks.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FriendProfileViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val booksRepository = BooksRepository()

    // 1. Estado del Usuario (Carga básica)
    private val _friendState = MutableStateFlow<Resource<User>>(Resource.Loading)
    val friendState: StateFlow<Resource<User>> = _friendState.asStateFlow()

    // 2. VARIABLES NECESARIAS PARA LA UI (Listas para el Dashboard)
    private val _favoriteBooks = MutableStateFlow<List<Book>>(emptyList())
    val favoriteBooks: StateFlow<List<Book>> = _favoriteBooks.asStateFlow()

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()

    fun loadFriendProfile(userId: String) {
        viewModelScope.launch {
            _friendState.value = Resource.Loading
            try {
                val doc = db.collection("users").document(userId).get().await()
                if (doc.exists()) {
                    val user = doc.toObject(User::class.java)
                    if (user != null) {
                        _friendState.value = Resource.Success(user)
                        // Una vez cargado el usuario, cargamos sus extras (libros y reseñas)
                        loadAdditionalContent(user)
                    } else {
                        _friendState.value = Resource.Error(Exception("Error de datos"))
                    }
                } else {
                    _friendState.value = Resource.Error(Exception("Usuario no encontrado"))
                }
            } catch (e: Exception) {
                _friendState.value = Resource.Error(e)
            }
        }
    }

    private fun loadAdditionalContent(user: User) {
        viewModelScope.launch {
            // A. Cargar Libros Favoritos (limitado a 4 para la tarjeta pequeña)
            if (user.favoriteBooks.isNotEmpty()) {
                val limitedIds = user.favoriteBooks.take(4)
                // Buscamos los detalles de cada libro en paralelo
                val books = limitedIds.map { bookId ->
                    async { booksRepository.getBookDetail(bookId).getOrNull() }
                }.awaitAll().filterNotNull()
                _favoriteBooks.value = books
            } else {
                _favoriteBooks.value = emptyList()
            }

            // B. Cargar Reseñas (limitado a 5)
            try {
                val snapshot = db.collection("reviews")
                    .whereEqualTo("userId", user.uid)
                    .limit(5)
                    .get().await()

                // Convertimos los documentos a objetos Review
                val reviewsList = snapshot.toObjects(Review::class.java)
                _reviews.value = reviewsList
            } catch (e: Exception) {
                _reviews.value = emptyList()
            }
        }
    }
}