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
    val targetReview: Comment? = null, // Para resaltar el hilo de Deep Link
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

//TODO MEJORAR EL DEEP LINKING
class ReviewsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(ReviewsFeedState())
    val uiState: StateFlow<ReviewsFeedState> = _uiState.asStateFlow()

    // Carga general por defecto
    init { loadSocialFeed() }

    fun loadSocialFeed(bookId: String? = null, targetCommentId: String? = null) {
        val uid = auth.currentUser?.uid ?: return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                if (bookId != null) {
                    // MODO DEEP LINK: Cargar solo comentarios de un libro
                    val query = db.collection("comments")
                        .whereEqualTo("bookId", bookId)
                        .orderBy("createAt", Query.Direction.DESCENDING)
                        .get().await()

                    val allComments = enrichComments(query.toObjects(Comment::class.java))

                    // Si buscamos uno específico, lo ponemos al principio
                    val sorted = if (targetCommentId != null) {
                        allComments.sortedByDescending { it.commentId == targetCommentId }
                    } else allComments

                    _uiState.update { it.copy(friendsReviews = sorted, isLoading = false) }
                } else {
                    // MODO NORMAL: Feed de amigos y comunidad
                    val friendsSnapshot = db.collection("users").document(uid).collection("friends").get().await()
                    val friendIds = friendsSnapshot.documents.map { it.id }

                    if (friendIds.isNotEmpty()) {
                        val friendsQuery = db.collection("comments")
                            .whereIn("userId", friendIds.take(10))
                            .orderBy("createAt", Query.Direction.DESCENDING)
                            .limit(20).get().await()
                        val enrichedFriends = enrichComments(friendsQuery.toObjects(Comment::class.java))
                        _uiState.update { it.copy(friendsReviews = enrichedFriends) }
                    }

                    val communityQuery = db.collection("comments")
                        .orderBy("createAt", Query.Direction.DESCENDING)
                        .limit(20).get().await()
                    val enrichedCommunity = enrichComments(communityQuery.toObjects(Comment::class.java))

                    _uiState.update { it.copy(communityReviews = enrichedCommunity, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                Log.e("ReviewsVM", "Error: ${e.message}")
            }
        }
    }

    fun addReply(commentId: String, text: String) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                val reply = Reply(
                    userId = user.uid,
                    userName = user.displayName ?: "Usuario",
                    userPhotoUrl = user.photoUrl?.toString() ?: "capibara_1",
                    text = text,
                    timestamp = System.currentTimeMillis()
                )
                db.collection("comments").document(commentId)
                    .update("replies", FieldValue.arrayUnion(reply)).await()

                loadSocialFeed()
            } catch (e: Exception) { Log.e("ReviewsVM", "Reply error") }
        }
    }

    private suspend fun enrichComments(comments: List<Comment>): List<Comment> {
        return comments.map { comment ->
            viewModelScope.async {
                var enriched = comment
                try {
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