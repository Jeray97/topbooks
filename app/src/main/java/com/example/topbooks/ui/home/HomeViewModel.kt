package com.example.topbooks.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.repository.*
import com.example.topbooks.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Modelo de datos para la sección social de la Home.
 * Vincula un libro específico con el amigo que lo tiene en favoritos.
 */
data class FriendBookRecommendation(
    val book: Book,
    val friendName: String,
    val friendPhotoUrl: String
)

/**
 * ViewModel central para la pantalla de Inicio.
 * Gestiona la lógica de recomendación personalizada y la integración de datos sociales.
 * * ESTRATEGIA DE CARGA: Utiliza un sistema de "Lazy Loading" controlado por [isDataLoaded]
 * para evitar llamadas redundantes a la API en recomposiciones o cambios de configuración.
 */
class HomeViewModel(
    private val booksRepository: BooksRepository = BooksRepository(),
    private val communityRepository: CommunityRepository = CommunityRepositoryImpl(),
    private val userRepository: UserRepository = UserRepositoryImpl()
) : ViewModel() {

    // --- ESTADOS DE UI (STATEFLOW) ---
    private val _categoryBooks = MutableStateFlow<Resource<List<Book>>>(Resource.Loading)
    val categoryBooks: StateFlow<Resource<List<Book>>> = _categoryBooks.asStateFlow()

    private val _recommendedBooks = MutableStateFlow<Resource<List<Book>>>(Resource.Loading)
    val recommendedBooks: StateFlow<Resource<List<Book>>> = _recommendedBooks.asStateFlow()

    private val _friendsBooks = MutableStateFlow<Resource<List<FriendBookRecommendation>>>(Resource.Loading)
    val friendsBooks: StateFlow<Resource<List<FriendBookRecommendation>>> = _friendsBooks.asStateFlow()

    private var isDataLoaded = false

    /**
     * Orquestador principal de carga de datos.
     * @param categoryQuery Término para la fila de categorías (ej. "Fiction").
     * @param fallbackRecommendedQuery Término de respaldo si el usuario no tiene preferencias aún.
     */
    fun loadData(categoryQuery: String, fallbackRecommendedQuery: String) {
        if (isDataLoaded) return
        isDataLoaded = true

        // Carga de libros por categoría general
        fetchBooks(categoryQuery, _categoryBooks, filterModern = true)

        // Algoritmo de recomendaciones personalizadas
        fetchPersonalizedRecommendations(fallbackRecommendedQuery)

        // Feed de libros favoritos de amigos
        fetchFriendsFavorites()
    }

    /**
     * MOTOR DE RECOMENDACIÓN PERSONALIZADO (Algoritmo en Cascada):
     * 1. Busca libros de la misma categoría que los "Favoritos" actuales del usuario.
     * 2. Busca libros basados en los "Géneros Favoritos" marcados en el perfil.
     * 3. Rellena con "Libros Populares" globales si falta contenido.
     * 4. Usa un "Fallback" general si to-do lo anterior falla.
     */
    private fun fetchPersonalizedRecommendations(fallbackQuery: String) {
        viewModelScope.launch {
            _recommendedBooks.value = Resource.Loading
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                val db = FirebaseFirestore.getInstance()
                val personalizedBooks = mutableListOf<Book>()

                if (uid != null) {
                    val userDoc = db.collection("users").document(uid).get().await()
                    val favoriteGenres = (userDoc.get("favoriteGenres") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val favoriteBookIds = (userDoc.get("favoriteBooks") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

                    // 1️ HIDRATACIÓN POR SIMILITUD: Buscamos libros similares a los que ya le gustan
                    val booksFromFavorites = coroutineScope {
                        favoriteBookIds.take(3).map { bookId ->
                            async {
                                val book = booksRepository.getBookDetail(bookId).getOrNull()
                                book?.categories?.firstOrNull()?.let { category ->
                                    booksRepository.searchHybrid("subject:$category").getOrNull()?.take(4)
                                } ?: emptyList()
                            }
                        }
                    }.awaitAll().flatten()
                    personalizedBooks.addAll(booksFromFavorites)

                    // 2️ HIDRATACIÓN POR GÉNERO: Preferencias declaradas en el perfil
                    if (favoriteGenres.isNotEmpty()) {
                        val genreBooks = coroutineScope {
                            favoriteGenres.take(3).map { genre ->
                                async {
                                    booksRepository.searchHybrid("subject:$genre").getOrNull()?.take(4) ?: emptyList()
                                }
                            }
                        }.awaitAll().flatten()
                        personalizedBooks.addAll(genreBooks)
                    }
                }

                // 3️ POPULARES: Si la lista es corta, añadimos lo más leído en la plataforma
                if (personalizedBooks.size < 10) {
                    val popularBooks = booksRepository.getPopularBooks(10)
                    personalizedBooks.addAll(popularBooks)
                }

                // 4 FALLBACK: Último recurso si no hay conexión o datos de usuario
                if (personalizedBooks.isEmpty()) {
                    val fallback = booksRepository.searchHybrid(fallbackQuery).getOrNull() ?: emptyList()
                    personalizedBooks.addAll(fallback)
                }

                // Limpieza de duplicados y mezcla aleatoria para dar frescura al feed
                val finalBooks = personalizedBooks
                    .distinctBy { it.id }
                    .shuffled()
                    .take(10)

                _recommendedBooks.value = Resource.Success(finalBooks)

            } catch (e: Exception) {
                Log.e("HomeVM", "Error recomendaciones: ${e.message}")
                _recommendedBooks.value = Resource.Error(e)
            }
        }
    }

    /**
     * Obtiene los libros favoritos de la red de amigos.
     * Cruza datos de CommunityRepository (IDs) con UserRepository (Perfiles)
     * y BooksRepository (Detalles del libro).
     */
    private fun fetchFriendsFavorites() {
        viewModelScope.launch {
            try {
                _friendsBooks.value = Resource.Loading
                val friendIds = communityRepository.getMyFriendsIds().getOrDefault(emptySet())

                if (friendIds.isEmpty()) {
                    _friendsBooks.value = Resource.Success(emptyList())
                    return@launch
                }

                val recommendations = coroutineScope {
                    friendIds.map { friendId ->
                        async {
                            val user = userRepository.getUserProfile(friendId).getOrNull()
                            val favIds = userRepository.getFavoriteIds(friendId).getOrDefault(emptyList())

                            favIds.take(2).mapNotNull { bookId ->
                                val book = booksRepository.getBookDetail(bookId).getOrNull()
                                if (book != null && user != null) {
                                    FriendBookRecommendation(book, user.displayName, user.photoURL)
                                } else null
                            }
                        }
                    }.awaitAll().flatten()
                }

                _friendsBooks.value = Resource.Success(recommendations.shuffled().take(10))
            } catch (e: Exception) {
                Log.e("HomeVM", "Error cargando favoritos amigos: ${e.message}")
                _friendsBooks.value = Resource.Error(e)
            }
        }
    }

    /**
     * Función genérica para fetching de libros desde la API principal.
     */
    private fun fetchBooks(
        query: String,
        state: MutableStateFlow<Resource<List<Book>>>,
        filterModern: Boolean = false
    ) {
        viewModelScope.launch {
            state.value = Resource.Loading
            val result = booksRepository.getBooks(query = query, filterModern = filterModern)
            if (result.isSuccess) {
                state.value = Resource.Success(result.getOrDefault(emptyList()))
            } else {
                state.value = Resource.Error(result.exceptionOrNull() ?: Exception("Error"))
            }
        }
    }
}