package com.example.topbooks.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Comment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class SimpleUser(val uid: String = "", val name: String = "", val photo: String = "")
data class SimpleBook(val id: String = "", val title: String = "", val imageUrl: String = "")

data class UserListState(
    val friends: List<SimpleUser> = emptyList(),
    val readBooks: List<SimpleBook> = emptyList(),
    val reviews: List<Comment> = emptyList(),
    val isLoading: Boolean = false
)

class UserListViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    // AÑADIDO: Necesitamos auth para saber si somos nosotros mismos y ver los diarios privados
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(UserListState())
    val uiState: StateFlow<UserListState> = _uiState.asStateFlow()

    fun loadData(type: String, userId: String) {
        if (userId.isEmpty()) return
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                when (type) {
                    "friends" -> fetchFriends(userId)
                    "read" -> fetchReadBooks(userId)
                    "reviews" -> fetchReviews(userId)
                    // --- NUEVAS RUTAS ---
                    "journals" -> fetchJournals(userId)
                    "bookmarks" -> fetchBookmarks(userId)
                    "comments" -> fetchComments(userId)
                    else -> _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                Log.e("UserListVM", "Error al cargar datos: ${e.message}")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun fetchFriends(userId: String) {
        val snapshot = db.collection("users").document(userId).collection("friends").get().await()
        val list = snapshot.documents.map { doc ->
            SimpleUser(
                uid = doc.id,
                name = doc.getString("displayName") ?: "Usuario",
                photo = doc.getString("photoURL") ?: ""
            )
        }
        _uiState.update { it.copy(friends = list, isLoading = false) }
    }

    private suspend fun fetchReadBooks(userId: String) {
        val snapshot = db.collection("users").document(userId).collection("read_books").get().await()
        val list = snapshot.documents.map { doc ->
            SimpleBook(
                id = doc.id,
                title = doc.getString("title") ?: "Libro",
                imageUrl = doc.getString("imageUrl") ?: ""
            )
        }
        _uiState.update { it.copy(readBooks = list, isLoading = false) }
    }

    private suspend fun fetchReviews(userId: String) {
        val snapshot = db.collection("reviews").whereEqualTo("userId", userId).get().await()
        val rawReviews = snapshot.toObjects(Comment::class.java)
        enrichAndSetReviews(rawReviews, userId)
    }

    // --- NUEVAS FUNCIONES DE BÚSQUEDA ---

    private suspend fun fetchBookmarks(userId: String) {
        val snapshot = db.collection("users").document(userId).collection("bookmarks").get().await()
        val list = snapshot.documents.map { doc ->
            SimpleBook(
                id = doc.getString("bookId") ?: doc.id,
                title = "Marcador guardado", // Puedes enriquecer esto luego buscando en books
                imageUrl = ""
            )
        }
        _uiState.update { it.copy(readBooks = list, isLoading = false) }
    }

    private suspend fun fetchComments(userId: String) {
        val snapshot = db.collection("comments").whereEqualTo("userId", userId).get().await()
        val rawComments = snapshot.toObjects(Comment::class.java)
        enrichAndSetReviews(rawComments, userId)
    }

    private suspend fun fetchJournals(userId: String) {
        val query = db.collection("users").document(userId).collection("journals")

        // --- LÓGICA DE PRIVACIDAD ---
        val snapshot = if (userId == auth.currentUser?.uid) {
            // Si soy yo, me traigo TODOS mis diarios (públicos y privados)
            query.get().await()
        } else {
            // Si es el perfil de un amigo, filtro SOLO los públicos
            query.whereEqualTo("isPublic", true).get().await()
        }

        // Reciclamos la clase Comment para aprovechar la interfaz de usuario de las tarjetas
        val rawJournals = snapshot.documents.mapNotNull { doc ->
            Comment(
                commentId = doc.id,
                bookId = doc.getString("bookId") ?: "",
                userId = userId,
                // Mostraremos el momento favorito o una frase genérica si está vacío
                text = doc.getString("moments")?.takeIf { it.isNotBlank() } ?: "Diario de lectura del libro.",
                rating = doc.getLong("mainRating")?.toInt() ?: 0,
                bookTitle = doc.getString("bookTitle") ?: "Libro",
                bookImageUrl = doc.getString("bookImageUrl") ?: ""
            )
        }

        enrichAndSetReviews(rawJournals, userId)
    }

    // Función "Helper" para buscar el nombre y foto del usuario, y del libro si faltase
    private suspend fun enrichAndSetReviews(rawItems: List<Comment>, userId: String) {
        val userDoc = try { db.collection("users").document(userId).get().await() } catch(e:Exception){ null }
        val userName = userDoc?.getString("displayName") ?: "Usuario"
        val userPhoto = userDoc?.getString("photoURL") ?: "capibara_1"

        val enriched = rawItems.map { item ->
            viewModelScope.async {
                var current = item.copy(userName = userName, userPhotoUrl = userPhoto)
                try {
                    // Si el título viene vacío, lo buscamos en la base de libros global
                    if (current.bookTitle.isEmpty() || current.bookTitle == "Libro") {
                        val bookDoc = db.collection("books").document(item.bookId).get().await()
                        if (bookDoc.exists()) {
                            current = current.copy(
                                bookTitle = bookDoc.getString("title") ?: "Sin título",
                                bookImageUrl = bookDoc.getString("thumbnail") ?: ""
                            )
                        }
                    }
                } catch (e: Exception) { }
                current
            }
        }.awaitAll()

        _uiState.update { it.copy(reviews = enriched, isLoading = false) }
    }
}