package com.example.topbooks.ui.book

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.repository.BooksRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// --- ESTADO DEL DETALLE ---
data class BookDetailState(
    val book: Book? = null,
    val authorImageUrl: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isBookSaved: Boolean = false,
    val savedInList: String? = null,
    val isReviewing: Boolean = false
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
            }.onFailure { error ->
                _uiState.update { it.copy(error = error.message, isLoading = false) }
            }
        }
    }

    private fun checkIfBookIsSaved(bookId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .collection("favorites").document(bookId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val list = doc.getString("list") ?: "Favoritos"
                    _uiState.update { it.copy(isBookSaved = true, savedInList = list) }
                }
            }
    }

    fun addToList(book: Book, listName: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                ensureBookInGlobalCollection(book)

                val userFavRef = db.collection("users").document(uid)
                    .collection("favorites").document(book.id)

                val userFavData = hashMapOf(
                    "bookId" to book.id,
                    "title" to book.title,
                    "imageUrl" to book.imageUrl,
                    "list" to listName,
                    "addedAt" to System.currentTimeMillis()
                )
                userFavRef.set(userFavData).await()

                _uiState.update { it.copy(isBookSaved = true, savedInList = listName) }
            } catch (e: Exception) {
                Log.e("Firestore", "Error al guardar favorito: ${e.message}")
            }
        }
    }

    /**
     * CORRECCIÓN: Ahora guarda IDs (Strings) en lugar de DocumentReferences
     * para coincidir con el nuevo modelo de Comment y evitar errores de serialización.
     */
    fun saveReview(book: Book, rating: Int, text: String, chapter: String, onSuccess: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        _uiState.update { it.copy(isReviewing = true) }

        viewModelScope.launch {
            try {
                // Aseguramos que el libro exista en la colección global para poder mostrarlo en el muro
                ensureBookInGlobalCollection(book)

                val newCommentRef = db.collection("comments").document()

                // Mapeamos los datos usando IDs de tipo String
                val commentData = hashMapOf(
                    "commentId" to newCommentRef.id,
                    "bookId" to book.id,  // ID del libro como String
                    "userId" to uid,      // ID del usuario como String
                    "rating" to rating,
                    "text" to text,
                    "chapter" to chapter,
                    "likes" to 0,
                    "edited" to false,
                    "createAt" to com.google.firebase.Timestamp.now()
                )

                newCommentRef.set(commentData).await()
                Log.d("BookDetailVM", "Reseña guardada con éxito: ${newCommentRef.id}")
                onSuccess()
            } catch (e: Exception) {
                Log.e("Firestore", "Error al guardar la reseña: ${e.message}")
            } finally {
                _uiState.update { it.copy(isReviewing = false) }
            }
        }
    }

    private suspend fun ensureBookInGlobalCollection(book: Book) {
        val globalBookRef = db.collection("books").document(book.id)
        val doc = globalBookRef.get().await()
        if (!doc.exists()) {
            val globalData = hashMapOf(
                "title" to book.title,
                "thumbnail" to book.imageUrl,
                "createdAt" to com.google.firebase.Timestamp.now()
            )
            globalBookRef.set(globalData).await()
        }
    }
}