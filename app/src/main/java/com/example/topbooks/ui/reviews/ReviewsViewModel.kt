package com.example.topbooks.ui.reviews

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Comment
import com.example.topbooks.data.model.Reply
import com.example.topbooks.data.repository.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado que representa el feed de reseñas y comentarios.
 * @property friendsReviews Comentarios realizados por los contactos del usuario.
 * @property communityReviews Comentarios generales de toda la plataforma.
 * @property targetReview El comentario que se está respondiendo actualmente en la UI.
 * @property isLoading Indica si se están descargando datos de red.
 * @property errorMessage Mensaje descriptivo en caso de fallo.
 */
data class ReviewsFeedState(
    val friendsReviews: List<Comment> = emptyList(),
    val communityReviews: List<Comment> = emptyList(),
    val targetReview: Comment? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel que orquesta la actividad social y el sistema de hilos de comentarios.
 * Conecta múltiples repositorios para cruzar datos de amistades, perfiles,
 * libros de la API y persistencia de comentarios en Firebase.
 */
class ReviewsViewModel(
    private val feedRepository: SocialFeedRepository = SocialFeedRepositoryImpl(),
    private val communityRepository: CommunityRepository = CommunityRepositoryImpl(),
    private val userRepository: UserRepository = UserRepositoryImpl(),
    private val booksRepository: BooksRepository = BooksRepository(),
    private val authRepository: AuthRepository = AuthRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewsFeedState())
    val uiState: StateFlow<ReviewsFeedState> = _uiState.asStateFlow()

    private var currentBookId: String? = null

    init {
        loadSocialFeed()
    }

    /**
     * Carga y procesa el feed social filtrando por libro o mostrando la actividad global.
     * * El proceso sigue estos pasos:
     * 1. Obtiene los IDs de los amigos.
     * 2. Descarga los comentarios de esos amigos en paralelo.
     * 3. Descarga los comentarios globales de la comunidad.
     * 4. Filtra por libro si es necesario.
     * 5. Hidrata (enriquece) los comentarios con nombres, fotos y títulos de libros.
     * * @param bookId Si se provee, muestra solo comentarios de ese libro.
     * @param targetCommentId ID del comentario a resaltar (opcional).
     */
    fun loadSocialFeed(bookId: String? = null, targetCommentId: String? = null) {
        currentBookId = bookId

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Obtenemos los amigos
                val myFriends = communityRepository.getMyFriendsIds().getOrDefault(emptySet()).toList()
                val friendsComments = mutableListOf<Comment>()

                if (myFriends.isNotEmpty()) {
                    // Descarga paralela para cada amigo
                    val deferredFriends = myFriends.map { friendId ->
                        async { feedRepository.getUserComments(friendId).getOrDefault(emptyList()) }
                    }
                    friendsComments.addAll(deferredFriends.awaitAll().flatten())
                }

                // Obtenemos los comentarios globales de la comunidad
                // Se sube el límite a 50 para garantizar que haya contenido suficiente tras filtrar
                val globalComments = feedRepository.getCommunityComments(50).getOrDefault(emptyList())

                // Aplicamos filtrado por libro si el contexto de la pantalla lo requiere
                val filteredFriends = if (bookId != null) friendsComments.filter { it.bookId == bookId } else friendsComments
                val filteredGlobal = if (bookId != null) globalComments.filter { it.bookId == bookId } else globalComments

                // Enriquecimiento de datos: Añadimos metadatos de usuario y de libros
                val enrichedFriends = enrichComments(filteredFriends)
                val enrichedGlobal = enrichComments(filteredGlobal)

                _uiState.update {
                    it.copy(
                        friendsReviews = enrichedFriends.sortedByDescending { c -> c.createAt },
                        communityReviews = enrichedGlobal.sortedByDescending { c -> c.createAt },
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("ReviewsVM", "Error cargando feed social: ${e.message}")
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    /**
     * Verifica en Firebase si el usuario ha confirmado su dirección de correo electrónico.
     */
    fun checkEmailVerification(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            authRepository.reloadUser()
            onResult(authRepository.isEmailVerified())
        }
    }

    fun openReplyDialog(comment: Comment) {
        _uiState.update { it.copy(targetReview = comment) }
    }

    fun closeReplyDialog() {
        _uiState.update { it.copy(targetReview = null) }
    }

    /**
     * Publica una respuesta a un comentario existente.
     * * Además de guardar la respuesta en Firestore, recupera el token FCM del destinatario
     * para permitir el envío de una notificación push de aviso.
     * * @param targetComment El comentario original al que se responde.
     * @param text El contenido de la respuesta.
     */
    fun addReply(targetComment: Comment, text: String) {
        val myUid = userRepository.getCurrentUserId() ?: return

        viewModelScope.launch {
            try {
                // Obtenemos datos de nuestro perfil para la respuesta
                val me = userRepository.getUserProfile(myUid).getOrNull()
                val myName = me?.displayName ?: "Usuario"
                val myPhoto = me?.photoURL ?: "capibara_1"

                val newReply = Reply(userId = myUid, userName = myName, userPhotoUrl = myPhoto, text = text)

                // Buscamos el token del destinatario para la notificación
                val targetUser = userRepository.getUserProfile(targetComment.userId).getOrNull()
                val targetToken = targetUser?.fcmToken

                // Persistimos en Firebase
                feedRepository.addReply(targetComment.commentId, newReply, targetToken, targetComment.bookId)

                // Recargamos el feed para mostrar la respuesta inmediatamente
                loadSocialFeed(currentBookId)
            } catch (e: Exception) {
                Log.e("ReviewsVM", "Error al enviar respuesta: ${e.message}")
            }
        }
    }

    /**
     * Función auxiliar para hidratar listas de comentarios.
     * Consulta el perfil de cada autor y los detalles del libro asociado en paralelo.
     */
    private suspend fun enrichComments(comments: List<Comment>): List<Comment> {
        return comments.map { comment ->
            viewModelScope.async {
                var enriched = comment
                // Enriquecimiento de perfil de usuario
                val user = userRepository.getUserProfile(comment.userId).getOrNull()
                if (user != null) {
                    enriched = enriched.copy(userName = user.displayName, userPhotoUrl = user.photoURL)
                }

                // Enriquecimiento de detalles del libro desde la API
                val book = booksRepository.getBookDetail(comment.bookId).getOrNull()
                if (book != null) {
                    enriched = enriched.copy(bookTitle = book.title, bookImageUrl = book.imageUrl)
                }
                enriched
            }
        }.awaitAll()
    }
}