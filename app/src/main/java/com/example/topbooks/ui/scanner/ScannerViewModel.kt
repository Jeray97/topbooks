package com.example.topbooks.ui.scanner

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.repository.BooksRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class ScannerViewModel(private val repository: BooksRepository = BooksRepository()) : ViewModel() {

    // --- ESTADOS DE LA UI ---

    // Si estamos cargando (consultando la API)
    var isLoading = mutableStateOf(false)
        private set

    // Si hubo un error (libro no encontrado), guardamos el ISBN para mostrarlo en el mensaje
    var notFoundIsbn = mutableStateOf<String?>(null)
        private set

    // Evento de navegación (Solo ocurre una vez cuando encontramos el libro)
    private val _navigationEvent = MutableSharedFlow<String>() // Emitiremos el ID del libro
    val navigationEvent = _navigationEvent.asSharedFlow()

    // --- LÓGICA ---

    fun onIsbnDetected(isbn: String) {
        // Evitamos llamadas múltiples si ya estamos cargando o mostrando un error
        if (isLoading.value || notFoundIsbn.value != null) return

        viewModelScope.launch {
            isLoading.value = true

            //Para buscar por ISBN en Google Books, se usa "isbn:NUMERO"
            val query = "isbn:$isbn"
            val result = repository.getBooks(query)

            isLoading.value = false

            if (result.isSuccess) {
                val books = result.getOrDefault(emptyList())
                if (books.isNotEmpty()) {
                    // Encontramos el libro, emitimos su ID para navegar
                    _navigationEvent.emit(books.first().id)
                } else {
                    // La lista está vacía, mostramos la ventanita
                    notFoundIsbn.value = isbn
                }
            } else {
                // FALLO DE RED: Lo tratamos como no encontrado por ahora
                notFoundIsbn.value = isbn
            }
        }
    }

    // Cuando el usuario cierra la ventanita de error
    fun dismissError() {
        notFoundIsbn.value = null
    }
}