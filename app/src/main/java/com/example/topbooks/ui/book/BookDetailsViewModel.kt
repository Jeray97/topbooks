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

    // --- NUEVA FUNCIÓN MARCADORES ---
    fun addBookmark(bookId: String, page: String, quote: String, chapter: String, isPublic: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val ref = db.collection("users").document(uid).collection("bookmarks").document()
                val data = hashMapOf(
                    "id" to ref.id,
                    "bookId" to bookId,
                    "userId" to uid,
                    "page" to page,
                    "quote" to quote,
                    "chapter" to chapter,
                    "isPublic" to isPublic,
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
                ref.set(data).await()
                if (isPublic) {
                    db.collection("public_bookmarks").document(ref.id).set(data)
                }
            } catch (e: Exception) { Log.e("BookDetailVM", "Error bookmarking") }
        }
    }

    // --- RESTO DE LÓGICA (TUS FUNCIONES) ---
    private fun fetchBookReviews(bookId: String) {
        viewModelScope.launch {
            try {
                val res = db.collection("reviews").whereEqualTo("bookId", bookId).orderBy("createAt", Query.Direction.DESCENDING).get().await()
                val raw = res.toObjects(Review::class.java)
                val enriched = raw.map { review ->
                    async {
                        var current = review
                        try {
                            if (review.userId.isNotEmpty()) {
                                val userDoc = db.collection("users").document(review.userId).get().await()
                                if (userDoc.exists()) {
                                    current = current.copy(
                                        userName = userDoc.getString("displayName") ?: "Anónimo",
                                        userPhotoUrl = userDoc.getString("photoURL") ?: ""
                                    )
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

    private fun checkIfBookIsSaved(bookId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("favorites").document(bookId).get().addOnSuccessListener { doc ->
            _uiState.update { it.copy(isBookSaved = doc.exists(), savedInList = doc.getString("list")) }
        }
    }

    fun addToList(book: Book, listName: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                ensureBookInGlobal(book)
                val data = hashMapOf("bookId" to book.id, "title" to book.title, "imageUrl" to book.imageUrl, "list" to listName, "addedAt" to System.currentTimeMillis())
                db.collection("users").document(uid).collection("favorites").document(book.id).set(data).await()
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

    fun addToFavorites(bookId: String) {
        val uid = auth.currentUser?.uid ?: return
        val data = mapOf("bookId" to bookId, "list" to "Favoritos", "timestamp" to System.currentTimeMillis())
        db.collection("users").document(uid).collection("favorites").document(bookId)
            .set(data, SetOptions.merge())
    }

    // NUEVA FUNCIÓN PARA MARCADORES
    fun addBookmark(bookId: String, pageInfo: String) {
        val uid = auth.currentUser?.uid ?: return
        val data = mapOf(
            "bookId" to bookId,
            "pageInfo" to pageInfo,
            "updatedAt" to System.currentTimeMillis()
        )

        // Lo guardamos en una subcolección "bookmarks" para tener un historial si quisiéramos
        db.collection("users").document(uid)
            .collection("bookmarks").document(bookId)
            .set(data, SetOptions.merge())
            .addOnSuccessListener { Log.d("BookVM", "Marcador guardado!") }
    }
}