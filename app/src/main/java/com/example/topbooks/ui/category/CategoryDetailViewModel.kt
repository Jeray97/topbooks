package com.example.topbooks.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.repository.BooksRepository
import com.example.topbooks.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel encargado de la lógica de la pantalla de Detalles de Categoría.
 * * Actúa como intermediario entre la vista ([CategoryDetailScreen]) y la capa de datos ([BooksRepository]).
 *
 * @property repository Instancia del repositorio que gestiona las llamadas a las APIs de libros y Firebase.
 */
class CategoryDetailViewModel(
    private val repository: BooksRepository = BooksRepository()
) : ViewModel() {

    // --- ESTADO ---

    /**
     * Flujo de estado interno y mutable que contiene la lista de libros envuelta en la clase [Resource].
     * * Inicializa con [Resource.Loading] para que la pantalla muestre un spinner de carga nada más abrirse.
     */
    private val _categoryBooks = MutableStateFlow<Resource<List<Book>>>(Resource.Loading)

    /**
     * Flujo de estado público e inmutable que observa la interfaz de usuario (UI).
     */
    val booksState: StateFlow<Resource<List<Book>>> = _categoryBooks.asStateFlow()

    // --- FUNCIÓN PRINCIPAL ---

    /**
     * Descarga la lista de libros correspondientes a un género o categoría específica.
     * * Utiliza corrutinas ([viewModelScope]) para no bloquear el hilo principal (la pantalla)
     * mientras espera la respuesta de internet.
     *
     * @param query La cadena de búsqueda exacta formateada para las APIs (ej: "subject:Romance").
     */
    fun fetchBooksByCategory(query: String) {
        viewModelScope.launch {
            // 1. Avisamos a la UI de que empezamos a buscar (muestra el spinner)
            _categoryBooks.value = Resource.Loading

            // 2. Llamamos al repositorio utilizando la búsqueda híbrida
            // Esto buscará en Firebase -> Google Books -> Open Library en paralelo
            val result = repository.searchHybrid(query)

            // 3. Procesamos la respuesta
            if (result.isSuccess) {
                // Éxito: Extraemos la lista de libros y la enviamos a la UI
                val books = result.getOrDefault(emptyList())
                _categoryBooks.value = Resource.Success(books)
            } else {
                // Error: Extraemos el mensaje de fallo (ej. sin internet) y lo enviamos a la UI
                val error = result.exceptionOrNull() ?: Exception("Error desconocido")
                _categoryBooks.value = Resource.Error(error)
            }
        }
    }
}