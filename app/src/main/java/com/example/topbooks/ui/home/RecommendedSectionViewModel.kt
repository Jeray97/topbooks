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
 * * LÓGICA AVANZADA: Implementa un sistema de paginación automática (Scroll Infinito)
 * y agregación social de datos desde Firebase.
 *
 * @property repository Repositorio principal de libros (API y Firestore).
 * @property communityRepo Gestión de relaciones sociales (amigos).
 * @property userRepo Gestión de datos privados del usuario (favoritos).
 */
class RecommendedSectionViewModel(
    private val repository: BooksRepository = BooksRepository(),
    private val communityRepo: CommunityRepository = CommunityRepositoryImpl(),
    private val userRepo: UserRepository = UserRepositoryImpl()
) : ViewModel() {

    // --- ESTADO DE LA UI ---
    private val _booksState = MutableStateFlow<Resource<List<Book>>>(Resource.Loading)
    val booksState: StateFlow<Resource<List<Book>>> = _booksState.asStateFlow()

    // --- VARIABLES DE CONTROL DE PAGINACIÓN ---
    private var currentPage = 1
    private var currentType = ""
    private var currentGenre = ""
    private var isLastPage = false
    private var isLoadingMore = false

    // Lista persistente que acumula los libros de todas las páginas cargadas
    private val loadedBooks = mutableListOf<Book>()

    /**
     * Inicializa o reinicia la carga de datos para una sección específica.
     * * Si el tipo o género cambia, resetea todos los contadores de página y limpia la lista previa.
     *
     * @param type Tipo de sección ("popular", "tastes", "friends").
     * @param genre Nombre del género o consulta de búsqueda.
     */
    fun loadSectionData(type: String, genre: String) {
        if (currentType != type || currentGenre != genre) {
            currentType = type
            currentGenre = genre
            currentPage = 1
            loadedBooks.clear()
            isLastPage = false
            _booksState.value = Resource.Loading
            fetchData()
        }
    }

    /**
     * Método disparado por el Scroll Infinito de la UI.
     * Incrementa la página y solicita más datos si no hay una carga en curso ni se ha llegado al final.
     */
    fun loadMore() {
        if (isLoadingMore || isLastPage) return
        currentPage++
        fetchData()
    }

    /**
     * Enrutador interno que decide la fuente de datos basándose en el tipo de sección actual.
     */
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
     * Realiza peticiones paginadas a la API de libros.
     * @param query Término de búsqueda.
     * @param orderBy Criterio de ordenación de la API.
     * @param filterModern Aplica filtros de relevancia y fecha.
     */
    private suspend fun fetchFromApi(query: String, orderBy: String, filterModern: Boolean) {
        val result = repository.getBooks(query, orderBy, filterModern, currentPage, 20)
        if (result.isSuccess) {
            val newBooks = result.getOrDefault(emptyList())

            // Si la API no devuelve más resultados, marcamos el fin del scroll
            if (newBooks.isEmpty()) {
                isLastPage = true
            } else {
                // Añadimos los nuevos libros a la lista maestra y emitimos el nuevo estado
                loadedBooks.addAll(newBooks)
                _booksState.value = Resource.Success(loadedBooks.toList())
            }
        } else if (currentPage == 1) {
            // Solo emitimos error si falla la carga de la primera página
            _booksState.value = Resource.Error(result.exceptionOrNull() ?: Exception("Error"))
        }
        isLoadingMore = false
    }

    /**
     * Obtiene y agrega los libros favoritos de todos los amigos del usuario.
     * * TÉCNICA DE ALTO RENDIMIENTO (Faseada):
     * 1. Descarga en paralelo los IDs de libros favoritos de cada amigo.
     * 2. Une, filtra duplicados y limita a los 30 más relevantes.
     * 3. Descarga en paralelo los detalles completos de cada libro único.
     * 4. Ordena por año de lanzamiento usando expresiones regulares.
     */
    private suspend fun fetchFriendsFavorites() {
        try {
            val friendIds = communityRepo.getMyFriendsIds().getOrDefault(emptySet())
            if (friendIds.isEmpty()) {
                _booksState.value = Resource.Success(emptyList())
                isLoadingMore = false
                return
            }

            // FASE 1: Recolectar IDs de libros favoritos de amigos
            val allBookIds = coroutineScope {
                friendIds.map { friendId ->
                    async { userRepo.getFavoriteIds(friendId).getOrDefault(emptyList()).take(5) }
                }.awaitAll().flatten().distinct().take(30)
            }

            // FASE 2: Descargar detalles de los libros (Portadas, Títulos, etc)
            val booksDetails = coroutineScope {
                allBookIds.map { bookId -> async { repository.getBookDetail(bookId).getOrNull() } }
                    .awaitAll().filterNotNull()
            }

            // FASE 3: Procesamiento y Ordenación
            loadedBooks.clear()
            // TÉCNICA: Usamos Regex para extraer el año numérico de un String de fecha (ej: "2024-05-12" -> 2024)
            loadedBooks.addAll(booksDetails.sortedByDescending {
                Regex("\\d{4}").find(it.lanzamiento)?.value?.toIntOrNull() ?: 0
            })

            _booksState.value = Resource.Success(loadedBooks.toList())
            isLastPage = true // Las recomendaciones sociales se cargan todas de una vez
        } catch (e: Exception) {
            _booksState.value = Resource.Error(e)
        } finally {
            isLoadingMore = false
        }
    }
}