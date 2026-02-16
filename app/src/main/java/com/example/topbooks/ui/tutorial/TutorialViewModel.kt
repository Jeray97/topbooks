package com.example.topbooks.ui.tutorial

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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
import com.example.topbooks.R

data class OnboardingState(
    val selectedGenres: Set<String> = emptySet(),
    val suggestedBooks: List<Book> = emptyList(),
    val selectedBookIds: Set<String> = emptySet(),
    val isLoadingBooks: Boolean = false,
    val isSaving: Boolean = false
)

class TutorialViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val booksRepository = BooksRepository()
    private val _uiState = MutableStateFlow(OnboardingState())
    val uiState: StateFlow<OnboardingState> = _uiState

    val availableGenres = listOf(
        getApplication<Application>().getString(R.string.cat_historia_text),
        getApplication<Application>().getString(R.string.cat_bibliografia_text),
        getApplication<Application>().getString(R.string.cat_horror_text),
        getApplication<Application>().getString(R.string.cat_arte_text),
        getApplication<Application>().getString(R.string.cat_romance_text),
        getApplication<Application>().getString(R.string.cat_misterio_text),
        getApplication<Application>().getString(R.string.cat_manga_text),
        getApplication<Application>().getString(R.string.cat_fantasia_text),
        getApplication<Application>().getString(R.string.cat_infantil_text),
        getApplication<Application>().getString(R.string.cat_filosofia_text),
        getApplication<Application>().getString(R.string.cat_poesia_text),
        getApplication<Application>().getString(R.string.cat_novela_grafica_text),
        getApplication<Application>().getString(R.string.cat_aventura_text),
        getApplication<Application>().getString(R.string.cat_ciencia_ficcion_text),
        getApplication<Application>().getString(R.string.cat_religion_text)
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

            // Lógica Paralela: Busca 2 libros por cada género seleccionado
            val deferredBooks = genres.map { genre ->
                async {
                    booksRepository.getBooks(
                        query = "subject:$genre",
                        orderBy = "relevance",
                        filterModern = true // Libros modernos y con portada
                    ).getOrNull()?.take(2) ?: emptyList()
                }
            }

            val allBooks = deferredBooks.awaitAll().flatten().distinctBy { it.id }
            _uiState.update { it.copy(suggestedBooks = allBooks, isLoadingBooks = false) }
        }
    }

    fun finishOnboarding(onSuccess: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        _uiState.update { it.copy(isSaving = true) }

        val updates = mapOf(
            "isTutorialCompleted" to true, // CORREGIDO: Coincide con User.kt
            "favoriteGenres" to _uiState.value.selectedGenres.toList(),
            "favoriteBooks" to _uiState.value.selectedBookIds.toList()
        )

        db.collection("users").document(userId)
            .set(updates, SetOptions.merge())
            .addOnSuccessListener {
                _uiState.update { it.copy(isSaving = false) }
                onSuccess() // Esto dispara la navegación al Home
            }
            .addOnFailureListener {
                _uiState.update { it.copy(isSaving = false) }
            }
    }
}