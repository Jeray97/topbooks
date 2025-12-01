package com.example.topbooks.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.repository.BooksRepository
import com.example.topbooks.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeViewModel(private val repository: BooksRepository = BooksRepository()) : ViewModel() {

    // --- ESTADOS DE LAS 3 SECCIONES ---
    // Cada una tiene su propio estado (Cargando, Éxito con libros, o Error)

    // 1. Libros de la sección Categorías
    private val _categoryBooks = MutableStateFlow<Resource<List<Book>>>(Resource.Loading)
    val categoryBooks: StateFlow<Resource<List<Book>>> = _categoryBooks.asStateFlow()

    // 2. Libros Recomendados
    private val _recommendedBooks = MutableStateFlow<Resource<List<Book>>>(Resource.Loading)
    val recommendedBooks: StateFlow<Resource<List<Book>>> = _recommendedBooks.asStateFlow()

    // 3. Libros Favoritos (Amigos)
    private val _friendsBooks = MutableStateFlow<Resource<List<Book>>>(Resource.Loading)
    val friendsBooks: StateFlow<Resource<List<Book>>> = _friendsBooks.asStateFlow()

    init {
        // Al arrancar, cargamos datos automáticos
        loadInitialData()
        fetchRecommendedBooks()
    }

    private fun loadInitialData() {
        // Carga inicial:
        // Categoría por defecto: Romance
        fetchBooks("subject:romance", _categoryBooks)

        // Recomendados: "Best Sellers"
        //fetchBooks("best sellers", _recommendedBooks)

        // Amigos: "Misterio" (por poner algo)
        //TODO IMPLEMENTAR POR ESTADISTICAS
        fetchBooks("subject:mystery", _friendsBooks)
    }

    // Esta función la llamaremos cuando se pulsen los botones redondos (Romance, Terror...)
    fun onCategorySelected(category: String) {
        // "subject:" es un filtro especial de Google para buscar por temática
        fetchBooks("subject:$category", _categoryBooks)
    }

    // Función genérica para reutilizar la lógica de carga
    private fun fetchBooks(query: String, state: MutableStateFlow<Resource<List<Book>>>) {
        viewModelScope.launch {
            state.value = Resource.Loading

            val result = repository.getBooks(query)

            if (result.isSuccess) {
                state.value = Resource.Success(result.getOrDefault(emptyList()))
            } else {
                state.value = Resource.Error(result.exceptionOrNull() ?: Exception("Error desconocido"))
            }
        }
    }

    private fun fetchRecommendedBooks() {
        viewModelScope.launch {
            _recommendedBooks.value = Resource.Loading

            val year = Calendar.getInstance().get(Calendar.YEAR)

            val queryfecha = "BlackWater"

            val result = repository.getBooks(
                query = queryfecha, // Buscamos lista por preferencias de usuario TODO
                orderBy = "newest"  // Ordenamos por fecha
            )

            if (result.isSuccess) {
                _recommendedBooks.value = Resource.Success(result.getOrDefault(emptyList()))
            } else {
                _recommendedBooks.value = Resource.Error(result.exceptionOrNull() ?: Exception("Error"))
            }
        }
    }
}