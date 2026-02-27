package com.example.topbooks.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Comment
import com.example.topbooks.data.repository.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SimpleUser(val uid: String = "", val name: String = "", val photo: String = "")
data class SimpleBook(val id: String = "", val title: String = "", val imageUrl: String = "")
data class BookmarkUI(val id: String = "", val bookId: String = "", val bookTitle: String = "", val quote: String = "", val chapter: String = "", val page: String = "", val isPublic: Boolean = true)

data class UserListState(
    val friends: List<SimpleUser> = emptyList(),
    val readBooks: List<SimpleBook> = emptyList(),
    val reviews: List<Comment> = emptyList(),
    val bookmarks: List<BookmarkUI> = emptyList(),
    val favorites: List<SimpleBook> = emptyList(),
    val isLoading: Boolean = false
)

class UserListViewModel(
    private val progressRepo: ProgressRepository = ProgressRepositoryImpl(),
    private val feedRepo: SocialFeedRepository = SocialFeedRepositoryImpl(),
    private val communityRepo: CommunityRepository = CommunityRepositoryImpl(),
    private val userRepo: UserRepository = UserRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserListState())
    val uiState: StateFlow<UserListState> = _uiState.asStateFlow()

    private var currentListType: String = ""
    private var currentUserId: String = ""

    fun loadList(listType: String, userId: String) {
        currentListType = listType
        currentUserId = userId

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (listType) {
                "friends" -> fetchFriends(userId)
                "read" -> fetchReadBooks(userId)
                "reviews" -> fetchReviews(userId)
                "comments" -> fetchComments(userId)
                "bookmarks" -> fetchBookmarks(userId)
                "favorites" -> fetchFavorites(userId)
            }
        }
    }

    private suspend fun fetchFriends(userId: String) = coroutineScope {
        val ids = communityRepo.getMyFriendsIds().getOrDefault(emptySet())
        val deferred = ids.map { friendId ->
            async { userRepo.getUserProfile(friendId).getOrNull() }
        }
        val users = deferred.awaitAll().filterNotNull().map { user ->
            SimpleUser(user.uid, user.displayName, user.photoURL)
        }
        _uiState.update { it.copy(friends = users, isLoading = false) }
    }

    private suspend fun fetchReadBooks(userId: String) {
        val books = progressRepo.getReadBooks(userId).getOrDefault(emptyList())
        _uiState.update { it.copy(readBooks = books, isLoading = false) }
    }

    private suspend fun fetchFavorites(userId: String) {
        val covers = userRepo.getFavoriteCovers(userId, 50).getOrDefault(emptyList())
        val ids = userRepo.getFavoriteIds(userId).getOrDefault(emptyList())
        val favs = ids.zip(covers).map { SimpleBook(id = it.first, imageUrl = it.second) }
        _uiState.update { it.copy(favorites = favs, isLoading = false) }
    }

    private suspend fun fetchReviews(userId: String) {
        val comments = feedRepo.getUserComments(userId).getOrDefault(emptyList())
        _uiState.update { it.copy(reviews = comments, isLoading = false) }
    }

    private suspend fun fetchComments(userId: String) = fetchReviews(userId)

    private suspend fun fetchBookmarks(userId: String) {
        val marks = progressRepo.getBookmarks(userId).getOrDefault(emptyList())
        _uiState.update { it.copy(bookmarks = marks, isLoading = false) }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            progressRepo.deleteDocument("comments", commentId)
            loadList(currentListType, currentUserId)
        }
    }

    fun deleteReview(reviewId: String) {
        viewModelScope.launch {
            progressRepo.deleteDocument("reviews", reviewId)
            loadList(currentListType, currentUserId)
        }
    }

    fun removeBookmark(bookId: String) {
        viewModelScope.launch {
            progressRepo.deleteUserSubdocument("bookmarks", bookId)
            loadList(currentListType, currentUserId)
        }
    }

    fun removeFavorite(bookId: String) {
        viewModelScope.launch {
            progressRepo.deleteUserSubdocument("favorites", bookId)
            loadList(currentListType, currentUserId)
        }
    }

    fun removeReadBook(bookId: String) {
        viewModelScope.launch {
            progressRepo.deleteUserSubdocument("read_books", bookId)
            loadList(currentListType, currentUserId)
        }
    }

    // 🟢 NUEVA FUNCIÓN AÑADIDA PARA EDITAR EL MARCADOR
    fun updateBookmark(bookmark: BookmarkUI) {
        viewModelScope.launch {
            try {
                // Empaquetamos solo los datos que pueden ser editados
                val updatedData = mapOf(
                    "page" to bookmark.page,
                    "chapter" to bookmark.chapter,
                    "quote" to bookmark.quote,
                    "isPublic" to bookmark.isPublic
                )

                // Actualizamos en Firebase
                progressRepo.updateUserSubdocument("bookmarks", bookmark.bookId, updatedData)

                // Recargamos la lista para ver el cambio instantáneo
                loadList(currentListType, currentUserId)
            } catch (e: Exception) {
                Log.e("UserListVM", "Error al actualizar marcador: ${e.message}")
            }
        }
    }
}