package com.example.topbooks.ui.reviews

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Comment
import com.example.topbooks.data.model.Reply
import com.example.topbooks.data.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Representa el estado de la interfaz de usuario para la vista de un comentario individual.
 *
 * @property comment El objeto de comentario cargado, enriquecido con datos de usuario y libro.
 * @property isLoading Indica si se está realizando la carga inicial del hilo de conversación.
 * @property isSendingReply Indica si hay una operación de envío de respuesta en curso.
 */
data class SingleCommentState(
    val comment: Comment? = null,
    val isLoading: Boolean = false,
    val isSendingReply: Boolean = false
)

/**
 * ViewModel encargado de la lógica de visualización y respuesta para un hilo de conversación único.
 *
 * Utiliza flujos reactivos para mantener la interfaz actualizada en tiempo real ante cambios
 * en el servidor de base de datos.
 */
class SingleCommentViewModel(
    private val feedRepo: SocialFeedRepository = SocialFeedRepositoryImpl(),
    private val userRepo: UserRepository = UserRepositoryImpl(),
    private val booksRepo: BooksRepository = BooksRepository(),
    private val authRepo: AuthRepository = AuthRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SingleCommentState())
    val uiState: StateFlow<SingleCommentState> = _uiState.asStateFlow()

    /**
     * Inicia la observación del comentario por ID mediante un flujo en tiempo real.
     * * Cada vez que el comentario o sus respuestas cambien en la base de datos,
     * se recogen los datos, se enriquecen con información del autor y del libro,
     * y se actualiza el estado de la UI de forma automática.
     *
     * @param commentId Identificador único del comentario en el repositorio.
     */
    fun loadComment(commentId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Uso de observador en tiempo real para actualizaciones constantes
            feedRepo.observeCommentById(commentId).collect { result ->
                if (result.isSuccess) {
                    val c = result.getOrNull()!!

                    // Proceso de hidratación de datos: Autor y Libro
                    val user = userRepo.getUserProfile(c.userId).getOrNull()
                    val book = booksRepo.getBookDetail(c.bookId).getOrNull()

                    if (user != null) {
                        c.userName = user.displayName
                        c.userPhotoUrl = user.photoURL
                    }
                    if (book != null) {
                        c.bookTitle = book.title
                        c.bookImageUrl = book.imageUrl
                    }
                    _uiState.update { it.copy(comment = c, isLoading = false) }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    /**
     * Verifica si el usuario actual ha confirmado su dirección de correo electrónico.
     * * @param onResult Callback que retorna el estado de verificación.
     */
    fun checkEmailVerification(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            authRepo.reloadUser()
            onResult(authRepo.isEmailVerified())
        }
    }

    /**
     * Procesa y envía una respuesta al comentario actualmente cargado.
     * * Implementa validación de texto, gestión de estados de carga y disparo
     * de notificaciones FCM al autor del comentario original.
     *
     * @param text El contenido de la respuesta.
     * @param onSuccess Callback ejecutado tras la confirmación exitosa del envío.
     */
    fun sendReply(text: String, onSuccess: () -> Unit) {
        // Validacion de entrada: Evita cadenas vacías o solo con espacios
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return

        val comment = _uiState.value.comment ?: return
        val myUid = userRepo.getCurrentUserId() ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingReply = true) }
            try {
                // Recuperación de perfil del emisor para la respuesta
                val me = userRepo.getUserProfile(myUid).getOrNull()
                val myName = me?.displayName ?: "Usuario"
                val myPhoto = me?.photoURL ?: "capibara_1"

                val newReply = Reply(
                    userId = myUid,
                    userName = myName,
                    userPhotoUrl = myPhoto,
                    text = cleanText
                )

                // Obtención del token de notificación del destinatario
                val targetUser = userRepo.getUserProfile(comment.userId).getOrNull()
                val targetToken = targetUser?.fcmToken

                // Persistencia en repositorio social
                feedRepo.addReply(comment.commentId, newReply, targetToken, comment.bookId)

                // Notificamos exito a la vista
                onSuccess()
            } catch (e: Exception) {
                Log.e("SingleCommentViewModel", "Error al enviar respuesta: ${e.message}")
            } finally {
                _uiState.update { it.copy(isSendingReply = false) }
            }
        }
    }
}