package com.example.topbooks.ui.community

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Post as DataPost
import com.example.topbooks.data.repository.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CommunityViewModel(
    private val postRepository: PostRepository = PostRepositoryImpl(),
    private val storyRepository: StoryRepository = StoryRepositoryImpl(),
    private val communityRepository: CommunityRepository = CommunityRepositoryImpl(),
    private val userRepository: UserRepository = UserRepositoryImpl(),
    private val booksRepository: BooksRepository = BooksRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunityFeedUiState())
    val uiState: StateFlow<CommunityFeedUiState> = _uiState.asStateFlow()

    private var myUid: String = ""
    private var friendIds: Set<String> = emptySet()
    private var favoriteGenres: List<String> = emptyList()

    private val enrichedPostsCache = mutableMapOf<String, Post>()
    private val userCache = mutableMapOf<String, com.example.topbooks.data.model.User>()
    private val bookCache = mutableMapOf<String, com.example.topbooks.data.model.Book>()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            myUid = userRepository.getCurrentUserId() ?: ""
            friendIds = communityRepository.getMyFriendsIds().getOrDefault(emptySet())
            val user = userRepository.getUserProfile(myUid).getOrNull()
            favoriteGenres = user?.favoriteGenres ?: emptyList()
            loadStories()
            selectTab(FeedTab.FRIENDS)
        }
    }

    private fun loadStories() {
        viewModelScope.launch {
            try {
                val stories = storyRepository.getFriendsStories(friendIds.toList()).getOrDefault(emptyList())
                val enrichedStories = stories.map { story ->
                    viewModelScope.async {
                        val user = getCachedUser(story.userId)
                        val enrichedStory = if (story.bookId.isNotBlank()) {
                            val book = getCachedBook(story.bookId)
                            story.copy(
                                userName = user?.displayName ?: story.userName,
                                userPhotoUrl = user?.photoURL ?: story.userPhotoUrl,
                                bookTitle = book?.title ?: story.bookTitle,
                                bookAuthor = book?.authors?.joinToString() ?: story.bookAuthor,
                                bookImageUrl = book?.imageUrl ?: story.bookImageUrl
                            )
                        } else {
                            story.copy(
                                userName = user?.displayName ?: story.userName,
                                userPhotoUrl = user?.photoURL ?: story.userPhotoUrl
                            )
                        }
                        enrichedStory.toUiStoryItem(user, isFriend = true)
                    }
                }.awaitAll()
                _uiState.update { it.copy(stories = enrichedStories) }
            } catch (e: Exception) {
                Log.e("CommunityVM", "Error cargando stories: ${e.message}")
            }
        }
    }

    private suspend fun getCachedUser(userId: String): com.example.topbooks.data.model.User? {
        return userCache.getOrPut(userId) {
            userRepository.getUserProfile(userId).getOrNull() ?: return null
        }
    }

    private suspend fun getCachedBook(bookId: String): com.example.topbooks.data.model.Book? {
        return bookCache.getOrPut(bookId) {
            booksRepository.getBookDetail(bookId).getOrNull() ?: return null
        }
    }

    fun selectTab(tab: FeedTab) {
        _uiState.update { it.copy(activeTab = tab, isLoading = true) }
        viewModelScope.launch {
            try {
                val dataPosts = when (tab) {
                    FeedTab.COMMUNITY -> postRepository.getAlgorithmicFeed(myUid, friendIds.toList(), favoriteGenres, limit = 30).getOrDefault(emptyList())
                    FeedTab.FRIENDS -> postRepository.getFriendsFeed(friendIds.toList(), limit = 30).getOrDefault(emptyList())
                    FeedTab.TOP -> postRepository.getTopFeed(limit = 20).getOrDefault(emptyList())
                }
                val enrichedPosts = enrichPosts(dataPosts)
                _uiState.update {
                    it.copy(
                        posts = enrichedPosts,
                        isLoading = false,
                        newPostsCountToday = enrichedPosts.count { post ->
                            val today = System.currentTimeMillis()
                            val dayMs = 86_400_000L
                            today - post.createdAtMillis < dayMs
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("CommunityVM", "Error cargando feed: ${e.message}")
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun refresh() {
        selectTab(_uiState.value.activeTab)
    }

    private suspend fun enrichPosts(posts: List<DataPost>): List<Post> {
        return posts.map { dataPost ->
            viewModelScope.async {
                val cachedUiPost = enrichedPostsCache[dataPost.id]
                if (cachedUiPost != null) {
                    val isLikedByMe = myUid in dataPost.likedBy
                    val isSavedByMe = myUid in dataPost.savedBy
                    cachedUiPost.copy(
                        likeCount = dataPost.likes,
                        isLikedByMe = isLikedByMe,
                        isSavedByMe = isSavedByMe,
                        commentCount = dataPost.replyCount
                    )
                } else {
                    val user = getCachedUser(dataPost.userId)
                    val enrichedDataPost = if (dataPost.bookId.isNotBlank()) {
                        val book = getCachedBook(dataPost.bookId)
                        dataPost.copy(
                            userName = user?.displayName ?: dataPost.userName,
                            userPhotoUrl = user?.photoURL ?: dataPost.userPhotoUrl,
                            bookTitle = book?.title ?: dataPost.bookTitle,
                            bookAuthor = book?.authors?.joinToString() ?: dataPost.bookAuthor,
                            bookImageUrl = book?.imageUrl ?: dataPost.bookImageUrl
                        )
                    } else {
                        dataPost.copy(
                            userName = user?.displayName ?: dataPost.userName,
                            userPhotoUrl = user?.photoURL ?: dataPost.userPhotoUrl
                        )
                    }
                    val isFriend = dataPost.userId in friendIds
                    val isLikedByMe = myUid in dataPost.likedBy
                    val isSavedByMe = myUid in dataPost.savedBy
                    val uiPost = enrichedDataPost.toUiPost(user, isFriend, isLikedByMe, isSavedByMe)
                    enrichedPostsCache[dataPost.id] = uiPost
                    uiPost
                }
            }
        }.awaitAll()
    }

    fun toggleLike(post: Post) {
        val updated = post.copy(
            isLikedByMe = !post.isLikedByMe,
            likeCount = post.likeCount + (if (post.isLikedByMe) -1 else 1)
        )
        _uiState.update { state ->
            state.copy(posts = state.posts.map { if (it.id == post.id) updated else it })
        }
        viewModelScope.launch {
            try {
                postRepository.toggleLike(post.id, myUid)
            } catch (e: Exception) {
                Log.e("CommunityVM", "Error toggle like: ${e.message}")
                _uiState.update { state ->
                    state.copy(posts = state.posts.map { if (it.id == post.id) post else it })
                }
            }
        }
    }

    fun toggleSave(post: Post) {
        val updated = post.copy(isSavedByMe = !post.isSavedByMe)
        _uiState.update { state ->
            state.copy(posts = state.posts.map { if (it.id == post.id) updated else it })
        }
        viewModelScope.launch {
            try {
                postRepository.toggleSave(post.id, myUid)
            } catch (e: Exception) {
                Log.e("CommunityVM", "Error toggle save: ${e.message}")
                _uiState.update { state ->
                    state.copy(posts = state.posts.map { if (it.id == post.id) post else it })
                }
            }
        }
    }
}