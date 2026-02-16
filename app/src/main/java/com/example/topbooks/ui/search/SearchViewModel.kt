package com.example.topbooks.ui.search

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
            // Aumentamos un poco el delay (800ms) para no saturar las dos APIs mientras escribes
            delay(800)
            _isLoading.value = true

            // CORRECCIÓN: Ahora usamos searchHybrid en lugar de getBooks
            // Esto buscará en Google y OpenLibrary en paralelo y combinará los resultados
            val result = repository.searchHybrid(query)

            if (result.isSuccess) {
                _searchResults.value = result.getOrDefault(emptyList())
            } else {
                _searchResults.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    fun clearResults() {
        _searchResults.value = emptyList()
    }
}