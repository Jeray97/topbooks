package com.example.topbooks.ui.community

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.PostReply as DataPostReply
import com.example.topbooks.data.repository.BooksRepository
import com.example.topbooks.data.repository.CommunityRepository
import com.example.topbooks.data.repository.CommunityRepositoryImpl
import com.example.topbooks.data.repository.PostRepository
import com.example.topbooks.data.repository.PostRepositoryImpl
import com.example.topbooks.data.repository.UserRepository
import com.example.topbooks.data.repository.UserRepositoryImpl
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PostDetailViewModel(
    private val postRepository: PostRepository = PostRepositoryImpl(),
    private val userRepository: UserRepository = UserRepositoryImpl(),
    private val booksRepository: BooksRepository = BooksRepository(),
    private val communityRepository: CommunityRepository = CommunityRepositoryImpl()
) : ViewModel() {

    private val functions = FirebaseFunctions.getInstance()

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    private var myUid: String = ""
    private var friendIds: Set<String> = emptySet()
    private var currentPostId: String = ""

    init {
        viewModelScope.launch {
            myUid = userRepository.getCurrentUserId() ?: ""
            friendIds = communityRepository.getMyFriendsIds().getOrDefault(emptySet())
        }
    }

    fun loadPost(postId: String) {
        currentPostId = postId
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val dataPost = postRepository.getPostById(postId).getOrNull()
                if (dataPost == null) {
                    _uiState.update { it.copy(isLoading = false, post = null, errorMessage = "Post no encontrado") }
                    return@launch
                }

                val user = userRepository.getUserProfile(dataPost.userId).getOrNull()
                val book = if (dataPost.bookId.isNotBlank()) {
                    booksRepository.getBookDetail(dataPost.bookId).getOrNull()
                } else null

                val enrichedPost = dataPost.copy(
                    userName = user?.displayName ?: dataPost.userName,
                    userPhotoUrl = user?.photoURL ?: dataPost.userPhotoUrl,
                    bookTitle = book?.title ?: dataPost.bookTitle,
                    bookAuthor = book?.authors?.joinToString() ?: dataPost.bookAuthor,
                    bookImageUrl = book?.imageUrl ?: dataPost.bookImageUrl
                )

                val isFriend = dataPost.userId in friendIds
                val isLikedByMe = myUid in dataPost.likedBy
                val isSavedByMe = myUid in dataPost.savedBy
                val uiPost = enrichedPost.toUiPost(user, isFriend, isLikedByMe, isSavedByMe)

                val reactions = buildReactionsFromPost(enrichedPost, myUid)
                val replies = enrichReplies(enrichedPost.replies, dataPost.userId)
                val totalReactions = enrichedPost.reactions.values.sumOf { it.size }
                val savedCount = enrichedPost.savedBy.size

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        post = uiPost,
                        reactions = reactions,
                        replies = replies,
                        totalReactionCount = totalReactions,
                        savedCount = savedCount
                    )
                }
            } catch (e: Exception) {
                Log.e("PostDetailVM", "Error cargando post: ${e.message}")
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    private suspend fun enrichReplies(
        replies: List<DataPostReply>,
        originalAuthorId: String
    ): List<PostReply> {
        return replies.map { dataReply ->
            viewModelScope.async {
                val user = userRepository.getUserProfile(dataReply.userId).getOrNull()
                val enrichedReply = dataReply.copy(
                    userName = user?.displayName ?: dataReply.userName,
                    userPhotoUrl = user?.photoURL ?: dataReply.userPhotoUrl
                )
                val isFromOriginalAuthor = dataReply.userId == originalAuthorId
                val isLikedByMe = myUid in dataReply.likedBy
                enrichedReply.toUiPostReply(user, isFromOriginalAuthor, isLikedByMe)
            }
        }.awaitAll().sortedBy { it.createdAtMillis }
    }

    fun toggleReaction(emoji: String) {
        val post = _uiState.value.post ?: return
        val oldReactions = _uiState.value.reactions

        val updatedReactions = oldReactions.map { reaction ->
            if (reaction.emoji == emoji) {
                Reaction(
                    emoji = emoji,
                    count = if (reaction.reactedByMe) reaction.count - 1 else reaction.count + 1,
                    reactedByMe = !reaction.reactedByMe
                )
            } else reaction
        }.filter { it.emoji in TOP_FIXED_REACTIONS || it.count > 0 }
         .sortedWith(
             compareByDescending<Reaction> { it.emoji in TOP_FIXED_REACTIONS }
                 .thenByDescending { it.count }
         )

        val totalReactions = updatedReactions.sumOf { it.count }

        _uiState.update {
            it.copy(
                reactions = updatedReactions,
                totalReactionCount = totalReactions,
                emojiPickerOpen = false
            )
        }

        viewModelScope.launch {
            try {
                postRepository.toggleReaction(post.id, emoji, myUid)
            } catch (e: Exception) {
                Log.e("PostDetailVM", "Error toggle reaction: ${e.message}")
                _uiState.update { it.copy(reactions = oldReactions) }
            }
        }
    }

    fun toggleEmojiPicker() {
        _uiState.update { it.copy(emojiPickerOpen = !it.emojiPickerOpen) }
    }

    fun sendReply(text: String, onSuccess: () -> Unit = {}) {
        val post = _uiState.value.post ?: return
        if (text.isBlank()) return

        _uiState.update { it.copy(isSendingReply = true) }
        viewModelScope.launch {
            try {
                val me = userRepository.getUserProfile(myUid).getOrNull()
                val myName = me?.displayName ?: "Usuario"
                val myPhoto = me?.photoURL ?: "capibara_1"

                val dataReply = DataPostReply(
                    userId = myUid,
                    text = text.trim(),
                    userName = myName,
                    userPhotoUrl = myPhoto
                )

                postRepository.addReply(post.id, dataReply).fold(
                    onSuccess = {
                        val newReply = PostReply(
                            id = System.currentTimeMillis().toString(),
                            author = PostAuthor(
                                id = myUid,
                                displayName = myName,
                                photoUrl = myPhoto,
                                isFriend = false,
                                isVerified = false
                            ),
                            body = text.trim(),
                            createdAtMillis = System.currentTimeMillis(),
                            likeCount = 0,
                            isLikedByMe = false,
                            isFromOriginalAuthor = myUid == post.author.id
                        )

                        _uiState.update {
                            it.copy(
                                replies = it.replies + newReply,
                                isSendingReply = false
                            )
                        }

                        if (myUid != post.author.id) {
                            sendPostReplyNotification(post.author.id, myName, post.id)
                        }

                        onSuccess()
                    },
                    onFailure = { error ->
                        Log.e("PostDetailVM", "Error enviando respuesta: ${error.message}")
                        _uiState.update {
                            it.copy(
                                isSendingReply = false,
                                errorMessage = "No se pudo enviar la respuesta"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("PostDetailVM", "Error: ${e.message}")
                _uiState.update {
                    it.copy(
                        isSendingReply = false,
                        errorMessage = e.message
                    )
                }
            }
        }
    }

    fun toggleReplyLike(replyId: String) {
        val post = _uiState.value.post ?: return
        val oldReplies = _uiState.value.replies

        val updatedReplies = oldReplies.map { reply ->
            if (reply.id == replyId) {
                reply.copy(
                    isLikedByMe = !reply.isLikedByMe,
                    likeCount = if (reply.isLikedByMe) reply.likeCount - 1 else reply.likeCount + 1
                )
            } else reply
        }

        _uiState.update { it.copy(replies = updatedReplies) }

        viewModelScope.launch {
            try {
                postRepository.toggleReplyLike(post.id, replyId, myUid)
            } catch (e: Exception) {
                Log.e("PostDetailVM", "Error toggle reply like: ${e.message}")
                _uiState.update { it.copy(replies = oldReplies) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun toggleLike() {
        val post = _uiState.value.post ?: return
        val updated = post.copy(
            isLikedByMe = !post.isLikedByMe,
            likeCount = post.likeCount + (if (post.isLikedByMe) -1 else 1)
        )
        _uiState.update { it.copy(post = updated) }
        viewModelScope.launch {
            try {
                postRepository.toggleLike(post.id, myUid)
            } catch (e: Exception) {
                Log.e("PostDetailVM", "Error toggle like: ${e.message}")
                _uiState.update { it.copy(post = post) }
            }
        }
    }

    fun toggleSave() {
        val post = _uiState.value.post ?: return
        val updated = post.copy(isSavedByMe = !post.isSavedByMe)
        _uiState.update { it.copy(post = updated) }
        viewModelScope.launch {
            try {
                postRepository.toggleSave(post.id, myUid)
            } catch (e: Exception) {
                Log.e("PostDetailVM", "Error toggle save: ${e.message}")
                _uiState.update { it.copy(post = post) }
            }
        }
    }

    private fun sendPostReplyNotification(postAuthorId: String, responderName: String, postId: String) {
        val data = hashMapOf(
            "postAuthorId" to postAuthorId,
            "responderName" to responderName,
            "postId" to postId
        )
        functions.getHttpsCallable("enviarNotificacionRespuestaPost")
            .call(data)
            .addOnSuccessListener {
                Log.d("PostDetailVM", "Notificación de respuesta enviada")
            }
            .addOnFailureListener { e ->
                Log.e("PostDetailVM", "Error enviando notificación: ${e.message}")
            }
    }
}