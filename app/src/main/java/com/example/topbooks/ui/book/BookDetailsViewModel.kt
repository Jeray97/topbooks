package com.example.topbooks.ui.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.network.RetrofitClient
import com.example.topbooks.data.repository.BooksRepository
import com.example.topbooks.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Creamos un estado específico para la pantalla de detalle que incluya la foto del autor
data class BookDetailState(
    val book: Book? = null,
    val authorImageUrl: String? = null, // Aquí guardaremos la URL de la foto
    val isLoading: Boolean = false,
    val error: String? = null
)

class BookDetailViewModel(private val repository: BooksRepository = BooksRepository()) : ViewModel() {

    private val _uiState = MutableStateFlow(BookDetailState())
    val uiState: StateFlow<BookDetailState> = _uiState.asStateFlow()

    fun getBookById(bookId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = repository.getBookDetail(bookId)

            if (result.isSuccess) {
                // getBookDetail devuelve un solo objeto Book
                val foundBook = result.getOrNull()

                if (foundBook != null) {
                    _uiState.value = _uiState.value.copy(book = foundBook, isLoading = false)

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

            // Llamada directa usando la instancia de Retrofit que ya tienes
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
}