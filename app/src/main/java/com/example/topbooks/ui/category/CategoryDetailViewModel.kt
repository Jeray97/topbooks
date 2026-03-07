package com.example.topbooks.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.repository.BooksRepository
import com.example.topbooks.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CategoryDetailViewModel (private val repository: BooksRepository = BooksRepository()) : ViewModel() {

    // --- ESTADO ---
    private val _categoryBooks = MutableStateFlow<Resource<List<Book>>>(Resource.Loading)
    val booksState: StateFlow<Resource<List<Book>>> = _categoryBooks.asStateFlow()

    // --- FUNCIÓN PRINCIPAL ---
    fun fetchBooksByCategory(query: String) {
        viewModelScope.launch {
            _categoryBooks.value = Resource.Loading

            // Llamamos al repositorio con la query exacta que nos pasan desde la UI (ej: "subject:Romance")
            // Le pasamos filterModern = true para que aplique nuestros filtros de fama y año
            val result = repository.getBooks(query, filterModern = true)

            if(result.isSuccess) {
                val books = result.getOrDefault(emptyList())
                _categoryBooks.value = Resource.Success(books)
            } else {
                val error = result.exceptionOrNull() ?: Exception("Error desconocido")
                _categoryBooks.value = Resource.Error(error)
            }
        }
    }
}