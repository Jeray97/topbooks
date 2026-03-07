package com.example.topbooks.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.repository.*
import com.example.topbooks.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

    private var isDataLoaded = false

    fun loadData(categoryQuery: String, fallbackRecommendedQuery: String) {
        if (isDataLoaded) return
        isDataLoaded = true

        fetchBooks(categoryQuery, _categoryBooks, filterModern = true)
        fetchPersonalizedRecommendations(fallbackRecommendedQuery)
        fetchFriendsFavorites()
    }

    private fun fetchPersonalizedRecommendations(fallbackQuery: String) {

        viewModelScope.launch {

            _recommendedBooks.value = Resource.Loading

            try {

                val uid = FirebaseAuth.getInstance().currentUser?.uid
                val db = FirebaseFirestore.getInstance()

                val personalizedBooks = mutableListOf<Book>()

                if (uid != null) {

                    val userDoc = db.collection("users")
                        .document(uid)
                        .get()
                        .await()

                    val favoriteGenres =
                        userDoc.get("favoriteGenres") as? List<String> ?: emptyList()

                    val favoriteBookIds =
                        userDoc.get("favoriteBooks") as? List<String> ?: emptyList()

                    // 1️⃣ Libros similares a favoritos
                    val booksFromFavorites = coroutineScope {

                        favoriteBookIds.take(3).map { bookId ->
                            async {

                                val book = booksRepository
                                    .getBookDetail(bookId)
                                    .getOrNull()

                                book?.categories?.firstOrNull()?.let { category ->

                                    booksRepository
                                        .searchHybrid("subject:$category")
                                        .getOrNull()
                                        ?.take(4)
                                        ?: emptyList()

                                } ?: emptyList()

                            }
                        }

                    }.awaitAll().flatten()

                    personalizedBooks.addAll(booksFromFavorites)

                    // 2️⃣ Libros por géneros favoritos
                    if (favoriteGenres.isNotEmpty()) {

                        val genreBooks = coroutineScope {

                            favoriteGenres.take(3).map { genre ->
                                async {
                                    booksRepository
                                        .searchHybrid("subject:$genre")
                                        .getOrNull()
                                        ?.take(4)
                                        ?: emptyList()
                                }
                            }

                        }.awaitAll().flatten()

                        personalizedBooks.addAll(genreBooks)
                    }
                }

                // 3️⃣ Libros populares de Firebase
                if (personalizedBooks.size < 10) {

                    val popularBooks = booksRepository.getPopularBooks(10)
                    personalizedBooks.addAll(popularBooks)

                }

                // 4️⃣ Fallback
                if (personalizedBooks.isEmpty()) {

                    val fallback = booksRepository
                        .searchHybrid(fallbackQuery)
                        .getOrNull()
                        ?: emptyList()

                    personalizedBooks.addAll(fallback)

                }

                val finalBooks = personalizedBooks
                    .distinctBy { it.id }
                    .shuffled()
                    .take(10)

                _recommendedBooks.value = Resource.Success(finalBooks)

            } catch (e: Exception) {

                Log.e("HomeVM", "Error recomendaciones: ${e.message}")
                _recommendedBooks.value = Resource.Error(e)

            }
        }
    }

    private fun fetchFriendsFavorites() {

        viewModelScope.launch {

            try {

                _friendsBooks.value = Resource.Loading

                val friendIds =
                    communityRepository.getMyFriendsIds().getOrDefault(emptySet())

                if (friendIds.isEmpty()) {
                    _friendsBooks.value = Resource.Success(emptyList())
                    return@launch
                }

                val recommendations = coroutineScope {

                    friendIds.map { friendId ->

                        async {

                            val user =
                                userRepository.getUserProfile(friendId).getOrNull()

                            val favIds =
                                userRepository.getFavoriteIds(friendId)
                                    .getOrDefault(emptyList())

                            favIds.take(2).mapNotNull { bookId ->

                                val book =
                                    booksRepository.getBookDetail(bookId).getOrNull()

                                if (book != null && user != null) {

                                    FriendBookRecommendation(
                                        book,
                                        user.displayName,
                                        user.photoURL
                                    )

                                } else null
                            }

                        }

                    }.awaitAll().flatten()

                }

                _friendsBooks.value =
                    Resource.Success(recommendations.shuffled().take(10))

            } catch (e: Exception) {

                Log.e("HomeVM", "Error cargando favoritos amigos: ${e.message}")
                _friendsBooks.value = Resource.Error(e)

            }
        }
    }

    private fun fetchBooks(
        query: String,
        state: MutableStateFlow<Resource<List<Book>>>,
        filterModern: Boolean = false
    ) {

        viewModelScope.launch {

            state.value = Resource.Loading

            val result = booksRepository.getBooks(
                query = query,
                filterModern = filterModern
            )

            if (result.isSuccess) {

                state.value = Resource.Success(
                    result.getOrDefault(emptyList())
                )

            } else {

                state.value = Resource.Error(
                    result.exceptionOrNull() ?: Exception("Error")
                )

            }
        }
    }
}