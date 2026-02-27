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

// Usamos el mismo modelo unificado de SocialActivity
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

class FriendsActivityViewModel(
    private val feedRepository: SocialFeedRepository = SocialFeedRepositoryImpl(),
    private val communityRepository: CommunityRepository = CommunityRepositoryImpl(),
    private val userRepository: UserRepository = UserRepositoryImpl(),
    private val booksRepository: BooksRepository = BooksRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<Resource<List<FriendActivityItem>>>(Resource.Loading)
    val uiState: StateFlow<Resource<List<FriendActivityItem>>> = _uiState.asStateFlow()

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

                val allActivities = coroutineScope {
                    friendsIds.map { friendId ->
                        async {
                            val user = userRepository.getUserProfile(friendId).getOrNull() ?: return@async emptyList()
                            val friendName = user.displayName.ifEmpty { "Usuario" }
                            val friendPhoto = user.photoURL.ifEmpty { "capibara_1" }

                            val reviews = feedRepository.getUserReviews(friendId).getOrDefault(emptyList())
                            val comments = feedRepository.getUserComments(friendId).getOrDefault(emptyList())
                            val favorites = feedRepository.getUserFavorites(friendId).getOrDefault(emptyList())

                            val items = mutableListOf<FriendActivityItem>()

                            reviews.forEach { r ->
                                val (title, img) = getBookInfo(r.bookId)
                                items.add(FriendActivityItem(type = ActivityType.REVIEW, friendName = friendName, friendPhotoUrl = friendPhoto, bookId = r.bookId, bookTitle = title, bookImageUrl = img, content = r.text, rating = r.rating, timestamp = r.createAt ?: Date()))
                            }
                            comments.forEach { c ->
                                val (title, img) = getBookInfo(c.bookId)
                                items.add(FriendActivityItem(type = ActivityType.COMMENT, friendName = friendName, friendPhotoUrl = friendPhoto, bookId = c.bookId, bookTitle = title, bookImageUrl = img, content = c.text, rating = 0, timestamp = c.createAt ?: Date()))
                            }
                            favorites.forEach { fav ->
                                val bookId = fav["bookId"] as? String ?: return@forEach
                                val timestamp = fav["addedAt"] as? Long ?: 0L
                                val (title, img) = getBookInfo(bookId)
                                items.add(FriendActivityItem(type = ActivityType.FAVORITE, friendName = friendName, friendPhotoUrl = friendPhoto, bookId = bookId, bookTitle = title, bookImageUrl = img, content = "Ha añadido un libro a favoritos", rating = 0, timestamp = Date(timestamp)))
                            }
                            items
                        }
                    }.awaitAll().flatten().sortedByDescending { it.timestamp }
                }

                _uiState.value = Resource.Success(allActivities)
            } catch (e: Exception) {
                _uiState.value = Resource.Error(e)
            }
        }
    }

    private suspend fun getBookInfo(bookId: String): Pair<String, String> {
        val apiBook = booksRepository.getBookDetail(bookId).getOrNull()
        return Pair(apiBook?.title ?: "Libro", apiBook?.imageUrl ?: "")
    }
}