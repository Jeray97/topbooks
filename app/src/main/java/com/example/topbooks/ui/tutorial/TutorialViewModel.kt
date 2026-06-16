package com.example.topbooks.ui.tutorial

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.repository.BooksRepository
import com.example.topbooks.data.repository.ShelfRepository
import com.example.topbooks.data.repository.ShelfRepositoryImpl
import com.example.topbooks.data.repository.UserRepository
import com.example.topbooks.data.repository.UserRepositoryImpl
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Representa el estado reactivo del flujo de Onboarding.
 * @property selectedGenres Conjunto de códigos de géneros elegidos por el usuario.
 * @property suggestedBooks Lista de libros obtenidos según las preferencias de género.
 * @property selectedBookIds Conjunto de IDs de libros que el usuario ha marcado como favoritos.
 * @property isLoadingBooks Indica si se están descargando sugerencias de la API.
 * @property isSaving Indica si se está guardando la configuración final en Firebase.
 */
data class OnboardingState(
    val selectedGenres: Set<String> = emptySet(),
    val suggestedBooks: List<Book> = emptyList(),
    val selectedBookIds: Set<String> = emptySet(),
    val isLoadingBooks: Boolean = false,
    val isSaving: Boolean = false
)

/**
 * ViewModel que gestiona la lógica del asistente de configuración inicial (Tutorial).
 * Procesa la selección de intereses y libros iniciales para personalizar el feed del usuario.
 */
class TutorialViewModel(
    private val booksRepository: BooksRepository = BooksRepository(),
    private val userRepository: UserRepository = UserRepositoryImpl(),
    private val shelfRepository: ShelfRepository = ShelfRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingState())
    val uiState: StateFlow<OnboardingState> = _uiState.asStateFlow()

    /**
     * Alterna la selección de un género.
     * Al añadir un género nuevo, dispara automáticamente la búsqueda de libros relacionados.
     */
    fun toggleGenre(genre: String) {
        val current = _uiState.value.selectedGenres
        val newSelection = if (current.contains(genre)) current - genre else current + genre
        _uiState.update { it.copy(selectedGenres = newSelection) }
        fetchSuggestionsForGenres(newSelection)
    }

    /**
     * Alterna la selección de un libro sugerido en la última página del tutorial.
     */
    fun toggleBookSelection(bookId: String) {
        val current = _uiState.value.selectedBookIds
        val newSelection = if (current.contains(bookId)) current - bookId else current + bookId
        _uiState.update { it.copy(selectedBookIds = newSelection) }
    }

    /**
     * Obtiene sugerencias de libros basadas en los géneros seleccionados.
     * Utiliza ejecución paralela mediante async/awaitAll para consultar múltiples
     * categorías simultáneamente en la API de Google Books.
     */
    private fun fetchSuggestionsForGenres(genres: Set<String>) {
        if (genres.isEmpty()) {
            _uiState.update { it.copy(suggestedBooks = emptyList()) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingBooks = true) }

            val allBooks = coroutineScope {
                // Mapeamos cada género a una tarea asíncrona de red
                genres.map { genre ->
                    async {
                        booksRepository.getBooks("subject:$genre", "relevance", true)
                            .getOrNull()?.take(3) ?: emptyList()
                    }
                }.awaitAll().flatten().distinctBy { it.id }
            }

            _uiState.update { it.copy(suggestedBooks = allBooks, isLoadingBooks = false) }
        }
    }

    /**
     * Finaliza el proceso de Onboarding.
     * Delega en UserRepository la persistencia de las preferencias en Firestore.
     */
    fun finishOnboarding(onSuccess: () -> Unit) {
        val state = _uiState.value
        val uid = userRepository.getCurrentUserId()

        if (uid == null) {
            Log.e("TutorialVM", "Error: Usuario no autenticado al finalizar tutorial")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            userRepository.completeTutorial(
                userId = uid,
                genres = state.selectedGenres.toList(),
                books = state.selectedBookIds.toList()
            ).onSuccess {
                createDefaultShelves()
                _uiState.update { it.copy(isSaving = false) }
                onSuccess()
            }.onFailure { error ->
                Log.e("TutorialVM", "Fallo al guardar tutorial: ${error.message}")
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    private suspend fun createDefaultShelves() {
        val defaultShelves = listOf(
            Triple("Leídos", 0xFF8D5B4CL, 0),
            Triple("Quiero leer", 0xFF6B8E23L, 1),
            Triple("Favoritos", 0xFFCD853FL, 2)
        )

        defaultShelves.forEach { (name, color, order) ->
            shelfRepository.createShelf(name, color).onFailure { error ->
                Log.e("TutorialVM", "Error creando estantería '$name': ${error.message}")
            }
        }

        Log.d("TutorialVM", "Estanterías predefinidas creadas correctamente")
    }
}