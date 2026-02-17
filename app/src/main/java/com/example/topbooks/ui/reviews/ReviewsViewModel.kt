package com.example.topbooks.ui.reviews

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Comment
import com.example.topbooks.data.model.Reply
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ReviewsFeedState(
    val friendsReviews: List<Comment> = emptyList(),
    val communityReviews: List<Comment> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ReviewsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(ReviewsFeedState())
    val uiState: StateFlow<ReviewsFeedState> = _uiState.asStateFlow()

    init { loadSocialFeed() }

    fun loadSocialFeed() {
        val uid = auth.currentUser?.uid ?: return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val friendsSnapshot = db.collection("users").document(uid)
                    .collection("friends").get().await()

                val socialIds = friendsSnapshot.documents.map { it.id }.toMutableList()
                socialIds.add(0, uid) // Incluir al usuario actual

                // 1. Cargar Reseñas Sociales (Tuyo + Amigos)
                val socialComments = if (socialIds.isNotEmpty()) {
                    val query = db.collection("comments")
                        .whereIn("userId", socialIds.take(10))
                        .orderBy("createAt", Query.Direction.DESCENDING)
                        .get().await()

                    enrichComments(query.toObjects(Comment::class.java))
                } else { emptyList() }

                // 2. Cargar Comunidad
                val communityQuery = db.collection("comments")
                    .orderBy("rating", Query.Direction.DESCENDING)
                    .limit(20).get().await()

                val communityComments = enrichComments(
                    communityQuery.toObjects(Comment::class.java).filter {
                        it.userId.isNotEmpty() && !socialIds.contains(it.userId)
                    }
                )

                _uiState.update { it.copy(friendsReviews = socialComments, communityReviews = communityComments, isLoading = false) }
            } catch (e: Exception) {
                Log.e("ReviewsVM", "Error: ${e.message}")
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    // --- FUNCIÓN PARA RESPONDER Y ALMACENAR EN DB ---
    fun postReply(commentId: String, text: String) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                // Obtenemos los datos frescos del usuario para la respuesta
                val userDoc = db.collection("users").document(user.uid).get().await()
                val reply = Reply(
                    userId = user.uid,
                    userName = userDoc.getString("displayName") ?: "Usuario",
                    userPhotoUrl = userDoc.getString("photoURL") ?: "capibara_1",
                    text = text
                )

                db.collection("comments").document(commentId)
                    .update("replies", FieldValue.arrayUnion(reply))
                    .await()

                loadSocialFeed() // Recargar para mostrar respuesta
            } catch (e: Exception) {
                Log.e("ReviewsVM", "Error al responder: ${e.message}")
            }
        }
    }

    private suspend fun enrichComments(comments: List<Comment>): List<Comment> {
        return comments.map { comment ->
            viewModelScope.async {
                var enriched = comment
                try {
                    // CORRECCIÓN: Usando 'userDoc' consistentemente
                    val userDoc = db.collection("users").document(comment.userId).get().await()
                    if (userDoc.exists()) {
                        enriched = enriched.copy(
                            userName = userDoc.getString("displayName") ?: "Anónimo",
                            userPhotoUrl = userDoc.getString("photoURL") ?: "capibara_1"
                        )
                    }
                    val bookDoc = db.collection("books").document(comment.bookId).get().await()
                    if (bookDoc.exists()) {
                        enriched = enriched.copy(
                            bookTitle = bookDoc.getString("title") ?: "Sin título",
                            bookImageUrl = bookDoc.getString("thumbnail") ?: ""
                        )
                    }
                } catch (e: Exception) { }
                enriched
            }
        }.awaitAll()
    }
}