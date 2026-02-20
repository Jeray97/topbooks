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
    val isBookSaved: Boolean = false,
    val savedInList: String? = null,
    val isReviewing: Boolean = false,
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

    // --- LÓGICA DE MARCADORES (Página y Texto obligatorios, Capítulo opcional) ---
    fun addBookmark(
        bookId: String,
        page: String,
        quote: String,
        chapter: String,
        isPublic: Boolean
    ) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val bookmarkRef = db.collection("users").document(uid)
                    .collection("bookmarks").document() // Genera ID automático

                val data = hashMapOf(
                    "id" to bookmarkRef.id,
                    "bookId" to bookId,
                    "userId" to uid,
                    "page" to page,
                    "quote" to quote,
                    "chapter" to chapter,
                    "isPublic" to isPublic,
                    "createdAt" to com.google.firebase.Timestamp.now()
                )

                bookmarkRef.set(data).await()

                // Si es público, se guarda en la colección global para que otros lo vean
                if (isPublic) {
                    db.collection("public_bookmarks").document(bookmarkRef.id).set(data)
                }
            } catch (e: Exception) {
                Log.e("BookDetailVM", "Error al guardar marcador: ${e.message}")
            }
        }
    }

    // --- LÓGICA DE RESEÑAS Y LISTAS (Tus funciones originales) ---
    private fun fetchBookReviews(bookId: String) {
        viewModelScope.launch {
            try {
                val result = db.collection("reviews")
                    .whereEqualTo("bookId", bookId)
                    .orderBy("createAt", Query.Direction.DESCENDING)
                    .get().await()

                val rawReviews = result.toObjects(Review::class.java)
                val enrichedReviews = rawReviews.map { review ->
                    async {
                        var enriched = review
                        try {
                            if (review.userId.isNotEmpty()) {
                                val userDoc = db.collection("users").document(review.userId).get().await()
                                if (userDoc.exists()) {
                                    enriched = enriched.copy(
                                        userName = userDoc.getString("displayName") ?: "Anónimo",
                                        userPhotoUrl = userDoc.getString("photoURL") ?: ""
                                    )
                                }
                            }
                        } catch (e: Exception) { }
                        enriched
                    }
                }.awaitAll()
                _uiState.update { it.copy(reviews = enrichedReviews) }
            } catch (e: Exception) { Log.e("BookDetailVM", "Error reviews") }
        }
    }

    private fun checkIfBookIsSaved(bookId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("favorites").document(bookId).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                _uiState.update { it.copy(isBookSaved = true, savedInList = doc.getString("list")) }
            } else {
                _uiState.update { it.copy(isBookSaved = false, savedInList = null) }
            }
        }
    }

    fun addToList(book: Book, listName: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                ensureBookInGlobalCollection(book)
                val userFavRef = db.collection("users").document(uid).collection("favorites").document(book.id)
                val userFavData = hashMapOf("bookId" to book.id, "title" to book.title, "imageUrl" to book.imageUrl, "list" to listName, "addedAt" to System.currentTimeMillis())
                userFavRef.set(userFavData).await()
                _uiState.update { it.copy(isBookSaved = true, savedInList = listName) }
            } catch (e: Exception) { }
        }
    }

    fun removeFromList(bookId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(uid).collection("favorites").document(bookId).delete().await()
                _uiState.update { it.copy(isBookSaved = false, savedInList = null) }
            } catch (e: Exception) { }
        }
    }

    fun saveReview(book: Book, rating: Int, text: String, onSuccess: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        _uiState.update { it.copy(isReviewing = true) }
        viewModelScope.launch {
            try {
                ensureBookInGlobalCollection(book)
                val newReviewRef = db.collection("reviews").document()
                val reviewData = hashMapOf("id" to newReviewRef.id, "bookId" to book.id, "userId" to uid, "rating" to rating, "text" to text, "likes" to 0, "createAt" to com.google.firebase.Timestamp.now())
                newReviewRef.set(reviewData).await()
                fetchBookReviews(book.id)
                onSuccess()
            } catch (e: Exception) { }
            finally { _uiState.update { it.copy(isReviewing = false) } }
        }
    }

    private suspend fun ensureBookInGlobalCollection(book: Book) {
        val globalBookRef = db.collection("books").document(book.id)
        if (!globalBookRef.get().await().exists()) {
            val globalData = hashMapOf("title" to book.title, "thumbnail" to book.imageUrl, "createdAt" to com.google.firebase.Timestamp.now())
            globalBookRef.set(globalData).await()
        }
    }
}