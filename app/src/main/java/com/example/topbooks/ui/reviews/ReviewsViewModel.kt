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
import com.google.firebase.Firebase
import com.google.firebase.functions.functions

data class ReviewsFeedState(
    val friendsReviews: List<Comment> = emptyList(),
    val communityReviews: List<Comment> = emptyList(),
    val targetReview: Comment? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ReviewsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(ReviewsFeedState())
    val uiState: StateFlow<ReviewsFeedState> = _uiState.asStateFlow()

    init { loadSocialFeed() }

    fun loadSocialFeed(bookId: String? = null, targetCommentId: String? = null) {
        val uid = auth.currentUser?.uid ?: return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                if (bookId != null) {
                    val query = db.collection("comments")
                        .whereEqualTo("bookId", bookId)
                        .orderBy("createAt", Query.Direction.DESCENDING)
                        .get().await()

                    val allComments = enrichComments(query.toObjects(Comment::class.java))
                    val sorted = if (targetCommentId != null) {
                        allComments.sortedByDescending { it.commentId == targetCommentId }
                    } else allComments

                    _uiState.update { it.copy(friendsReviews = sorted, isLoading = false) }
                } else {
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
            }
        }
    }

    // 🟢 NUEVA FUNCIÓN: Comprueba la verificación del correo
    fun checkEmailVerification(onResult: (Boolean) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            try {
                user.reload().await()
                onResult(user.isEmailVerified)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun addReply(comment: Comment, text: String) {
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

                db.collection("comments").document(comment.commentId)
                    .update("replies", FieldValue.arrayUnion(reply)).await()

                if (comment.userId != user.uid) {
                    val data = hashMapOf(
                        "autorComentarioOriginalId" to comment.userId,
                        "nombreRespondedor" to (user.displayName ?: "Alguien"),
                        "bookId" to comment.bookId,
                        "commentId" to comment.commentId
                    )
                    Firebase.functions.getHttpsCallable("enviarNotificacionRespuesta").call(data)
                }

                loadSocialFeed()
            } catch (e: Exception) {
                Log.e("ReviewsVM", "ERROR CRÍTICO en addReply: ${e.message}")
            }
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