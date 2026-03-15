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
 * ViewModel encargado de gestionar el contenido detallado de las secciones de recomendaciones.
 * * LÓGICA AVANZADA: Implementa un sistema de Paginación Dinámica (Scroll Infinito)
 * combinando Búsqueda Híbrida para el primer impacto y paginación en caché local para la red social.
 */
class RecommendedSectionViewModel(
    private val repository: BooksRepository = BooksRepository(),
    private val communityRepo: CommunityRepository = CommunityRepositoryImpl(),
    private val userRepo: UserRepository = UserRepositoryImpl()
) : ViewModel() {

    private val _booksState = MutableStateFlow<Resource<List<Book>>>(Resource.Loading)
    val booksState: StateFlow<Resource<List<Book>>> = _booksState.asStateFlow()

    // --- VARIABLES DE CONTROL DE PAGINACIÓN ---
    private var currentPage = 1
    private var currentType = ""
    private var currentGenre = ""
    private var isLastPage = false
    private var isLoadingMore = false

    private val loadedBooks = mutableListOf<Book>()

    // Caché local de IDs para poder paginar los libros de los amigos sin saturar la red
    private var socialBookIdsList = listOf<String>()

    fun loadSectionData(type: String, genre: String) {
        if (currentType != type || currentGenre != genre) {
            currentType = type
            currentGenre = genre
            currentPage = 1
            loadedBooks.clear()
            socialBookIdsList = emptyList()
            isLastPage = false
            _booksState.value = Resource.Loading
            fetchData()
        }
    }

    fun loadMore() {
        if (isLoadingMore || isLastPage) return
        currentPage++
        fetchData()
    }

    private fun fetchData() {
        isLoadingMore = true
        viewModelScope.launch {
            when (currentType) {
                "popular" -> fetchFromApi(currentGenre, "relevance", true)
                "tastes" -> fetchFromApi(currentGenre, "relevance", true)
                "friends" -> fetchFriendsFavorites()
            }
        }
    }

    /**
     * Realiza peticiones paginadas a las APIs.
     * * Página 1: Usa la Búsqueda Híbrida (Firebase+Google+OL) para la mejor calidad inicial.
     * * Página > 1: Usa la paginación estándar de Google Books para seguir bajando en el catálogo.
     */
    private suspend fun fetchFromApi(query: String, orderBy: String, filterModern: Boolean) {
        try {
            val newBooks = if (currentPage == 1) {
                repository.searchHybrid(query).getOrDefault(emptyList())
            } else {
                repository.getBooks(query, orderBy, filterModern, currentPage, 20).getOrDefault(emptyList())
            }

            if (newBooks.isEmpty()) {
                isLastPage = true
            } else {
                // ESCUDO ANTI-DUPLICADOS: Filtramos libros que ya teníamos en páginas anteriores
                val uniqueNewBooks = newBooks.filter { newBook -> loadedBooks.none { it.id == newBook.id } }

                // Si todos los libros de esta página ya los teníamos, forzamos la siguiente automáticamente
                if (uniqueNewBooks.isEmpty() && newBooks.isNotEmpty()) {
                    if (currentPage < 5) { // Límite de seguridad para no crear un bucle infinito
                        currentPage++
                        fetchFromApi(query, orderBy, filterModern)
                    } else {
                        isLastPage = true
                        isLoadingMore = false
                    }
                    return
                }

                loadedBooks.addAll(uniqueNewBooks)
                _booksState.value = Resource.Success(loadedBooks.toList())
            }
        } catch (e: Exception) {
            if (currentPage == 1) _booksState.value = Resource.Error(e)
        } finally {
            isLoadingMore = false
        }
    }

    /**
     * Obtiene y pagina los libros favoritos de todos los amigos del usuario.
     * * TÉCNICA DE ALTO RENDIMIENTO (Chunking local):
     * En la primera página descarga todos los IDs. Luego, al hacer scroll, va pidiendo a la API
     * los detalles completos en "paquetes" (chunks) de 20 en 20.
     */
    private suspend fun fetchFriendsFavorites() {
        try {
            // FASE 1: Recolección masiva de IDs (Solo ocurre en la página 1)
            if (currentPage == 1) {
                val friendIds = communityRepo.getMyFriendsIds().getOrDefault(emptySet())
                if (friendIds.isEmpty()) {
                    _booksState.value = Resource.Success(emptyList())
                    isLastPage = true
                    isLoadingMore = false
                    return
                }

                val allBookIds = coroutineScope {
                    friendIds.map { friendId ->
                        async { userRepo.getFavoriteIds(friendId).getOrDefault(emptyList()) }
                    }.awaitAll().flatten().distinct()
                }

                // Mezclamos aleatoriamente para que cada vez que entres descubras libros distintos
                socialBookIdsList = allBookIds.shuffled()
            }

            // FASE 2: Paginación de IDs locales (Calculamos qué trozo de la lista toca cargar)
            val pageSize = 20
            val startIndex = (currentPage - 1) * pageSize
            val endIndex = minOf(startIndex + pageSize, socialBookIdsList.size)

            if (startIndex >= socialBookIdsList.size) {
                isLastPage = true
                isLoadingMore = false
                return
            }

            val pageIds = socialBookIdsList.subList(startIndex, endIndex)

            // FASE 3: Descargar detalles ricos solo del "chunk" actual
            val booksDetails = coroutineScope {
                pageIds.map { bookId ->
                    async { repository.getBookDetail(bookId).getOrNull() }
                }.awaitAll().filterNotNull()
            }

            loadedBooks.addAll(booksDetails)
            _booksState.value = Resource.Success(loadedBooks.toList())

            // Comprobamos si hemos llegado al final de nuestra caché de IDs
            if (endIndex == socialBookIdsList.size) {
                isLastPage = true
            }

        } catch (e: Exception) {
            if (currentPage == 1) _booksState.value = Resource.Error(e)
        } finally {
            isLoadingMore = false
        }
    }
}