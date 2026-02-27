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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 🟢 1. Pequeño repositorio local para aislar Firebase
interface TutorialUpdater {
    fun completeTutorial(genres: List<String>, books: List<String>, onComplete: (Boolean) -> Unit)
}
class TutorialUpdaterImpl : TutorialUpdater {
    override fun completeTutorial(genres: List<String>, books: List<String>, onComplete: (Boolean) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return onComplete(false)
        val updates = mapOf(
            "isTutorialCompleted" to true,
            "favoriteGenres" to genres,
            "favoriteBooks" to books
        )
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .set(updates, SetOptions.merge())
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}

// 🟢 2. El Estado
data class OnboardingState(
    val selectedGenres: Set<String> = emptySet(),
    val suggestedBooks: List<Book> = emptyList(),
    val selectedBookIds: Set<String> = emptySet(),
    val isLoadingBooks: Boolean = false,
    val isSaving: Boolean = false
)

// 🟢 3. El ViewModel limpio (sin Application)
class TutorialViewModel(
    private val booksRepository: BooksRepository = BooksRepository(),
    private val tutorialUpdater: TutorialUpdater = TutorialUpdaterImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingState())
    val uiState: StateFlow<OnboardingState> = _uiState

    // 🟢 Extraemos los strings fijos de Android y los ponemos puros en Kotlin
    val availableGenres = listOf(
        "Historia", "Fantasía", "Ciencia Ficción", "Romance",
        "Misterio", "Manga", "Infantil", "Filosofía",
        "Poesía", "Novela Gráfica", "Aventura", "Terror",
        "Biografía", "Desarrollo Personal", "Ensayo", "Humor"
    )

    fun toggleGenre(genre: String) {
        val current = _uiState.value.selectedGenres
        val newSelection = if (current.contains(genre)) current - genre else current + genre
        _uiState.update { it.copy(selectedGenres = newSelection) }
        fetchSuggestionsForGenres(newSelection)
    }

    fun toggleBookSelection(bookId: String) {
        val current = _uiState.value.selectedBookIds
        val newSelection = if (current.contains(bookId)) current - bookId else current + bookId
        _uiState.update { it.copy(selectedBookIds = newSelection) }
    }

    private fun fetchSuggestionsForGenres(genres: Set<String>) {
        if (genres.isEmpty()) {
            _uiState.update { it.copy(suggestedBooks = emptyList()) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingBooks = true) }

            val allBooks = coroutineScope {
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

    fun finishOnboarding(onSuccess: () -> Unit) {
        _uiState.update { it.copy(isSaving = true) }

        val state = _uiState.value
        tutorialUpdater.completeTutorial(state.selectedGenres.toList(), state.selectedBookIds.toList()) { success ->
            _uiState.update { it.copy(isSaving = false) }
            if (success) onSuccess()
        }
    }
}