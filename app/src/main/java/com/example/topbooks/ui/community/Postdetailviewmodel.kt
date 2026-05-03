package com.example.topbooks.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * VIEWMODEL DEL DETALLE DE POST
 *
 * Responsabilidades:
 *  - Cargar el post + sus reacciones agregadas + el hilo plano de replies.
 *  - Gestionar las reacciones del usuario (toggle con optimistic UI).
 *  - Enviar respuestas nuevas al hilo.
 *  - Likes en respuestas individuales.
 *  - Estado del emoji picker (abierto / cerrado).
 *
 * Por ahora consume MockPostDetailRepository (datos en memoria). Los métodos
 * son síncronos por simplicidad — al pasar a Firestore haremos suspend funs.
 */
class PostDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    /**
     * "Usuario actual" mock. En producción vendría del repositorio de auth/users.
     * Lo necesitamos para construir las respuestas que escriba el usuario.
     */
    private val mockCurrentUser = PostAuthor(
        id = "u_me",
        displayName = "Tú",
        photoUrl = null,
        isFriend = false,
        isVerified = true
    )

    /**
     * Carga el post inicial + reacciones + replies.
     * Si el postId no existe, deja la UI con isLoading=false y post=null
     * (la pantalla mostrará un mensaje de "no encontrado").
     */
    fun loadPost(postId: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            MockPostDetailRepository.getPostDetail(postId).collect { snapshot ->
                if (snapshot == null) {
                    _uiState.update {
                        it.copy(isLoading = false, post = null, errorMessage = "Post no encontrado")
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            post = snapshot.post,
                            reactions = snapshot.reactions,
                            replies = snapshot.replies,
                            totalReactionCount = snapshot.totalReactionCount,
                            savedCount = snapshot.savedCount
                        )
                    }
                }
            }
        }
    }

    /**
     * Toggle de una reacción (top fijo o emoji custom). Aplica optimistic UI:
     * actualizamos local antes de "confirmar" con el repo.
     */
    fun toggleReaction(emoji: String) {
        val postId = _uiState.value.post?.id ?: return
        val snapshot = MockPostDetailRepository.toggleReaction(postId, emoji) ?: return
        _uiState.update {
            it.copy(
                reactions = snapshot.reactions,
                totalReactionCount = snapshot.totalReactionCount,
                emojiPickerOpen = false  // Si estaba abierto el picker, lo cerramos al elegir
            )
        }
    }

    /**
     * Toggle del estado del emoji picker (botón "+ reaccionar").
     */
    fun toggleEmojiPicker() {
        _uiState.update { it.copy(emojiPickerOpen = !it.emojiPickerOpen) }
    }

    /**
     * Envía una nueva respuesta al hilo.
     * Mientras se envía, marcamos isSendingReply=true para que la UI pueda
     * deshabilitar el botón de envío (evita doble click).
     */
    fun sendReply(text: String, onSuccess: () -> Unit = {}) {
        val post = _uiState.value.post ?: return
        if (text.isBlank()) return

        _uiState.update { it.copy(isSendingReply = true) }
        viewModelScope.launch {
            delay(200)  // Simulamos latencia
            val snapshot = MockPostDetailRepository.addReply(
                postId = post.id,
                body = text.trim(),
                currentUserAuthor = mockCurrentUser,
                postOriginalAuthorId = post.author.id
            )
            if (snapshot != null) {
                _uiState.update {
                    it.copy(
                        replies = snapshot.replies,
                        isSendingReply = false
                    )
                }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isSendingReply = false,
                        errorMessage = "No se pudo enviar la respuesta"
                    )
                }
            }
        }
    }

    /**
     * Toggle de like sobre una respuesta concreta.
     */
    fun toggleReplyLike(replyId: String) {
        val postId = _uiState.value.post?.id ?: return
        val snapshot = MockPostDetailRepository.toggleReplyLike(postId, replyId) ?: return
        _uiState.update { it.copy(replies = snapshot.replies) }
    }

    /**
     * Limpia el mensaje de error tras mostrarlo.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}