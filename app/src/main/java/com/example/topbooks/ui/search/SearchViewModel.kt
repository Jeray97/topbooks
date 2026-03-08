package com.example.topbooks.ui.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.repository.BooksRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(private val repository: BooksRepository = BooksRepository()) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<Book>>(emptyList())
    val searchResults: StateFlow<List<Book>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        searchJob?.cancel()

        if (query.length < 3) {
            _searchResults.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            delay(800)
            _isLoading.value = true

            val result = repository.searchHybrid(query)

            if (result.isSuccess) {
                val books = result.getOrDefault(emptyList())
                Log.d("SEARCH_DEBUG", "¡Éxito! Libros encontrados: ${books.size}")

                books.forEach { book ->
                    Log.d("SEARCH_DEBUG", "Libro: ${book.title} -> Provider: ${book.provider}")
                }

                _searchResults.value = books
            } else {
                val error = result.exceptionOrNull()
                Log.e("SEARCH_DEBUG", "¡CRASH EN LA BÚSQUEDA!: ${error?.message}", error)
                _searchResults.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    fun clearResults() {
        _searchResults.value = emptyList()
    }
}