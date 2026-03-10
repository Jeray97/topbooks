package com.example.topbooks.ui.friends

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.repository.*
import com.example.topbooks.utils.Resource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

/**
 * Representa un elemento individual en el muro de actividad social.
 * Contiene toda la información necesaria para renderizar tarjetas de reseñas,
 * favoritos, comentarios y respuestas (replies).
 */
data class SocialActivityItem(
    val id: String = UUID.randomUUID().toString(),
    val type: ActivityType,
    val friendName: String,
    val friendPhotoUrl: String,
    val bookId: String,
    val bookTitle: String,
    val bookImageUrl: String,
    val content: String,
    val rating: Int,
    val timestamp: Date,
    val replyToName: String? = null,    // Nombre del autor original si es una respuesta
    val replyToContent: String? = null, // Contenido original si es una respuesta
    val commentId: String? = null       // ID para navegación directa al hilo
)

/**
 * Define los tipos de interacciones sociales soportadas por la plataforma.
 */
enum class ActivityType { REVIEW, FAVORITE, COMMENT, REPLY }

/**
 * ViewModel encargado de construir el "Feed" o muro social del usuario.
 * * LÓGICA DE NEGOCIO: Cruza datos de amigos, sus actividades en Firebase y
 * detalles de libros de la API de Google Books para crear una lista unificada y cronológica.
 */
class SocialActivityViewModel(
    private val feedRepository: SocialFeedRepository = SocialFeedRepositoryImpl(),
    private val communityRepository: CommunityRepository = CommunityRepositoryImpl(),
    private val userRepository: UserRepository = UserRepositoryImpl(),
    private val booksRepository: BooksRepository = BooksRepository()
) : ViewModel() {

    // Estado que expone la lista de actividades envuelta en un Resource (Loading, Success, Error)
    private val _uiState = MutableStateFlow<Resource<List<SocialActivityItem>>>(Resource.Loading)
    val uiState: StateFlow<Resource<List<SocialActivityItem>>> = _uiState.asStateFlow()

    init {
        loadActivityFeed()
    }

    /**
     * Carga y procesa toda la actividad de los amigos del usuario actual.
     * * TÉCNICA DE RENDIMIENTO: Utiliza Corrutinas paralelas (`async`) para consultar
     * la actividad de cada amigo simultáneamente, reduciendo drásticamente el tiempo de carga total.
     */
    fun loadActivityFeed() {
        viewModelScope.launch {
            _uiState.value = Resource.Loading
            try {
                // 1. Obtenemos la lista de IDs de mis amigos
                val friendsIds = communityRepository.getMyFriendsIds().getOrDefault(emptySet()).toList()

                if (friendsIds.isEmpty()) {
                    _uiState.value = Resource.Success(emptyList())
                    return@launch
                }

                // 2. Por cada amigo, lanzamos una tarea asíncrona para recolectar su actividad
                val activitiesDeferred = friendsIds.map { friendId ->
                    async {
                        // Obtenemos perfil básico del amigo
                        val user = userRepository.getUserProfile(friendId).getOrNull() ?: return@async emptyList()
                        val friendName = user.displayName.ifEmpty { "Usuario" }
                        val friendPhoto = user.photoURL.ifEmpty { "capibara_1" }

                        // Consultamos sus 3 tipos de actividad en Firebase
                        val reviews = feedRepository.getUserReviews(friendId).getOrDefault(emptyList())
                        val comments = feedRepository.getUserComments(friendId).getOrDefault(emptyList())
                        val favorites = feedRepository.getUserFavorites(friendId).getOrDefault(emptyList())

                        val items = mutableListOf<SocialActivityItem>()

                        // Procesamos Reseñas
                        reviews.forEach { r ->
                            val (bookTitle, bookImage) = getBookInfo(r.bookId)
                            items.add(SocialActivityItem(
                                type = ActivityType.REVIEW,
                                friendName = friendName,
                                friendPhotoUrl = friendPhoto,
                                bookId = r.bookId,
                                bookTitle = bookTitle,
                                bookImageUrl = bookImage,
                                content = r.text,
                                rating = r.rating,
                                timestamp = r.createAt ?: Date()
                            ))
                        }

                        // Procesamos Comentarios y sus Respuestas (Replies)
                        comments.forEach { c ->
                            val (bookTitle, bookImage) = getBookInfo(c.bookId)

                            // El comentario principal
                            items.add(SocialActivityItem(
                                type = ActivityType.COMMENT,
                                friendName = friendName,
                                friendPhotoUrl = friendPhoto,
                                bookId = c.bookId,
                                bookTitle = bookTitle,
                                bookImageUrl = bookImage,
                                content = c.text,
                                rating = 0,
                                timestamp = c.createAt ?: Date(),
                                commentId = c.commentId
                            ))

                            // Las respuestas que el amigo haya hecho en este u otros hilos
                            c.replies.forEach { reply ->
                                if (reply.userId == friendId) {
                                    items.add(SocialActivityItem(
                                        type = ActivityType.REPLY,
                                        friendName = friendName,
                                        friendPhotoUrl = friendPhoto,
                                        bookId = c.bookId,
                                        bookTitle = bookTitle,
                                        bookImageUrl = bookImage,
                                        content = reply.text,
                                        rating = 0,
                                        timestamp = Date(reply.timestamp),
                                        replyToName = c.userName,
                                        replyToContent = c.text,
                                        commentId = c.commentId
                                    ))
                                }
                            }
                        }

                        // Procesamos Favoritos
                        favorites.forEach { fav ->
                            val bookId = fav["bookId"] as? String ?: return@forEach
                            val timestamp = fav["addedAt"] as? Long ?: 0L
                            val (bookTitle, bookImage) = getBookInfo(bookId)
                            items.add(SocialActivityItem(
                                type = ActivityType.FAVORITE,
                                friendName = friendName,
                                friendPhotoUrl = friendPhoto,
                                bookId = bookId,
                                bookTitle = bookTitle,
                                bookImageUrl = bookImage,
                                content = "Ha añadido un libro a sus favoritos",
                                rating = 0,
                                timestamp = Date(timestamp)
                            ))
                        }

                        items
                    }
                }

                // 3. Esperamos a que todas las tareas terminen, aplanamos las listas y ordenamos por fecha (más reciente primero)
                val allActivities = activitiesDeferred.awaitAll().flatten().sortedByDescending { it.timestamp }
                _uiState.value = Resource.Success(allActivities)

            } catch (e: Exception) {
                Log.e("SocialActivityVM", "Error cargando feed: ${e.message}")
                _uiState.value = Resource.Error(e)
            }
        }
    }

    /**
     * Función auxiliar para hidratar los datos de actividad con información del libro.
     * Dado que Firebase solo guarda el ID del libro, consultamos el repositorio de libros para obtener Título e Imagen.
     */
    private suspend fun getBookInfo(bookId: String): Pair<String, String> {
        return try {
            val apiBook = booksRepository.getBookDetail(bookId).getOrNull()
            Pair(apiBook?.title ?: "Libro", apiBook?.imageUrl ?: "")
        } catch (e: Exception) {
            Pair("Libro", "")
        }
    }
}