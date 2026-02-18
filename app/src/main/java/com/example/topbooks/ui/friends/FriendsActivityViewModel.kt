package com.example.topbooks.ui.friends

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.model.Comment
import com.example.topbooks.data.model.Review
import com.example.topbooks.data.repository.BooksRepository
import com.example.topbooks.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

// --- MODELO UNIFICADO PARA LA UI ---
data class FriendActivityItem(
    val id: String,
    val type: ActivityType, // REVIEW, COMMENT, FAVORITE
    val friendName: String,
    val friendPhotoUrl: String,
    val bookId: String,
    val bookTitle: String,
    val bookImageUrl: String,
    val content: String, // Texto de la reseña o comentario
    val rating: Int,     // 0 si es favorito
    val timestamp: Date?
)

enum class ActivityType { REVIEW, COMMENT, FAVORITE }

class FriendsActivityViewModel(
    private val repository: BooksRepository = BooksRepository()
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _activityState = MutableStateFlow<Resource<List<FriendActivityItem>>>(Resource.Loading)
    val activityState: StateFlow<Resource<List<FriendActivityItem>>> = _activityState.asStateFlow()

    init {
        loadFriendsActivity()
    }

    fun loadFriendsActivity() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _activityState.value = Resource.Error(Exception("No usuario"))
            return
        }

        viewModelScope.launch {
            _activityState.value = Resource.Loading
            try {
                // 1. Obtener lista de amigos
                val friendsSnapshot = db.collection("users").document(uid)
                    .collection("friends").get().await()

                if (friendsSnapshot.isEmpty) {
                    _activityState.value = Resource.Success(emptyList())
                    return@launch
                }

                val friendIds = friendsSnapshot.documents.map { it.id }

                // 2. Cargar actividades en paralelo
                val activities = coroutineScope {
                    val reviewsDeferred = async { fetchReviews(friendIds) }
                    val commentsDeferred = async { fetchComments(friendIds) }
                    val favoritesDeferred = async { fetchFavorites(friendIds) }

                    // Esperamos a que terminen todas y unimos las listas
                    val allActivities = reviewsDeferred.await() + commentsDeferred.await() + favoritesDeferred.await()

                    // Ordenamos por fecha descendente (lo más nuevo arriba)
                    allActivities.sortedByDescending { it.timestamp }
                }

                _activityState.value = Resource.Success(activities)

            } catch (e: Exception) {
                Log.e("FriendsActivityVM", "Error: ${e.message}")
                _activityState.value = Resource.Error(e)
            }
        }
    }

    // --- 1. BUSCAR RESEÑAS DE AMIGOS ---
    private suspend fun fetchReviews(friendIds: List<String>): List<FriendActivityItem> {
        if (friendIds.isEmpty()) return emptyList()
        // Firestore limita 'whereIn' a 10. Hacemos lotes si es necesario o cogemos los 10 primeros.
        val safeIds = friendIds.take(10)

        return try {
            val snapshot = db.collection("reviews")
                .whereIn("userId", safeIds)
                .orderBy("createAt", Query.Direction.DESCENDING)
                .limit(20)
                .get().await()

            val items = snapshot.toObjects(Review::class.java)
            enrichItems(items, ActivityType.REVIEW)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- 2. BUSCAR COMENTARIOS DE AMIGOS ---
    private suspend fun fetchComments(friendIds: List<String>): List<FriendActivityItem> {
        if (friendIds.isEmpty()) return emptyList()
        val safeIds = friendIds.take(10)

        return try {
            val snapshot = db.collection("comments")
                .whereIn("userId", safeIds)
                .orderBy("createAt", Query.Direction.DESCENDING)
                .limit(20)
                .get().await()

            val items = snapshot.toObjects(Comment::class.java)
            enrichItems(items, ActivityType.COMMENT)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- 3. BUSCAR FAVORITOS DE AMIGOS ---
    private suspend fun fetchFavorites(friendIds: List<String>): List<FriendActivityItem> = coroutineScope {
        // Iteramos por cada amigo para ver sus últimos favoritos
        val tasks = friendIds.map { friendId ->
            async {
                try {
                    val snapshot = db.collection("users").document(friendId)
                        .collection("favorites")
                        .orderBy("addedAt", Query.Direction.DESCENDING)
                        .limit(3)
                        .get().await()

                    // Mapeamos manualmente el favorito a nuestro objeto ActivityItem
                    // Necesitamos obtener datos del usuario (amigo) también
                    val userDoc = db.collection("users").document(friendId).get().await()
                    val friendName = userDoc.getString("displayName") ?: "Amigo"
                    val friendPhoto = userDoc.getString("photoURL") ?: "capibara_1"

                    snapshot.documents.map { doc ->
                        FriendActivityItem(
                            id = doc.id,
                            type = ActivityType.FAVORITE,
                            friendName = friendName,
                            friendPhotoUrl = friendPhoto,
                            bookId = doc.getString("bookId") ?: "",
                            bookTitle = doc.getString("title") ?: "Libro",
                            bookImageUrl = doc.getString("imageUrl") ?: "",
                            content = "Ha añadido este libro a favoritos.",
                            rating = 0,
                            timestamp = Date(doc.getLong("addedAt") ?: System.currentTimeMillis())
                        )
                    }
                } catch (e: Exception) {
                    emptyList<FriendActivityItem>()
                }
            }
        }
        tasks.awaitAll().flatten()
    }

    // --- HELPER: ENRIQUECER DATOS (BUSCAR INFO LIBRO Y USUARIO) ---
    // Esta función convierte Reviews o Comments (que tienen IDs) en FriendActivityItem completos
    private suspend fun enrichItems(rawItems: List<Any>, type: ActivityType): List<FriendActivityItem> = coroutineScope {
        rawItems.map { item ->
            async {
                try {
                    var userId = ""
                    var bookId = ""
                    var text = ""
                    var rating = 0
                    var date: Date? = null
                    var id = ""

                    if (item is Review) {
                        id = item.id
                        userId = item.userId
                        bookId = item.bookId
                        text = item.text
                        rating = item.rating
                        date = item.createAt
                    } else if (item is Comment) {
                        id = item.commentId
                        userId = item.userId
                        bookId = item.bookId
                        text = item.text
                        rating = item.rating
                        date = item.createAt
                    }

                    // 1. Datos Usuario
                    val userDoc = db.collection("users").document(userId).get().await()
                    val friendName = userDoc.getString("displayName") ?: "Usuario"
                    val friendPhoto = userDoc.getString("photoURL") ?: "capibara_1"

                    // 2. Datos Libro (Si no tenemos el título, lo buscamos.
                    // En review/comment no guardamos título, así que hay que buscarlo o cachearlo)
                    // Para optimizar, intentamos obtenerlo de Firestore 'books' global si existe, o API
                    val bookDoc = db.collection("books").document(bookId).get().await()
                    var bookTitle = "Libro"
                    var bookImage = ""

                    if (bookDoc.exists()) {
                        bookTitle = bookDoc.getString("title") ?: "Sin título"
                        bookImage = bookDoc.getString("thumbnail") ?: ""
                    } else {
                        // Fallback API
                        val apiBook = repository.getBookDetail(bookId).getOrNull()
                        bookTitle = apiBook?.title ?: "Desconocido"
                        bookImage = apiBook?.imageUrl ?: ""
                    }

                    FriendActivityItem(
                        id = id,
                        type = type,
                        friendName = friendName,
                        friendPhotoUrl = friendPhoto,
                        bookId = bookId,
                        bookTitle = bookTitle,
                        bookImageUrl = bookImage,
                        content = text,
                        rating = rating,
                        timestamp = date
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }
}