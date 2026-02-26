package com.example.topbooks.ui.book

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.model.Review
import com.example.topbooks.data.repository.BooksRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class BookDetailState(
    val book: Book? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFavorite: Boolean = false,
    val readingStatus: String? = null,
    val reviews: List<Review> = emptyList()
)

class BookDetailViewModel(private val repository: BooksRepository = BooksRepository()) : ViewModel() {

    private val _uiState = MutableStateFlow(BookDetailState())
    val uiState: StateFlow<BookDetailState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun getBook(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getBookDetail(id).onSuccess { book ->
                _uiState.update { it.copy(book = book, isLoading = false) }
                checkIfBookIsSaved(book.id)
                fetchBookReviews(book.id)
            }.onFailure { error ->
                _uiState.update { it.copy(error = error.message, isLoading = false) }
            }
        }
    }

    // Maneja el botón de Favoritos independientemente
    fun toggleFavorite(book: Book) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val newState = !_uiState.value.isFavorite
            _uiState.update { it.copy(isFavorite = newState) }

            try {
                ensureBookInGlobal(book)
                val ref = db.collection("users").document(uid).collection("favorites").document(book.id)

                if (newState) {
                    val data = hashMapOf(
                        "bookId" to book.id, "title" to book.title, "imageUrl" to book.imageUrl,
                        "isFavorite" to true, "list" to "Favoritos", "addedAt" to System.currentTimeMillis()
                    )
                    ref.set(data, SetOptions.merge()).await()
                } else {
                    ref.update("isFavorite", false).await()
                    cleanupIfEmpty(uid, book.id)
                }
            } catch (e: Exception) { Log.e("BookDetailVM", "Error toggle fav: ${e.message}") }
        }
    }

    //Maneja Leídos y Pendientes de forma exclusiva
    fun toggleReadingStatus(book: Book, status: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val currentStatus = _uiState.value.readingStatus
            // Si pulsa el mismo botón que ya estaba activo, lo desmarcamos (null)
            val newStatus = if (currentStatus == status) null else status
            _uiState.update { it.copy(readingStatus = newStatus) }

            try {
                ensureBookInGlobal(book)
                val ref = db.collection("users").document(uid).collection("favorites").document(book.id)

                if (newStatus != null) {
                    val data = hashMapOf(
                        "bookId" to book.id, "title" to book.title, "imageUrl" to book.imageUrl,
                        "readingStatus" to newStatus, "list" to newStatus, "addedAt" to System.currentTimeMillis()
                    )
                    ref.set(data, SetOptions.merge()).await()

                    // También lo guardamos en la colección antigua 'read_books' por si otras partes de la app lo necesitan
                    if (newStatus == "Leídos") {
                        db.collection("users").document(uid).collection("read_books").document(book.id)
                            .set(hashMapOf("bookId" to book.id, "title" to book.title, "imageUrl" to book.imageUrl), SetOptions.merge()).await()
                    } else {
                        db.collection("users").document(uid).collection("read_books").document(book.id).delete().await()
                    }
                } else {
                    ref.update("readingStatus", null).await()
                    db.collection("users").document(uid).collection("read_books").document(book.id).delete().await()
                    cleanupIfEmpty(uid, book.id)
                }
            } catch (e: Exception) { Log.e("BookDetailVM", "Error toggle status: ${e.message}") }
        }
    }

    // Borra el documento si no es favorito y no tiene estado de lectura
    private suspend fun cleanupIfEmpty(uid: String, bookId: String) {
        val ref = db.collection("users").document(uid).collection("favorites").document(bookId)
        val doc = ref.get().await()
        if (doc.exists()) {
            val isFav = doc.getBoolean("isFavorite") == true
            val status = doc.getString("readingStatus")
            if (!isFav && status == null) ref.delete().await()
        }
    }

    // Actualizado para leer el nuevo formato (con retrocompatibilidad)
    private fun checkIfBookIsSaved(bookId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("favorites").document(bookId).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val oldList = doc.getString("list")
                val isFav = doc.getBoolean("isFavorite") ?: (oldList == "Favoritos")
                val status = doc.getString("readingStatus") ?: if (oldList == "Leídos" || oldList == "Pendientes") oldList else null
                _uiState.update { it.copy(isFavorite = isFav, readingStatus = status) }
            } else {
                _uiState.update { it.copy(isFavorite = false, readingStatus = null) }
            }
        }
    }

    fun addBookmark(bookId: String, page: String, quote: String, chapter: String, isPublic: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val privateRef = db.collection("users").document(uid).collection("bookmarks").document(bookId)
                val data = hashMapOf(
                    "bookId" to bookId, "userId" to uid, "page" to page, "quote" to quote,
                    "chapter" to chapter, "isPublic" to isPublic, "updatedAt" to System.currentTimeMillis()
                )
                privateRef.set(data, SetOptions.merge()).await()

                val publicRef = db.collection("public_bookmarks").document("${uid}_${bookId}")
                if (isPublic) publicRef.set(data, SetOptions.merge()).await() else publicRef.delete().await()
            } catch (e: Exception) { }
        }
    }

    private fun fetchBookReviews(bookId: String) {
        viewModelScope.launch {
            try {
                val res = db.collection("reviews").whereEqualTo("bookId", bookId).orderBy("createAt", Query.Direction.DESCENDING).get().await()
                val enriched = res.toObjects(Review::class.java).map { review ->
                    async {
                        var current = review
                        try {
                            if (review.userId.isNotEmpty()) {
                                val userDoc = db.collection("users").document(review.userId).get().await()
                                if (userDoc.exists()) {
                                    current = current.copy(userName = userDoc.getString("displayName") ?: "Anónimo", userPhotoUrl = userDoc.getString("photoURL") ?: "")
                                }
                            }
                        } catch (e: Exception) { }
                        current
                    }
                }.awaitAll()
                _uiState.update { it.copy(reviews = enriched) }
            } catch (e: Exception) { }
        }
    }

    fun saveReview(book: Book, rating: Int, text: String, onSuccess: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                ensureBookInGlobal(book)
                val ref = db.collection("reviews").document()
                val data = hashMapOf("id" to ref.id, "bookId" to book.id, "userId" to uid, "rating" to rating, "text" to text, "createAt" to com.google.firebase.Timestamp.now())
                ref.set(data).await()
                fetchBookReviews(book.id)
                onSuccess()
            } catch (e: Exception) { }
        }
    }

    private suspend fun ensureBookInGlobal(book: Book) {
        val ref = db.collection("books").document(book.id)
        if (!ref.get().await().exists()) {
            ref.set(hashMapOf("title" to book.title, "thumbnail" to book.imageUrl, "createdAt" to com.google.firebase.Timestamp.now())).await()
        }
    }

    fun saveComment(book: Book, text: String, chapter: String, onSuccess: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                ensureBookInGlobal(book)
                val ref = db.collection("comments").document()
                val data = hashMapOf("commentId" to ref.id, "bookId" to book.id, "userId" to uid, "text" to text, "chapter" to chapter, "createAt" to com.google.firebase.Timestamp.now(), "replies" to emptyList<Any>())
                ref.set(data).await()
                onSuccess()
            } catch (e: Exception) { }
        }
    }
}