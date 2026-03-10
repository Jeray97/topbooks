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

/**
 * Representa el estado visual y logístico de la pantalla de escaneo.
 * * @property isLoading Indica si se está realizando una consulta a la API de libros.
 * @property notFoundIsbn Almacena el código ISBN que no pudo ser localizado para mostrar un error.
 * @property foundBook El objeto libro recuperado tras un escaneo exitoso.
 * @property uiLog Registro de texto que muestra los pasos técnicos actuales en la interfaz.
 */
data class ScannerUiState(
    val isLoading: Boolean = false,
    val notFoundIsbn: String? = null,
    val foundBook: Book? = null,
    val uiLog: String = "Esperando detección...\n"
)

/**
 * ViewModel encargado de procesar los códigos de barras detectados por la cámara.
 * * Actúa como intermediario entre ML Kit (que provee el string del código) y el
 * repositorio de libros (que provee la información bibliográfica).
 */
class ScannerViewModel(private val repository: BooksRepository = BooksRepository()) : ViewModel() {

    // Gestión de estado mediante StateFlow para una UI reactiva
    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    /**
     * Procesa un código ISBN detectado por el escáner.
     * * Implementa una lógica de búsqueda en cascada:
     * 1. Búsqueda estricta mediante el calificador 'isbn:'.
     * 2. Búsqueda de respaldo mediante texto libre si la primera falla.
     * * @param isbn Cadena de texto que representa el código de barras detectado.
     */
    fun onIsbnDetected(isbn: String) {
        val currentState = _uiState.value
        // Prevención de peticiones redundantes: bloqueamos si ya hay un proceso activo o un resultado visible
        if (currentState.isLoading || currentState.notFoundIsbn != null || currentState.foundBook != null) return

        viewModelScope.launch {
            logToUi("CÁMARA: Detectado código $isbn")
            _uiState.update { it.copy(isLoading = true) }
            logToUi("API: Buscando...")

            // Paso 1: Búsqueda específica por ISBN
            var result = repository.getBooks("isbn:$isbn")
            var books = result.getOrDefault(emptyList())

            // Paso 2: Reintento genérico si la búsqueda estricta no dio frutos
            if (books.isEmpty()) {
                logToUi("API: Reintentando búsqueda genérica...")
                result = repository.getBooks(isbn)
                books = result.getOrDefault(emptyList())
            }

            if (books.isNotEmpty()) {
                // Heurística: Seleccionamos el libro que probablemente tenga más metadatos (descripción más larga)
                val bestMatch = books.find { it.description.length > 50 } ?: books.first()
                logToUi("ÉXITO: Libro '${bestMatch.title}' cargado.")

                _uiState.update {
                    it.copy(isLoading = false, foundBook = bestMatch)
                }
            } else {
                // Manejo de caso: Código válido pero sin información en la base de datos de Google
                logToUi("ERROR: Sin datos para $isbn")
                _uiState.update {
                    it.copy(isLoading = false, notFoundIsbn = isbn)
                }
            }
        }
    }

    /** Restablece el estado de error de ISBN no encontrado. */
    fun dismissError() {
        _uiState.update { it.copy(notFoundIsbn = null) }
        logToUi("Alerta cerrada. Escáner listo.")
    }

    /** Restablece el estado de éxito para permitir un nuevo escaneo. */
    fun dismissBookInfo() {
        _uiState.update { it.copy(foundBook = null) }
        logToUi("Info cerrada. Escáner listo.")
    }

    /**
     * Añade una línea al registro visual de la consola del escáner.
     * Mantiene un límite de 5 líneas para evitar saturar la pantalla.
     */
    private fun logToUi(message: String) {
        _uiState.update { currentState ->
            val lines = currentState.uiLog.split("\n").toMutableList()
            lines.add(message)
            // Eliminamos la línea más antigua si superamos el máximo de 5
            if (lines.size > 5) lines.removeAt(0)
            currentState.copy(uiLog = lines.joinToString("\n"))
        }
    }
}