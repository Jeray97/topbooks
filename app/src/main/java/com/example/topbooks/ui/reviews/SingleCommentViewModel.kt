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

data class SingleCommentState(
    val comment: Comment? = null,
    val isLoading: Boolean = false,
    val isSendingReply: Boolean = false
)

class SingleCommentViewModel(
    private val feedRepo: SocialFeedRepository = SocialFeedRepositoryImpl(),
    private val userRepo: UserRepository = UserRepositoryImpl(),
    private val booksRepo: BooksRepository = BooksRepository(),
    private val authRepo: AuthRepository = AuthRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SingleCommentState())
    val uiState: StateFlow<SingleCommentState> = _uiState.asStateFlow()

    fun loadComment(commentId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 🔥 AHORA USAMOS OBSERVE Y COLLECT PARA TIEMPO REAL
            feedRepo.observeCommentById(commentId).collect { result ->
                if (result.isSuccess) {
                    var c = result.getOrNull()!!
                    // Enriquecemos el comentario principal
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

    fun checkEmailVerification(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            authRepo.reloadUser()
            onResult(authRepo.isEmailVerified())
        }
    }

    fun sendReply(text: String, onSuccess: () -> Unit) {
        // VALIDACIÓN: Prevenir envío de texto vacío o solo espacios
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return

        val comment = _uiState.value.comment ?: return
        val myUid = userRepo.getCurrentUserId() ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingReply = true) }
            try {
                val me = userRepo.getUserProfile(myUid).getOrNull()
                val myName = me?.displayName ?: "Usuario"
                val myPhoto = me?.photoURL ?: "capibara_1"

                // Usamos el cleanText validado
                val newReply = Reply(userId = myUid, userName = myName, userPhotoUrl = myPhoto, text = cleanText)

                val targetUser = userRepo.getUserProfile(comment.userId).getOrNull()
                val targetToken = targetUser?.fcmToken

                // Guardamos en Firebase
                feedRepo.addReply(comment.commentId, newReply, targetToken, comment.bookId)

                // Eliminada la recarga manual (loadComment) porque Flow ya lo actualiza automáticamente
                onSuccess()
            } catch (e: Exception) {
                Log.e("SingleCommentVM", "Error al enviar respuesta: ${e.message}")
            } finally {
                _uiState.update { it.copy(isSendingReply = false) }
            }
        }
    }
}