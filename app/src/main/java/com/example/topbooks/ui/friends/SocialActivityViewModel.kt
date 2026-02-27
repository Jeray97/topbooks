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
    val replyToName: String? = null,
    val replyToContent: String? = null,
    val commentId: String? = null
)

enum class ActivityType { REVIEW, FAVORITE, COMMENT, REPLY }

class SocialActivityViewModel(
    private val feedRepository: SocialFeedRepository = SocialFeedRepositoryImpl(),
    private val communityRepository: CommunityRepository = CommunityRepositoryImpl(),
    private val userRepository: UserRepository = UserRepositoryImpl(),
    private val booksRepository: BooksRepository = BooksRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<Resource<List<SocialActivityItem>>>(Resource.Loading)
    val uiState: StateFlow<Resource<List<SocialActivityItem>>> = _uiState.asStateFlow()

    init {
        loadActivityFeed()
    }

    fun loadActivityFeed() {
        viewModelScope.launch {
            _uiState.value = Resource.Loading
            try {
                val friendsIds = communityRepository.getMyFriendsIds().getOrDefault(emptySet()).toList()

                if (friendsIds.isEmpty()) {
                    _uiState.value = Resource.Success(emptyList())
                    return@launch
                }

                val activitiesDeferred = friendsIds.map { friendId ->
                    async {
                        val user = userRepository.getUserProfile(friendId).getOrNull() ?: return@async emptyList()
                        val friendName = user.displayName.ifEmpty { "Usuario" }
                        val friendPhoto = user.photoURL.ifEmpty { "capibara_1" }

                        val reviews = feedRepository.getUserReviews(friendId).getOrDefault(emptyList())
                        val comments = feedRepository.getUserComments(friendId).getOrDefault(emptyList())
                        val favorites = feedRepository.getUserFavorites(friendId).getOrDefault(emptyList())

                        val items = mutableListOf<SocialActivityItem>()

                        reviews.forEach { r ->
                            val (bookTitle, bookImage) = getBookInfo(r.bookId)
                            items.add(SocialActivityItem(type = ActivityType.REVIEW, friendName = friendName, friendPhotoUrl = friendPhoto, bookId = r.bookId, bookTitle = bookTitle, bookImageUrl = bookImage, content = r.text, rating = r.rating, timestamp = r.createAt ?: Date()))
                        }

                        comments.forEach { c ->
                            val (bookTitle, bookImage) = getBookInfo(c.bookId)
                            items.add(SocialActivityItem(type = ActivityType.COMMENT, friendName = friendName, friendPhotoUrl = friendPhoto, bookId = c.bookId, bookTitle = bookTitle, bookImageUrl = bookImage, content = c.text, rating = 0, timestamp = c.createAt ?: Date(), commentId = c.commentId))

                            c.replies.forEach { reply ->
                                if (reply.userId == friendId) {
                                    items.add(SocialActivityItem(type = ActivityType.REPLY, friendName = friendName, friendPhotoUrl = friendPhoto, bookId = c.bookId, bookTitle = bookTitle, bookImageUrl = bookImage, content = reply.text, rating = 0, timestamp = Date(reply.timestamp), replyToName = c.userName, replyToContent = c.text, commentId = c.commentId))
                                }
                            }
                        }

                        favorites.forEach { fav ->
                            val bookId = fav["bookId"] as? String ?: return@forEach
                            val timestamp = fav["addedAt"] as? Long ?: 0L
                            val (bookTitle, bookImage) = getBookInfo(bookId)
                            items.add(SocialActivityItem(type = ActivityType.FAVORITE, friendName = friendName, friendPhotoUrl = friendPhoto, bookId = bookId, bookTitle = bookTitle, bookImageUrl = bookImage, content = "Ha añadido un libro a sus favoritos", rating = 0, timestamp = Date(timestamp)))
                        }

                        items
                    }
                }

                val allActivities = activitiesDeferred.awaitAll().flatten().sortedByDescending { it.timestamp }
                _uiState.value = Resource.Success(allActivities)
            } catch (e: Exception) {
                _uiState.value = Resource.Error(e)
            }
        }
    }

    private suspend fun getBookInfo(bookId: String): Pair<String, String> {
        return try {
            val apiBook = booksRepository.getBookDetail(bookId).getOrNull()
            Pair(apiBook?.title ?: "Libro", apiBook?.imageUrl ?: "")
        } catch (e: Exception) { Pair("Libro", "") }
    }
}