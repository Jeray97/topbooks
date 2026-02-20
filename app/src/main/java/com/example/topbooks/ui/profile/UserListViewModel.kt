package com.example.topbooks.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Comment
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
                }
            } catch (e: Exception) {
                Log.e("UserListVM", "Error cargando lista: ${e.message}")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun fetchFriends(userId: String) {
        val snp = db.collection("users").document(userId).collection("friends").get().await()
        val list = snp.documents.map { doc ->
            SimpleUser(
                uid = doc.id,
                name = doc.getString("displayName") ?: "Usuario",
                photo = doc.getString("photoURL") ?: "capibara_1"
            )
        }
        _uiState.update { it.copy(friends = list, isLoading = false) }
    }

    private suspend fun fetchReadBooks(userId: String) {
        val snp = db.collection("users").document(userId).collection("favorites")
            .whereEqualTo("list", "Leídos").get().await()
        val list = snp.documents.map { doc ->
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

        // --- PROCESO DE ENRIQUECIMIENTO ---
        // Para cada reseña, vamos a buscar el título del libro en la colección "books"
        val enrichedReviews = rawReviews.map { review ->
            viewModelScope.async {
                var current = review
                try {
                    val bookDoc = db.collection("books").document(review.bookId).get().await()
                    if (bookDoc.exists()) {
                        current = current.copy(
                            bookTitle = bookDoc.getString("title") ?: "Sin título",
                            bookImageUrl = bookDoc.getString("thumbnail") ?: ""
                        )
                    }
                } catch (e: Exception) {
                    Log.e("UserListVM", "Error al traer info del libro ${review.bookId}")
                }
                current
            }
        }.awaitAll()

        _uiState.update { it.copy(reviews = enrichedReviews, isLoading = false) }
    }
}