package com.example.topbooks.ui.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.repository.BooksRepository
import com.example.topbooks.ui.community.SearchFilter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel encargado de gestionar la lógica de búsqueda de libros.
 * Provee un flujo de resultados reactivo y maneja la optimización de peticiones a la red.
 */
class SearchViewModel(private val repository: BooksRepository = BooksRepository()) : ViewModel() {

    // Estado interno para los resultados de búsqueda
    private val _searchResults = MutableStateFlow<List<Book>>(emptyList())
    /** Flujo público de resultados de búsqueda para ser observado por la UI. */
    val searchResults: StateFlow<List<Book>> = _searchResults.asStateFlow()

    // Estado interno para el indicador de carga
    private val _isLoading = MutableStateFlow(false)
    /** Flujo público que indica si hay una búsqueda en curso. */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Estado interno para el filtro de búsqueda activo
    private val _searchFilter = MutableStateFlow(SearchFilter.GENERAL)
    /** Flujo público del filtro de búsqueda activo. */
    val searchFilter: StateFlow<SearchFilter> = _searchFilter.asStateFlow()

    // Referencia al trabajo de búsqueda actual para permitir su cancelación
    private var searchJob: Job? = null

    /**
     * Actualiza el filtro de búsqueda activo y re-ejecuta la búsqueda si hay texto.
     */
    fun setSearchFilter(filter: SearchFilter) {
        _searchFilter.value = filter
    }

    /**
     * Procesa el cambio en la consulta de búsqueda.
     * Implementa un mecanismo de Debounce para optimizar las llamadas al repositorio.
     *
     * @param query El texto ingresado por el usuario en la barra de búsqueda.
     */
    fun onQueryChange(query: String) {
        // Cancelamos cualquier búsqueda iniciada previamente para evitar colisiones de datos
        searchJob?.cancel()

        // Evitamos búsquedas con términos demasiado cortos para optimizar el rendimiento
        if (query.length < 3) {
            _searchResults.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            // Debounce: Esperamos 800ms antes de ejecutar la petición
            delay(800)
            _isLoading.value = true

            // Aplicar filtro de búsqueda según el tipo seleccionado
            val filteredQuery = when (_searchFilter.value) {
                SearchFilter.GENERAL -> query
                SearchFilter.TITLE -> "intitle:$query"
                SearchFilter.AUTHOR -> "inauthor:$query"
                SearchFilter.ISBN -> "isbn:$query"
                SearchFilter.SERIES -> "subject:$query"
            }

            // Ejecución de búsqueda híbrida (combina fuentes de datos)
            val result = repository.searchHybrid(filteredQuery)

            if (result.isSuccess) {
                val books = result.getOrDefault(emptyList())
                Log.d("SEARCH_DEBUG", "Exito! Libros encontrados: ${books.size}")

                // Log para depuración de proveedores de datos
                books.forEach { book ->
                    Log.d("SEARCH_DEBUG", "Libro: ${book.title} -> Provider: ${book.provider}")
                }

                _searchResults.value = books
            } else {
                val error = result.exceptionOrNull()
                Log.e("SEARCH_DEBUG", "Error critico en la busqueda: ${error?.message}", error)
                _searchResults.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    /**
     * Limpia los resultados de búsqueda actuales.
     */
    fun clearResults() {
        _searchResults.value = emptyList()
    }
}