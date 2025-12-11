package com.example.topbooks.ui.scanner

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.repository.BooksRepository
import kotlinx.coroutines.launch

class ScannerViewModel(private val repository: BooksRepository = BooksRepository()) : ViewModel() {

    // --- ESTADOS DE LA UI ---
    var isLoading = mutableStateOf(false)
        private set

    var notFoundIsbn = mutableStateOf<String?>(null)
        private set

    // 1. VARIABLE QUE GUARDA EL LIBRO ENCONTRADO
    var foundBook = mutableStateOf<Book?>(null)
        private set

    // Variable para la "consola" en pantalla
    var uiLog = mutableStateOf("Esperando detección...\n")
        private set

    // --- LÓGICA ---

    fun onIsbnDetected(isbn: String) {
        // Bloqueamos si ya estamos ocupados O SI YA TENEMOS UN LIBRO EN PANTALLA
        if (isLoading.value || notFoundIsbn.value != null || foundBook.value != null) return

        viewModelScope.launch {
            logToUi("CÁMARA: Detectado código $isbn")

            isLoading.value = true
            logToUi("API: Buscando...")

            // INTENTO 1: Búsqueda estricta
            var result = repository.getBooks("isbn:$isbn")
            var books = result.getOrDefault(emptyList())

            // INTENTO 2: Búsqueda genérica
            if (books.isEmpty()) {
                logToUi("API: Reintentando búsqueda genérica...")
                result = repository.getBooks(isbn)
                books = result.getOrDefault(emptyList())
            }

            isLoading.value = false

            if (books.isNotEmpty()) {
                // Buscamos el mejor resultado (el que tenga descripción o el primero)
                val bestMatch = books.find { it.description.length > 50 } ?: books.first()

                logToUi("ÉXITO: Libro '${bestMatch.title}' cargado.")

                // 2. AQUÍ ES DONDE GUARDAMOS EL LIBRO PARA QUE LA PANTALLA LO MUESTRE
                foundBook.value = bestMatch

            } else {
                logToUi("ERROR: Sin datos para $isbn")
                notFoundIsbn.value = isbn
            }
        }
    }

    fun dismissError() {
        notFoundIsbn.value = null
        logToUi("Alerta cerrada. Escáner listo.")
    }

    // Función para cerrar la tarjeta y seguir escaneando
    fun dismissBookInfo() {
        foundBook.value = null
        logToUi("Info cerrada. Escáner listo.")
    }

    private fun logToUi(message: String) {
        val currentLog = uiLog.value
        val lines = currentLog.split("\n").toMutableList()
        lines.add(message)
        // Guardamos solo las últimas 5 líneas para que no ocupe toda la pantalla
        if (lines.size > 5) {
            lines.removeAt(0)
        }
        uiLog.value = lines.joinToString("\n")
    }
}