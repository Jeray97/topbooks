package com.example.topbooks.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.repository.BooksRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScannerUiState(
    val isLoading: Boolean = false,
    val notFoundIsbn: String? = null,
    val foundBook: Book? = null
)

class ScannerViewModel(private val repository: BooksRepository = BooksRepository()) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun onIsbnDetected(isbn: String) {
        val currentState = _uiState.value
        // Bloqueo para evitar múltiples llamadas si ya está cargando o ya encontró algo
        if (currentState.isLoading || currentState.notFoundIsbn != null || currentState.foundBook != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = repository.getBookByIsbn(isbn)
            val book = result.getOrNull()

            if (book != null) {
                // --- Guardamos el libro en caché para la pantalla de Detalles ---
                BooksRepository.lastScannedBook = book

                _uiState.update {
                    it.copy(isLoading = false, foundBook = book)
                }
            } else {
                _uiState.update {
                    it.copy(isLoading = false, notFoundIsbn = isbn)
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(notFoundIsbn = null) }
    }

    fun dismissBookInfo() {
        _uiState.update { it.copy(foundBook = null) }
    }
}