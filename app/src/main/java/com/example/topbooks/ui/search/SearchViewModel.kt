package com.example.topbooks.ui.components

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

    // Lista de resultados que observará la vista
    private val _searchResults = MutableStateFlow<List<Book>>(emptyList())
    val searchResults: StateFlow<List<Book>> = _searchResults.asStateFlow()

    // Para saber si está cargando
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        // Cancelamos la búsqueda anterior si el usuario sigue escribiendo rápido
        searchJob?.cancel()

        if (query.length < 3) {
            _searchResults.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            // Esperamos 500ms antes de llamar a la API (Debounce)
            delay(500)
            _isLoading.value = true

            val result = repository.getBooks(query)

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