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
    val foundBook: Book? = null,
    val uiLog: String = "Esperando detección...\n"
)

class ScannerViewModel(private val repository: BooksRepository = BooksRepository()) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun onIsbnDetected(isbn: String) {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.notFoundIsbn != null || currentState.foundBook != null) return

        viewModelScope.launch {
            logToUi("CÁMARA: Detectado código $isbn")
            _uiState.update { it.copy(isLoading = true) }
            logToUi("API: Buscando libro...")

            val result = repository.getBookByIsbn(isbn)
            val book = result.getOrNull()

            if (book != null) {
                logToUi("ÉXITO: Libro '${book.title}' cargado.")

                // --- MAGIA: Guardamos el libro en caché para la pantalla de Detalles ---
                BooksRepository.lastScannedBook = book

                _uiState.update {
                    it.copy(isLoading = false, foundBook = book)
                }
            } else {
                logToUi("ERROR: Sin datos en Google ni OpenLibrary para $isbn")
                _uiState.update {
                    it.copy(isLoading = false, notFoundIsbn = isbn)
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(notFoundIsbn = null) }
        logToUi("Alerta cerrada. Escáner listo.")
    }

    fun dismissBookInfo() {
        _uiState.update { it.copy(foundBook = null) }
        logToUi("Info cerrada. Escáner listo.")
    }

    private fun logToUi(message: String) {
        _uiState.update { currentState ->
            val lines = currentState.uiLog.split("\n").toMutableList()
            lines.add(message)
            if (lines.size > 5) lines.removeAt(0)
            currentState.copy(uiLog = lines.joinToString("\n"))
        }
    }
}