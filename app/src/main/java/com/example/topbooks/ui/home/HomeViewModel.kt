package com.example.topbooks.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.repository.*
import com.example.topbooks.utils.Resource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FriendBookRecommendation(
    val book: Book,
    val friendName: String,
    val friendPhotoUrl: String
)

class HomeViewModel(
    private val booksRepository: BooksRepository = BooksRepository(),
    private val communityRepository: CommunityRepository = CommunityRepositoryImpl(),
    private val userRepository: UserRepository = UserRepositoryImpl()
) : ViewModel() {

    private val _categoryBooks = MutableStateFlow<Resource<List<Book>>>(Resource.Loading)
    val categoryBooks: StateFlow<Resource<List<Book>>> = _categoryBooks.asStateFlow()

    private val _recommendedBooks = MutableStateFlow<Resource<List<Book>>>(Resource.Loading)
    val recommendedBooks: StateFlow<Resource<List<Book>>> = _recommendedBooks.asStateFlow()

    private val _friendsBooks = MutableStateFlow<Resource<List<FriendBookRecommendation>>>(Resource.Loading)
    val friendsBooks: StateFlow<Resource<List<FriendBookRecommendation>>> = _friendsBooks.asStateFlow()

    // Control para evitar recargas al girar la pantalla o navegar
    private var isDataLoaded = false

    // 🔥 Recibe los textos en el idioma del teléfono
    fun loadData(categoryQuery: String, recommendedQuery: String) {
        if (isDataLoaded) return
        isDataLoaded = true
        fetchBooks(categoryQuery, _categoryBooks, filterModern = true)
        fetchBooks(recommendedQuery, _recommendedBooks, filterModern = true)
        fetchFriendsFavorites()
    }

    private fun fetchFriendsFavorites() {
        viewModelScope.launch {
            try {
                _friendsBooks.value = Resource.Loading
                val friendIds = communityRepository.getMyFriendsIds().getOrDefault(emptySet())
                if (friendIds.isEmpty()) {
                    _friendsBooks.value = Resource.Success(emptyList())
                    return@launch
                }

                val recommendations = coroutineScope {
                    friendIds.map { friendId ->
                        async {
                            val user = userRepository.getUserProfile(friendId).getOrNull()
                            val favIds = userRepository.getFavoriteIds(friendId).getOrDefault(emptyList())

                            favIds.take(2).mapNotNull { bookId ->
                                val book = booksRepository.getBookDetail(bookId).getOrNull()
                                if (book != null && user != null) {
                                    FriendBookRecommendation(book, user.displayName, user.photoURL)
                                } else null
                            }
                        }
                    }.awaitAll().flatten()
                }
                _friendsBooks.value = Resource.Success(recommendations.shuffled().take(10))
            } catch (e: Exception) {
                Log.e("HomeVM", "Error cargando favoritos amigos: ${e.message}")
                _friendsBooks.value = Resource.Error(e)
            }
        }
    }

    private fun fetchBooks(query: String, state: MutableStateFlow<Resource<List<Book>>>, filterModern: Boolean = false) {
        viewModelScope.launch {
            state.value = Resource.Loading
            val result = booksRepository.getBooks(query = query, filterModern = filterModern)
            if (result.isSuccess) {
                state.value = Resource.Success(result.getOrDefault(emptyList()))
            } else {
                state.value = Resource.Error(result.exceptionOrNull() ?: Exception("Error"))
            }
        }
    }
}