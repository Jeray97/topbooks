package com.example.topbooks.ui.tutorial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.repository.BooksRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingState(
    val selectedGenres: Set<String> = emptySet(),
    val suggestedBooks: List<Book> = emptyList(),
    val selectedBookIds: Set<String> = emptySet(),
    val isLoadingBooks: Boolean = false,
    val isSaving: Boolean = false
)

class TutorialViewModel(private val booksRepository: BooksRepository = BooksRepository()) : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(OnboardingState())
    val uiState: StateFlow<OnboardingState> = _uiState

    // Los 16 géneros solicitados
    val availableGenres = listOf(
        "Historia", "Biografías", "Horror", "Arte",
        "Romance", "Misterio", "Manga", "Fantasía",
        "Infantil", "Filosofía", "Poesía", "Novela Gráfica",
        "Aventuras", "Ciencia Ficción", "Bibliografía", "Religión"
    )

    fun toggleGenre(genre: String) {
        _uiState.update { state ->
            val newGenres = if (state.selectedGenres.contains(genre)) {
                state.selectedGenres - genre
            } else {
                state.selectedGenres + genre
            }
            state.copy(selectedGenres = newGenres)
        }
        if (_uiState.value.selectedGenres.isNotEmpty()) {
            fetchSuggestedBooks()
        }
    }

    fun toggleBookSelection(bookId: String) {
        _uiState.update { state ->
            val newSelection = if (state.selectedBookIds.contains(bookId)) {
                state.selectedBookIds - bookId
            } else {
                state.selectedBookIds + bookId
            }
            state.copy(selectedBookIds = newSelection)
        }
    }

    private fun fetchSuggestedBooks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingBooks = true) }
            val genres = _uiState.value.selectedGenres.toList()

            // Lógica Paralela: Busca 2 libros por cada género
            val deferredBooks = genres.map { genre ->
                async {
                    booksRepository.getBooks(query = "subject:$genre", orderBy = "relevance")
                        .getOrNull()?.take(2) ?: emptyList()
                }
            }

            // Esperamos todos los resultados y unificamos la lista
            val allBooks = deferredBooks.awaitAll().flatten().distinctBy { it.id }

            _uiState.update { it.copy(suggestedBooks = allBooks, isLoadingBooks = false) }
        }
    }

    fun finishOnboarding(onSuccess: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        _uiState.update { it.copy(isSaving = true) }

        val updates = mapOf(
            "isTutorialCompleted" to true,
            "favoriteGenres" to _uiState.value.selectedGenres.toList(),
            "favoriteBooks" to _uiState.value.selectedBookIds.toList()
        )

        // RUTA ESTÁNDAR: users/{uid} con merge para no borrar datos existentes
        db.collection("users").document(userId)
            .set(updates, SetOptions.merge())
            .addOnSuccessListener {
                _uiState.update { it.copy(isSaving = false) }
                onSuccess()
            }
            .addOnFailureListener {
                _uiState.update { it.copy(isSaving = false) }
            }
    }
}