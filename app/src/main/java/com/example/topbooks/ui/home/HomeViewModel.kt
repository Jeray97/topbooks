package com.example.topbooks.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.R
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.repository.BooksRepository
import com.example.topbooks.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BooksRepository()

    private val _categoryBooks = MutableStateFlow<Resource<List<Book>>>(Resource.Loading)
    val categoryBooks: StateFlow<Resource<List<Book>>> = _categoryBooks.asStateFlow()

    private val _recommendedBooks = MutableStateFlow<Resource<List<Book>>>(Resource.Loading)
    val recommendedBooks: StateFlow<Resource<List<Book>>> = _recommendedBooks.asStateFlow()

    private val _friendsBooks = MutableStateFlow<Resource<List<Book>>>(Resource.Loading)
    val friendsBooks: StateFlow<Resource<List<Book>>> = _friendsBooks.asStateFlow()

    init {
        loadInitialData()
        fetchRecommendedBooks()
        fetchFriendsBooks()
    }

    private fun loadInitialData() {
        // Carga inicial usando el string de recurso para "Fantasía"
        val initialCategory = getApplication<Application>().getString(R.string.cat_fantasia_text)
        fetchBooks("subject:$initialCategory", _categoryBooks)
    }

    fun onCategorySelected(categoryTerm: String) {
        fetchBooks("subject:$categoryTerm", _categoryBooks)
    }

    private fun fetchRecommendedBooks() {
        viewModelScope.launch {
            _recommendedBooks.value = Resource.Loading

            // Obtenemos el término de búsqueda general (ej: "bestseller" o "ficción")
            val baseQuery = getApplication<Application>().getString(R.string.query_recommended_base)

            // CAMBIO: Sustituimos getBestRatedModernBooks por getBooks con parámetros
            val result = repository.getBooks(
                query = baseQuery,
                orderBy = "relevance", // Ordenamos por relevancia
                filterModern = true    // Activamos el filtro de años (>= 2010)
            )

            if (result.isSuccess) {
                // Tomamos 10 aleatorios para dar variedad en la portada
                _recommendedBooks.value = Resource.Success(result.getOrDefault(emptyList()).shuffled().take(10))
            } else {
                _recommendedBooks.value = Resource.Error(result.exceptionOrNull() ?: Exception("Error"))
            }
        }
    }

    private fun fetchFriendsBooks() {
        viewModelScope.launch {
            _friendsBooks.value = Resource.Loading

            // Para la sección de amigos usamos una categoría fija (ej: Aventura)
            val baseQuery = getApplication<Application>().getString(R.string.cat_aventura_text)

            // Aquí usamos getBooks sin filtros modernos obligatorios
            val result = repository.getBooks("subject:$baseQuery")

            if (result.isSuccess) {
                _friendsBooks.value = Resource.Success(result.getOrDefault(emptyList()).take(10))
            } else {
                _friendsBooks.value = Resource.Error(result.exceptionOrNull() ?: Exception("Error"))
            }
        }
    }

    // Función auxiliar para cargar listas simples por categoría
    private fun fetchBooks(query: String, state: MutableStateFlow<Resource<List<Book>>>) {
        viewModelScope.launch {
            state.value = Resource.Loading
            val result = repository.getBooks(query)
            if (result.isSuccess) {
                state.value = Resource.Success(result.getOrDefault(emptyList()))
            } else {
                state.value = Resource.Error(result.exceptionOrNull() ?: Exception("Error"))
            }
        }
    }
}