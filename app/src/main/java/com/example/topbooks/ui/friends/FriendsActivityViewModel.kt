package com.example.topbooks.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.repository.*
import com.example.topbooks.utils.Resource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

/**
 * Modelo de datos unificado para la interfaz de actividad social.
 * * Transforma diferentes entidades de Firestore (Reviews, Comments, Favorites) en un solo
 * formato manejable por la lista (LazyColumn) de la UI.
 *
 * @property id Identificador único generado al vuelo para la lista de Compose.
 * @property type Tipo de actividad (REVIEW, COMMENT, FAVORITE).
 * @property friendName Nombre del amigo que realizó la acción.
 * @property friendPhotoUrl URL o nombre del avatar local del amigo.
 * @property bookId ID del libro sobre el que se realizó la acción.
 * @property bookTitle Título del libro (para mostrarlo si la portada no carga).
 * @property bookImageUrl Portada del libro.
 * @property content El texto de la reseña, comentario o el texto autogenerado para favoritos.
 * @property rating Puntuación de 1 a 5 (solo aplica si es una reseña).
 * @property timestamp Fecha exacta en la que se realizó la acción, usada para ordenar el feed.
 */
data class FriendActivityItem(
    val id: String = UUID.randomUUID().toString(),
    val type: ActivityType,
    val friendName: String,
    val friendPhotoUrl: String,
    val bookId: String,
    val bookTitle: String,
    val bookImageUrl: String,
    val content: String,
    val rating: Int,
    val timestamp: Date
)

/**
 * ViewModel que orquesta y construye el Feed Social ("Actividad de Amigos").
 * * Conecta con múltiples repositorios simultáneamente para cruzar datos de amistades,
 * interacciones y detalles de libros.
 */
class FriendsActivityViewModel(
    private val feedRepository: SocialFeedRepository = SocialFeedRepositoryImpl(),
    private val communityRepository: CommunityRepository = CommunityRepositoryImpl(),
    private val userRepository: UserRepository = UserRepositoryImpl(),
    private val booksRepository: BooksRepository = BooksRepository()
) : ViewModel() {

    // --- ESTADO ---
    // Usamos la clase envoltorio [Resource] para manejar los estados de carga de la red.
    private val _uiState = MutableStateFlow<Resource<List<FriendActivityItem>>>(Resource.Loading)
    val uiState: StateFlow<Resource<List<FriendActivityItem>>> = _uiState.asStateFlow()

    init {
        loadActivityFeed()
    }

    /**
     * Construye el feed de actividad de los amigos en tiempo real.
     * * ARQUITECTURA NOSQL AVANZADA: Al no existir 'JOINs' relacionales en Firestore, este méto-do
     * descarga los datos en crudo y los cruza en memoria utilizando Corrutinas en paralelo.
     */
    fun loadActivityFeed() {
        viewModelScope.launch {
            _uiState.value = Resource.Loading
            try {
                // 1. Obtenemos la lista de IDs de los usuarios a los que seguimos
                val friendsIds = communityRepository.getMyFriendsIds().getOrDefault(emptySet()).toList()
                if (friendsIds.isEmpty()) {
                    _uiState.value = Resource.Success(emptyList()) // No hay amigos, feed vacío
                    return@launch
                }

                // 2. Por cada amigo, lanzamos un bloque 'async' para buscar sus datos al mismo tiempo
                val allActivities = coroutineScope {
                    friendsIds.map { friendId ->
                        async {
                            // Descargamos el perfil del amigo para tener su nombre y foto actualizados
                            val user = userRepository.getUserProfile(friendId).getOrNull() ?: return@async emptyList()
                            val friendName = user.displayName.ifEmpty { "Usuario" }
                            val friendPhoto = user.photoURL.ifEmpty { "capibara_1" }

                            // Descargamos todas sus interacciones
                            val reviews = feedRepository.getUserReviews(friendId).getOrDefault(emptyList())
                            val comments = feedRepository.getUserComments(friendId).getOrDefault(emptyList())
                            val favorites = feedRepository.getUserFavorites(friendId).getOrDefault(emptyList())

                            val items = mutableListOf<FriendActivityItem>()

                            // Mapeamos las Reseñas
                            reviews.forEach { r ->
                                val (title, img) = getBookInfo(r.bookId)
                                items.add(FriendActivityItem(type = ActivityType.REVIEW, friendName = friendName, friendPhotoUrl = friendPhoto, bookId = r.bookId, bookTitle = title, bookImageUrl = img, content = r.text, rating = r.rating, timestamp = r.createAt ?: Date()))
                            }

                            // Mapeamos los Comentarios
                            comments.forEach { c ->
                                val (title, img) = getBookInfo(c.bookId)
                                items.add(FriendActivityItem(type = ActivityType.COMMENT, friendName = friendName, friendPhotoUrl = friendPhoto, bookId = c.bookId, bookTitle = title, bookImageUrl = img, content = c.text, rating = 0, timestamp = c.createAt ?: Date()))
                            }

                            // Mapeamos los Favoritos (generando un texto fijo para ellos)
                            favorites.forEach { fav ->
                                val bookId = fav["bookId"] as? String ?: return@forEach
                                val timestamp = fav["addedAt"] as? Long ?: 0L
                                val (title, img) = getBookInfo(bookId)
                                items.add(FriendActivityItem(type = ActivityType.FAVORITE, friendName = friendName, friendPhotoUrl = friendPhoto, bookId = bookId, bookTitle = title, bookImageUrl = img, content = "Ha añadido un libro a favoritos", rating = 0, timestamp = Date(timestamp)))
                            }

                            items // Retornamos la lista de este amigo específico
                        }
                    }.awaitAll() // Esperamos a que TODAS las búsquedas de todos los amigos terminen
                        .flatten() // Unimos la lista de listas en una sola lista general
                        .sortedByDescending { it.timestamp } // Ordenamos to-do cronológicamente, lo más nuevo arriba
                }

                _uiState.value = Resource.Success(allActivities)
            } catch (e: Exception) {
                _uiState.value = Resource.Error(e)
            }
        }
    }

    /**
     * Función auxiliar para obtener rápidamente el título y la portada de un libro basándose en su ID.
     * @return Un par (Pair) donde el primer valor es el Título y el segundo es la URL de la portada.
     */
    private suspend fun getBookInfo(bookId: String): Pair<String, String> {
        val apiBook = booksRepository.getBookDetail(bookId).getOrNull()
        return Pair(apiBook?.title ?: "Libro", apiBook?.imageUrl ?: "")
    }
}