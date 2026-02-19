package com.example.topbooks.ui.friends

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.repository.BooksRepository
import com.example.topbooks.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

// Agregamos commentId para poder navegar al hilo específico
data class SocialActivityItem(
    val id: String = UUID.randomUUID().toString(),
    val type: ActivityType,
    val friendName: String,
    val friendPhotoUrl: String,
    val bookId: String,
    val bookTitle: String,
    val bookImageUrl: String,
    val content: String,
    val rating: Int,
    val timestamp: Date,
    val replyToName: String? = null,
    val replyToContent: String? = null,
    val commentId: String? = null // <--- NUEVO: ID del hilo original
)

enum class ActivityType { REVIEW, FAVORITE, COMMENT, REPLY }

class SocialActivityViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val booksRepository = BooksRepository()

    private val _uiState = MutableStateFlow<Resource<List<SocialActivityItem>>>(Resource.Loading)
    val uiState: StateFlow<Resource<List<SocialActivityItem>>> = _uiState.asStateFlow()

    init {
        loadSocialFeed()
    }

    fun loadSocialFeed() {
        val currentUser = auth.currentUser ?: return
        _uiState.value = Resource.Loading

        viewModelScope.launch {
            try {
                val friendsSnapshot = db.collection("users").document(currentUser.uid)
                    .collection("friends").get().await()

                val friendIds = friendsSnapshot.documents.map { it.id }

                if (friendIds.isEmpty()) {
                    _uiState.value = Resource.Success(emptyList())
                    return@launch
                }

                val activeFriends = friendIds.take(10)

                val activitiesDeferred = activeFriends.map { friendId ->
                    async {
                        val activities = mutableListOf<SocialActivityItem>()
                        try {
                            val userDoc = db.collection("users").document(friendId).get().await()
                            val fName = userDoc.getString("displayName") ?: "Amigo"
                            val fPhoto = userDoc.getString("photoURL") ?: ""

                            // 1. Reseñas
                            val reviews = db.collection("reviews")
                                .whereEqualTo("userId", friendId)
                                .orderBy("createAt", Query.Direction.DESCENDING)
                                .limit(3).get().await()

                            reviews.documents.forEach { doc ->
                                val (title, image) = getBookInfo(doc.getString("bookId") ?: "")
                                activities.add(SocialActivityItem(
                                    type = ActivityType.REVIEW, friendName = fName, friendPhotoUrl = fPhoto,
                                    bookId = doc.getString("bookId") ?: "", bookTitle = title, bookImageUrl = image,
                                    content = doc.getString("text") ?: "",
                                    rating = doc.getLong("rating")?.toInt() ?: 0,
                                    timestamp = doc.getDate("createAt") ?: Date()
                                ))
                            }

                            // 2. Favoritos
                            val favorites = db.collection("users").document(friendId)
                                .collection("favorites").limit(3).get().await()

                            favorites.documents.forEach { doc ->
                                val (title, image) = getBookInfo(doc.getString("bookId") ?: "")
                                activities.add(SocialActivityItem(
                                    type = ActivityType.FAVORITE, friendName = fName, friendPhotoUrl = fPhoto,
                                    bookId = doc.getString("bookId") ?: "", bookTitle = title, bookImageUrl = image,
                                    content = "Añadió este libro a sus favoritos.",
                                    rating = 0, timestamp = Date()
                                ))
                            }

                            // 3. Comentarios y Respuestas (Ahora capturamos el doc.id como commentId)
                            val comments = db.collection("comments")
                                .orderBy("createAt", Query.Direction.DESCENDING)
                                .limit(20).get().await()

                            comments.documents.forEach { doc ->
                                val bookId = doc.getString("bookId") ?: ""
                                val bookData = getBookInfo(bookId)
                                val currentCommentId = doc.id

                                if (doc.getString("userId") == friendId) {
                                    activities.add(SocialActivityItem(
                                        type = ActivityType.COMMENT, friendName = fName, friendPhotoUrl = fPhoto,
                                        bookId = bookId, bookTitle = bookData.first, bookImageUrl = bookData.second,
                                        content = doc.getString("text") ?: "",
                                        rating = 0, timestamp = doc.getDate("createAt") ?: Date(),
                                        commentId = currentCommentId // Guardamos para navegar
                                    ))
                                }

                                val repliesRaw = doc.get("replies") as? List<Map<String, Any>>
                                repliesRaw?.forEach { reply ->
                                    if (reply["userId"] == friendId) {
                                        val rawTs = reply["timestamp"]
                                        val finalDate = when (rawTs) {
                                            is com.google.firebase.Timestamp -> rawTs.toDate()
                                            is Long -> Date(rawTs)
                                            else -> Date()
                                        }

                                        activities.add(SocialActivityItem(
                                            type = ActivityType.REPLY,
                                            friendName = fName,
                                            friendPhotoUrl = fPhoto,
                                            bookId = bookId,
                                            bookTitle = bookData.first,
                                            bookImageUrl = bookData.second,
                                            content = reply["text"] as? String ?: "",
                                            rating = 0,
                                            timestamp = finalDate,
                                            replyToName = doc.getString("userName") ?: "Usuario",
                                            replyToContent = doc.getString("text"),
                                            commentId = currentCommentId // El hilo es el comentario padre
                                        ))
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("SocialDebug", "Error amigo $friendId: ${e.message}")
                        }
                        activities
                    }
                }

                val allActivities = activitiesDeferred.awaitAll().flatten()
                    .sortedByDescending { it.timestamp }

                _uiState.value = Resource.Success(allActivities)

            } catch (e: Exception) {
                _uiState.value = Resource.Error(e)
            }
        }
    }

    private suspend fun getBookInfo(bookId: String): Pair<String, String> {
        return try {
            val doc = db.collection("books").document(bookId).get().await()
            if (doc.exists()) Pair(doc.getString("title") ?: "Libro", doc.getString("thumbnail") ?: "")
            else {
                val apiBook = booksRepository.getBookDetail(bookId).getOrNull()
                Pair(apiBook?.title ?: "Libro", apiBook?.imageUrl ?: "")
            }
        } catch (e: Exception) { Pair("Libro", "") }
    }
}