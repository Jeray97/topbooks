package com.example.topbooks.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.repository.*
import com.example.topbooks.ui.profile.SimpleBook
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProgressState(
    val journals: List<SimpleBook> = emptyList(),
    val favorites: List<SimpleBook> = emptyList(),
    val pending: List<SimpleBook> = emptyList(),
    val read: List<SimpleBook> = emptyList(),
    val isLoading: Boolean = true
)

class ProgressViewModel(
    private val progressRepo: ProgressRepository = ProgressRepositoryImpl(),
    private val userRepo: UserRepository = UserRepositoryImpl(),
    private val booksRepo: BooksRepository = BooksRepository(),
    // 🔥 1. AÑADIMOS EL REPOSITORIO DE DIARIOS
    private val journalRepo: JournalRepository = JournalRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressState())
    val uiState: StateFlow<ProgressState> = _uiState.asStateFlow()

    init {
        loadProgressData()
    }

    fun loadProgressData() {
        val uid = userRepo.getCurrentUserId() ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val readDeferred = async { progressRepo.getReadBooks(uid).getOrDefault(emptyList()) }
            val bookmarksDeferred = async { progressRepo.getBookmarks(uid).getOrDefault(emptyList()) }
            val favCoversDeferred = async { userRepo.getFavoriteCovers(uid, 50).getOrDefault(emptyList()) }
            val favIdsDeferred = async { userRepo.getFavoriteIds(uid).getOrDefault(emptyList()) }

            // 🔥 2. PEDIMOS LOS DIARIOS DE FORMA ASÍNCRONA
            val journalsDeferred = async { journalRepo.getAllJournals(uid).getOrDefault(emptyList()) }

            val readBooks = readDeferred.await()
            val pendingBooks = bookmarksDeferred.await().map { SimpleBook(it.bookId, it.bookTitle, "") }

            val favIds = favIdsDeferred.await()
            val favCovers = favCoversDeferred.await()
            val favoriteBooks = favIds.zip(favCovers).map { SimpleBook(it.first, imageUrl = it.second) }

            // 3. RECIBIMOS LOS DIARIOS Y LOS CONVERTIMOS A SimpleBook
            val myJournals = journalsDeferred.await().map {
                SimpleBook(id = it.bookId, title = it.bookTitle, imageUrl = it.bookImageUrl)
            }

            // Enriquecemos portadas (Añadimos myJournals a la lista de enriquecimiento)
            val enrichedPending = enrichWithGlobalBooks(pendingBooks)
            val enrichedRead = enrichWithGlobalBooks(readBooks)
            val enrichedJournals = enrichWithGlobalBooks(myJournals) // 🔥 Buscamos el título/portada en la API

            _uiState.update {
                it.copy(
                    journals = enrichedJournals, // 🔥 4. LO AÑADIMOS AL ESTADO
                    read = enrichedRead,
                    pending = enrichedPending,
                    favorites = favoriteBooks,
                    isLoading = false
                )
            }
        }
    }

    private suspend fun enrichWithGlobalBooks(list: List<SimpleBook>): List<SimpleBook> {
        return list.map { book ->
            viewModelScope.async {
                //Evitamos hacer peticiones a la API para los diarios creados manualmente
                if (book.id.length > 20) return@async book

                val apiBook = booksRepo.getBookDetail(book.id).getOrNull()
                book.copy(
                    title = apiBook?.title ?: book.title,
                    imageUrl = apiBook?.imageUrl ?: book.imageUrl
                )
            }.await()
        }
    }
}