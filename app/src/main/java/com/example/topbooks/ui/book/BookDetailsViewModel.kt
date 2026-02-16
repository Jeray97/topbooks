package com.example.topbooks.ui.book

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.repository.BooksRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class BookDetailState(
    val book: Book? = null,
    val authorImageUrl: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isBookSaved: Boolean = false,
    val savedInList: String? = null
)

class BookDetailViewModel(private val repository: BooksRepository = BooksRepository()) : ViewModel() {

    private val _uiState = MutableStateFlow(BookDetailState())
    val uiState = _uiState.asStateFlow()

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

        // Revisamos en users/{uid}/favorites/{bookId}
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
                // Guardar en favoritos del usuario
                val userFavRef = db.collection("users").document(uid)
                    .collection("favorites").document(book.id)

                val data = hashMapOf(
                    "bookId" to book.id,
                    "title" to book.title,
                    "imageUrl" to book.imageUrl,
                    "list" to listName,
                    "addedAt" to System.currentTimeMillis()
                )
                userFavRef.set(data).await()

                _uiState.update { it.copy(isBookSaved = true, savedInList = listName) }
            } catch (e: Exception) {
                Log.e("Firestore", "Error: ${e.message}")
            }
        }
    }
}