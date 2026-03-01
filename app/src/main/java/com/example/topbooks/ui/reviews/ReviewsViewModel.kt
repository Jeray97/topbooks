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

data class ReviewsFeedState(
    val friendsReviews: List<Comment> = emptyList(),
    val communityReviews: List<Comment> = emptyList(),
    val targetReview: Comment? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

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


    fun loadSocialFeed(bookId: String? = null, targetCommentId: String? = null) {
        currentBookId = bookId

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Obtenemos los amigos
                val myFriends = communityRepository.getMyFriendsIds().getOrDefault(emptySet()).toList()
                val friendsComments = mutableListOf<Comment>()

                if (myFriends.isNotEmpty()) {
                    val deferredFriends = myFriends.map { friendId ->
                        async { feedRepository.getUserComments(friendId).getOrDefault(emptyList()) }
                    }
                    friendsComments.addAll(deferredFriends.awaitAll().flatten())
                }

                // Obtenemos los globales
                val globalComments = feedRepository.getCommunityComments(50).getOrDefault(emptyList()) // Subí el límite a 50 para que haya más de donde filtrar

                // Filtramos si hay un bookId
                val filteredFriends = if (bookId != null) friendsComments.filter { it.bookId == bookId } else friendsComments
                val filteredGlobal = if (bookId != null) globalComments.filter { it.bookId == bookId } else globalComments

                // Enriquecemos (añadimos nombre de usuario y título del libro)
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
                Log.e("ReviewsVM", "Error: ${e.message}")
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }


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


    fun addReply(targetComment: Comment, text: String) {
        val myUid = userRepository.getCurrentUserId() ?: return

        viewModelScope.launch {
            try {
                val me = userRepository.getUserProfile(myUid).getOrNull()
                val myName = me?.displayName ?: "Usuario"
                val myPhoto = me?.photoURL ?: "capibara_1"

                val newReply = Reply(userId = myUid, userName = myName, userPhotoUrl = myPhoto, text = text)

                val targetUser = userRepository.getUserProfile(targetComment.userId).getOrNull()
                val targetToken = targetUser?.fcmToken

                feedRepository.addReply(targetComment.commentId, newReply, targetToken, targetComment.bookId)


                loadSocialFeed(currentBookId)
            } catch (e: Exception) {
                Log.e("ReviewsVM", "Error al enviar respuesta: ${e.message}")
            }
        }
    }

    private suspend fun enrichComments(comments: List<Comment>): List<Comment> {
        return comments.map { comment ->
            viewModelScope.async {
                var enriched = comment
                val user = userRepository.getUserProfile(comment.userId).getOrNull()
                if (user != null) {
                    enriched = enriched.copy(userName = user.displayName, userPhotoUrl = user.photoURL)
                }

                val book = booksRepository.getBookDetail(comment.bookId).getOrNull()
                if (book != null) {
                    enriched = enriched.copy(bookTitle = book.title, bookImageUrl = book.imageUrl)
                }
                enriched
            }
        }.awaitAll()
    }


}