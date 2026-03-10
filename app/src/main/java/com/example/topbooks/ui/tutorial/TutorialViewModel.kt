package com.example.topbooks.ui.tutorial

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.repository.BooksRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Interfaz para la persistencia de los datos del tutorial.
 * Aísla la lógica de Firebase del ViewModel para facilitar el testing.
 */
interface TutorialUpdater {
    fun completeTutorial(genres: List<String>, books: List<String>, onComplete: (Boolean) -> Unit)
}

/**
 * Implementación de la persistencia del tutorial usando Firebase Firestore.
 */
class TutorialUpdaterImpl : TutorialUpdater {
    override fun completeTutorial(genres: List<String>, books: List<String>, onComplete: (Boolean) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return onComplete(false)
        val updates = mapOf(
            "isTutorialCompleted" to true,
            "favoriteGenres" to genres,
            "favoriteBooks" to books
        )
        // Fusionamos los datos en el documento del usuario sin sobreescribir otros campos
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .set(updates, SetOptions.merge())
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}

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
    private val tutorialUpdater: TutorialUpdater = TutorialUpdaterImpl()
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
     * Guarda las preferencias en Firestore y marca el tutorial como completado en el perfil.
     */
    fun finishOnboarding(onSuccess: () -> Unit) {
        _uiState.update { it.copy(isSaving = true) }

        val state = _uiState.value
        tutorialUpdater.completeTutorial(
            state.selectedGenres.toList(),
            state.selectedBookIds.toList()
        ) { success ->
            _uiState.update { it.copy(isSaving = false) }
            if (success) onSuccess()
        }
    }
}