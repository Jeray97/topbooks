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
 * ViewModel encargado de la lógica de negocio para las sugerencias de lectura.
 * * Gestiona tres fuentes de datos distintas: libros populares, gustos personales y
 * recomendaciones basadas en la red social del usuario.
 *
 * @property repository Repositorio central de libros (API y Firestore).
 * @property communityRepo Gestión de relaciones sociales y amigos.
 * @property userRepo Gestión de datos privados del usuario actual.
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

    /**
     * Orquestador principal de carga de datos para la pantalla de Recomendados.
     * @param popularQuery Término de búsqueda para la sección de tendencias.
     * @param fallbackTastesQuery Término de respaldo si el usuario no tiene géneros favoritos definidos.
     */
    fun loadData(popularQuery: String, fallbackTastesQuery: String) {
        if (isDataLoaded) return
        isDataLoaded = true

        fetchPopularBooks(popularQuery)
        fetchPersonalizedTastesBooks(fallbackTastesQuery)
        fetchFriendsFavorites()
    }

    /**
     * Obtiene los libros más relevantes/populares del momento desde la API.
     */
    private fun fetchPopularBooks(query: String) {
        viewModelScope.launch {
            _popularBooks.value = Resource.Loading

            // Aplicamos el filtro 'filterModern' para priorizar libros recientes y conocidos
            val result = repository.getBooks(query, filterModern = true)

            if (result.isSuccess) {
                _popularBooks.value = Resource.Success(result.getOrDefault(emptyList()))
            } else {
                _popularBooks.value =
                    Resource.Error(result.exceptionOrNull() ?: Exception("Error loading popular books"))
            }
        }
    }

    /**
     * Algoritmo de recomendación basado en preferencias:
     * 1. Consulta los géneros favoritos marcados por el usuario.
     * 2. Si hay géneros, busca libros que coincidan con ellos.
     * 3. Si no hay géneros o no hay resultados, usa la consulta de respaldo.
     * 4. Filtra los libros que el usuario ya tiene marcados como favoritos.
     */
    private fun fetchPersonalizedTastesBooks(fallbackQuery: String) {
        viewModelScope.launch {
            _tastesBooks.value = Resource.Loading

            try {
                val uid = userRepo.getCurrentUserId()
                if (uid == null) {
                    _tastesBooks.value = Resource.Success(emptyList())
                    return@launch
                }

                // Cargamos preferencias del usuario
                val genres = userRepo.getFavoriteGenres(uid)
                val favoriteBookIds = userRepo.getFavoriteIds(uid).getOrDefault(emptyList())

                var books: List<Book> = emptyList()

                // Intentamos buscar por géneros específicos
                if (genres.isNotEmpty()) {
                    books = repository.getBooksByGenres(genres)
                }

                // Fallback: Si no hay resultados personalizados, usamos la búsqueda general de respaldo
                if (books.isEmpty()) {
                    books = repository.getBooks(fallbackQuery).getOrDefault(emptyList())
                }

                // POST-PROCESAMIENTO:
                // - Excluimos los libros que ya son favoritos (evita redundancia)
                // - Mezclamos aleatoriamente para dar sensación de frescura (shuffled)
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

    /**
     * Motor social de recomendaciones:
     * Agrega y procesa los libros favoritos de los amigos del usuario.
     * * TÉCNICA: Doble fase de procesamiento paralelo (`async/awaitAll`) para maximizar velocidad.
     */
    private fun fetchFriendsFavorites() {
        viewModelScope.launch {
            _friendsBooks.value = Resource.Loading

            try {
                // 1. Obtenemos la red de amigos
                val friendIds = communityRepo.getMyFriendsIds().getOrDefault(emptySet())

                if (friendIds.isEmpty()) {
                    _friendsBooks.value = Resource.Success(emptyList())
                    return@launch
                }

                // FASE 1: Recolectamos IDs de libros favoritos de todos los amigos en paralelo
                val allBookIds = coroutineScope {
                    friendIds.map { friendId ->
                        async {
                            userRepo.getFavoriteIds(friendId)
                                .getOrDefault(emptyList())
                                .take(5) // Tomamos solo los 5 más recientes por amigo
                        }
                    }.awaitAll()
                        .flatten()
                        .distinct()
                        .take(15) // Limitamos el pool total para agilizar la UI
                }

                // FASE 2: Descargamos los detalles (portadas, autores) de esos libros en paralelo
                val booksDetails = coroutineScope {
                    allBookIds.map { bookId ->
                        async { repository.getBookDetail(bookId).getOrNull() }
                    }.awaitAll().filterNotNull()
                }

                // FASE 3: Ordenación cronológica inversa (Más recientes arriba) usando Regex
                val sortedBooks = booksDetails.sortedByDescending { book ->
                    Regex("\\d{4}")
                        .find(book.lanzamiento)
                        ?.value
                        ?.toIntOrNull() ?: 0
                }

                _friendsBooks.value = Resource.Success(sortedBooks)

            } catch (e: Exception) {
                _friendsBooks.value = Resource.Error(e)
            }
        }
    }
}