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

// 1. ESTADO DE LA UI DEL ESCÁNER
data class ScannerUiState(
    val isLoading: Boolean = false,
    val notFoundIsbn: String? = null,
    val foundBook: Book? = null,
    val uiLog: String = "Esperando detección...\n"
)

class ScannerViewModel(private val repository: BooksRepository = BooksRepository()) : ViewModel() {

    // 2. CONFIGURAMOS STATEFLOW
    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun onIsbnDetected(isbn: String) {
        val currentState = _uiState.value
        // Bloqueamos si ya estamos ocupados o si ya tenemos un libro
        if (currentState.isLoading || currentState.notFoundIsbn != null || currentState.foundBook != null) return

        viewModelScope.launch {
            logToUi("CÁMARA: Detectado código $isbn")
            _uiState.update { it.copy(isLoading = true) }
            logToUi("API: Buscando...")

            var result = repository.getBooks("isbn:$isbn")
            var books = result.getOrDefault(emptyList())

            if (books.isEmpty()) {
                logToUi("API: Reintentando búsqueda genérica...")
                result = repository.getBooks(isbn)
                books = result.getOrDefault(emptyList())
            }

            if (books.isNotEmpty()) {
                val bestMatch = books.find { it.description.length > 50 } ?: books.first()
                logToUi("ÉXITO: Libro '${bestMatch.title}' cargado.")

                _uiState.update {
                    it.copy(isLoading = false, foundBook = bestMatch)
                }
            } else {
                logToUi("ERROR: Sin datos para $isbn")
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