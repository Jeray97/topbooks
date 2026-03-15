package com.example.topbooks.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.repository.*
import com.example.topbooks.utils.Resource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel encargado de la lógica de negocio para las sugerencias de lectura (Escaparate).
 * * Gestiona tres fuentes de datos distintas: libros populares, gustos personales y
 * recomendaciones basadas en la red social del usuario.
 */
class RecommendedViewModel(
    private val repository: BooksRepository = BooksRepository(),
    private val communityRepo: CommunityRepository = CommunityRepositoryImpl(),
    private val userRepo: UserRepository = UserRepositoryImpl()
) : ViewModel() {

    // --- ESTADOS DE LA UI (STATEFLOW) ---
    private val _popularBooks = MutableStateFlow<Resource<List<Book>>>(Resource.Loading)
    val popularBooks: StateFlow<Resource<List<Book>>> = _popularBooks.asStateFlow()

    private val _tastesBooks = MutableStateFlow<Resource<List<Book>>>(Resource.Loading)
    val tastesBooks: StateFlow<Resource<List<Book>>> = _tastesBooks.asStateFlow()

    private val _friendsBooks = MutableStateFlow<Resource<List<Book>>>(Resource.Loading)
    val friendsBooks: StateFlow<Resource<List<Book>>> = _friendsBooks.asStateFlow()

    // Flag para evitar recargas innecesarias al rotar la pantalla o navegar
    private var isDataLoaded = false

    fun loadData(popularQuery: String, fallbackTastesQuery: String) {
        if (isDataLoaded) return
        isDataLoaded = true

        fetchPopularBooks(popularQuery)
        fetchPersonalizedTastesBooks(fallbackTastesQuery)
        fetchFriendsFavorites()
    }

    /** Obtiene los libros más relevantes usando la Búsqueda Híbrida (Firebase + Google + OL) */
    private fun fetchPopularBooks(query: String) {
        viewModelScope.launch {
            _popularBooks.value = Resource.Loading

            // Usamos searchHybrid para garantizar la máxima calidad y variedad en el escaparate
            val result = repository.searchHybrid(query)

            if (result.isSuccess) {
                _popularBooks.value = Resource.Success(result.getOrDefault(emptyList()))
            } else {
                _popularBooks.value = Resource.Error(result.exceptionOrNull() ?: Exception("Error loading popular books"))
            }
        }
    }

    private fun fetchPersonalizedTastesBooks(fallbackQuery: String) {
        viewModelScope.launch {
            _tastesBooks.value = Resource.Loading

            try {
                val uid = userRepo.getCurrentUserId()
                if (uid == null) {
                    _tastesBooks.value = Resource.Success(emptyList())
                    return@launch
                }

                val genres = userRepo.getFavoriteGenres(uid)
                val favoriteBookIds = userRepo.getFavoriteIds(uid).getOrDefault(emptyList())
                var books: List<Book> = emptyList()

                // Intentamos buscar por géneros específicos
                if (genres.isNotEmpty()) {
                    books = repository.getBooksByGenres(genres)
                }

                // Fallback Híbrido: Si no hay resultados, usamos la búsqueda combinada
                if (books.isEmpty()) {
                    books = repository.searchHybrid(fallbackQuery).getOrDefault(emptyList())
                }

                val finalBooks = books
                    .filter { it.id !in favoriteBookIds }
                    .distinctBy { it.id }
                    .shuffled()
                    .take(20)

                _tastesBooks.value = Resource.Success(finalBooks)

            } catch (e: Exception) {
                _tastesBooks.value = Resource.Error(e)
            }
        }
    }

    private fun fetchFriendsFavorites() {
        viewModelScope.launch {
            _friendsBooks.value = Resource.Loading
            try {
                val friendIds = communityRepo.getMyFriendsIds().getOrDefault(emptySet())

                if (friendIds.isEmpty()) {
                    _friendsBooks.value = Resource.Success(emptyList())
                    return@launch
                }

                val allBookIds = coroutineScope {
                    friendIds.map { friendId ->
                        async { userRepo.getFavoriteIds(friendId).getOrDefault(emptyList()).take(5) }
                    }.awaitAll().flatten().distinct().take(15)
                }

                val booksDetails = coroutineScope {
                    allBookIds.map { bookId ->
                        async { repository.getBookDetail(bookId).getOrNull() }
                    }.awaitAll().filterNotNull()
                }

                val sortedBooks = booksDetails.sortedByDescending { book ->
                    Regex("\\d{4}").find(book.lanzamiento)?.value?.toIntOrNull() ?: 0
                }

                _friendsBooks.value = Resource.Success(sortedBooks)

            } catch (e: Exception) {
                _friendsBooks.value = Resource.Error(e)
            }
        }
    }
}