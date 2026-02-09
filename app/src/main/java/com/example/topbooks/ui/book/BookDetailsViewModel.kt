package com.example.topbooks.ui.book

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.network.RetrofitClient
import com.example.topbooks.data.repository.BooksRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Creamos un estado específico para la pantalla de detalle que incluya la foto del autor
data class BookDetailState(
    val book: Book? = null,
    val authorImageUrl: String? = null, // Aquí guardaremos la URL de la foto
    val isLoading: Boolean = false,
    val error: String? = null,
    val isBookSaved: Boolean = false,
    val savedInList: String? = null
)

class BookDetailViewModel(private val repository: BooksRepository = BooksRepository()) : ViewModel() {

    private val _uiState = MutableStateFlow(BookDetailState())
    val uiState: StateFlow<BookDetailState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun getBookById(bookId: String) {

        val uid = auth.currentUser?.uid

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = repository.getBookDetail(bookId)

            if (result.isSuccess) {
                // getBookDetail devuelve un solo objeto Book
                val foundBook = result.getOrNull()

                if (foundBook != null) {
                    //verificamos si el usuario lo tiene guardado
                    var isSaved = false
                    var listName: String? = null

                    if (uid != null) {
                        val doc = db.collection("users").document(uid)
                            .collection("favorites").document(foundBook.id)
                            .get()
                            .await()

                        if (doc.exists()) {
                            isSaved = true
                            listName = doc.getString("list")
                        }
                    }

                    // Actualización de estado unificada
                    _uiState.update {
                        it.copy(
                            book = foundBook,
                            isBookSaved = isSaved,
                            savedInList = listName,
                            isLoading = false
                        )
                    }

                    // Buscamos la foto del autor si existe
                    if (foundBook.authors.isNotEmpty()) {
                        fetchAuthorPhoto(foundBook.authors.first())
                    }
                } else {
                    _uiState.value = _uiState.value.copy(error = "Libro no encontrado", isLoading = false)
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    error = result.exceptionOrNull()?.message ?: "Error de conexión",
                    isLoading = false
                )
            }
        }
    }

    private suspend fun fetchAuthorPhoto(authorName: String) {
        try {
            // URL de búsqueda de Open Library
            val searchUrl = "https://openlibrary.org/search/authors.json?q=${authorName}"

            // Llamada directa usando la instancia de Retrofit
            val response = RetrofitClient.instance.searchAuthorExternal(searchUrl)

            if (response.isSuccessful && response.body()?.docs?.isNotEmpty() == true) {
                // OpenLibrary devuelve la clave así: "/authors/OL1234567"
                val key = response.body()!!.docs.first().key // ej: "/authors/OL34184A"
                val cleanId = key?.removePrefix("/authors/") // ej: "OL34184A"

                if (cleanId != null) {
                    // Construimos la URL final de la imagen
                    val photoUrl = "https://covers.openlibrary.org/a/olid/$cleanId-L.jpg"

                    // Actualizamos el estado
                    _uiState.value = _uiState.value.copy(authorImageUrl = photoUrl)
                }
            }
        } catch (e: Exception) {
            // Si falla, no hacemos nada (se quedará el icono por defecto)
            e.printStackTrace()
        }
    }

    fun addToList(book: Book, listName: String) {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                // 1. Guardar/Actualizar en la colección GLOBAL de libros
                val globalBookRef = db.collection("books").document(book.id)
                val globalDoc = globalBookRef.get().await()

                if (!globalDoc.exists()) {
                    val globalData = hashMapOf(
                        "bookId" to book.id,
                        "title" to book.title,
                        "authors" to book.authors,
                        "description" to book.description,
                        "thumbnail" to book.imageUrl, // API externa
                        "source" to "Google Books", //TODO controlar las diferentes APIS
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )
                    globalBookRef.set(globalData).await()
                }

                // 2. Guardar en la subcolección del USUARIO
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

                // Actualizamos el estado local para reflejar el cambio en la UI
                _uiState.update {
                    it.copy(
                        isBookSaved = true,
                        savedInList = listName
                    )
                }

            } catch (e: Exception) {
                Log.e("Firestore", "Error al guardar: ${e.message}")
            }
        }
    }
}