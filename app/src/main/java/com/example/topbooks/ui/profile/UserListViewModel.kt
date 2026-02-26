package com.example.topbooks.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Comment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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

data class BookmarkUI(
    val id: String = "",
    val bookId: String = "",
    val bookTitle: String = "",
    val quote: String = "",
    val chapter: String = "",
    val page: String = "",
    val isPublic: Boolean = true
)

data class UserListState(
    val friends: List<SimpleUser> = emptyList(),
    val readBooks: List<SimpleBook> = emptyList(),
    val reviews: List<Comment> = emptyList(),
    val bookmarks: List<BookmarkUI> = emptyList(), // 🟢 NUEVA LISTA
    val isLoading: Boolean = false
)

class UserListViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
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
                    "journals" -> fetchJournals(userId)
                    "bookmarks" -> fetchBookmarks(userId) // 🟢 Actualizado
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
            SimpleUser(uid = doc.id, name = doc.getString("displayName") ?: "Usuario", photo = doc.getString("photoURL") ?: "")
        }
        _uiState.update { it.copy(friends = list, isLoading = false) }
    }

    private suspend fun fetchReadBooks(userId: String) {
        val snapshot = db.collection("users").document(userId).collection("read_books").get().await()
        val list = snapshot.documents.map { doc ->
            SimpleBook(id = doc.id, title = doc.getString("title") ?: "Libro", imageUrl = doc.getString("imageUrl") ?: "")
        }
        _uiState.update { it.copy(readBooks = list, isLoading = false) }
    }

    private suspend fun fetchReviews(userId: String) {
        val snapshot = db.collection("reviews").whereEqualTo("userId", userId).get().await()
        val rawReviews = snapshot.toObjects(Comment::class.java)
        enrichAndSetReviews(rawReviews, userId)
    }

    private suspend fun fetchBookmarks(userId: String) {
        val isMe = auth.currentUser?.uid == userId
        val query = db.collection("users").document(userId).collection("bookmarks")

        val snapshot = if (isMe) query.get().await() else query.whereEqualTo("isPublic", true).get().await()

        val list = snapshot.documents.map { doc ->
            viewModelScope.async {
                val bookId = doc.getString("bookId") ?: doc.id
                var title = "Libro Desconocido"
                try {
                    val bookDoc = db.collection("books").document(bookId).get().await()
                    if (bookDoc.exists()) title = bookDoc.getString("title") ?: "Sin título"
                } catch (e: Exception) {}

                BookmarkUI(
                    id = doc.id,
                    bookId = bookId,
                    bookTitle = title,
                    quote = doc.getString("quote") ?: "",
                    chapter = doc.getString("chapter") ?: "",
                    page = doc.getString("page") ?: "",
                    isPublic = doc.getBoolean("isPublic") ?: true
                )
            }
        }.awaitAll()

        _uiState.update { it.copy(bookmarks = list, isLoading = false) }
    }

    // 🟢 NUEVAS FUNCIONES DE ACTUALIZAR Y BORRAR MARCADOR
    fun updateBookmark(bookmark: BookmarkUI) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val data = mapOf(
                    "bookId" to bookmark.bookId,
                    "userId" to uid,
                    "page" to bookmark.page,
                    "chapter" to bookmark.chapter,
                    "quote" to bookmark.quote,
                    "isPublic" to bookmark.isPublic,
                    "updatedAt" to System.currentTimeMillis()
                )
                db.collection("users").document(uid).collection("bookmarks").document(bookmark.id).update(data).await()

                val publicRef = db.collection("public_bookmarks").document("${uid}_${bookmark.bookId}")
                if (bookmark.isPublic) publicRef.set(data, SetOptions.merge()).await() else publicRef.delete().await()

                fetchBookmarks(uid) // Recargar lista
            } catch(e: Exception){}
        }
    }

    fun deleteBookmark(bookmarkId: String, bookId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(uid).collection("bookmarks").document(bookmarkId).delete().await()
                db.collection("public_bookmarks").document("${uid}_${bookId}").delete().await()
                fetchBookmarks(uid) // Recargar lista
            } catch(e: Exception){}
        }
    }

    private suspend fun fetchComments(userId: String) {
        val snapshot = db.collection("comments").whereEqualTo("userId", userId).get().await()
        val rawComments = snapshot.toObjects(Comment::class.java)
        enrichAndSetReviews(rawComments, userId)
    }

    private suspend fun fetchJournals(userId: String) {
        val query = db.collection("users").document(userId).collection("journals")
        val snapshot = if (userId == auth.currentUser?.uid) query.get().await() else query.whereEqualTo("isPublic", true).get().await()

        val rawJournals = snapshot.documents.mapNotNull { doc ->
            Comment(
                commentId = doc.id, bookId = doc.getString("bookId") ?: "", userId = userId,
                text = doc.getString("moments")?.takeIf { it.isNotBlank() } ?: "Diario de lectura del libro.",
                rating = doc.getLong("mainRating")?.toInt() ?: 0,
                bookTitle = doc.getString("bookTitle") ?: "Libro", bookImageUrl = doc.getString("bookImageUrl") ?: ""
            )
        }
        enrichAndSetReviews(rawJournals, userId)
    }

    private suspend fun enrichAndSetReviews(rawItems: List<Comment>, userId: String) {
        val userDoc = try { db.collection("users").document(userId).get().await() } catch(e:Exception){ null }
        val userName = userDoc?.getString("displayName") ?: "Usuario"
        val userPhoto = userDoc?.getString("photoURL") ?: "capibara_1"

        val enriched = rawItems.map { item ->
            viewModelScope.async {
                var current = item.copy(userName = userName, userPhotoUrl = userPhoto)
                try {
                    if (current.bookTitle.isEmpty() || current.bookTitle == "Libro") {
                        val bookDoc = db.collection("books").document(item.bookId).get().await()
                        if (bookDoc.exists()) {
                            current = current.copy(bookTitle = bookDoc.getString("title") ?: "Sin título", bookImageUrl = bookDoc.getString("thumbnail") ?: "")
                        }
                    }
                } catch (e: Exception) { }
                current
            }
        }.awaitAll()

        _uiState.update { it.copy(reviews = enriched, isLoading = false) }
    }

    fun deleteComment(commentId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("comments").document(commentId).delete().await()
                fetchComments(uid) // Recargar la lista al instante
            } catch (e: Exception) {
                Log.e("UserListVM", "Error al borrar comentario: ${e.message}")
            }
        }
    }
}