package com.example.topbooks.ui.home

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

class RecommendedSectionViewModel(
    private val repository: BooksRepository = BooksRepository(),
    private val communityRepo: CommunityRepository = CommunityRepositoryImpl(),
    private val userRepo: UserRepository = UserRepositoryImpl()
) : ViewModel() {

    private val _booksState = MutableStateFlow<Resource<List<Book>>>(Resource.Loading)
    val booksState: StateFlow<Resource<List<Book>>> = _booksState.asStateFlow()

    private var currentPage = 1
    private var currentType = ""
    private var currentGenre = ""
    private var isLastPage = false
    private var isLoadingMore = false
    private val loadedBooks = mutableListOf<Book>()

    fun loadSectionData(type: String, genre: String) {
        if (currentType != type || currentGenre != genre) {
            currentType = type
            currentGenre = genre
            currentPage = 1
            loadedBooks.clear()
            isLastPage = false
            _booksState.value = Resource.Loading
            fetchData()
        }
    }

    fun loadMore() {
        if (isLoadingMore || isLastPage) return
        currentPage++
        fetchData()
    }

    private fun fetchData() {
        isLoadingMore = true
        viewModelScope.launch {
            when (currentType) {
                "popular" -> fetchFromApi("subject:fiction", "newest", true)
                "tastes" -> fetchFromApi("subject:$currentGenre", "relevance", true)
                "friends" -> fetchFriendsFavorites()
            }
        }
    }

    private suspend fun fetchFromApi(query: String, orderBy: String, filterModern: Boolean) {
        val result = repository.getBooks(query, orderBy, filterModern, currentPage, 20)
        if (result.isSuccess) {
            val newBooks = result.getOrDefault(emptyList())
            if (newBooks.isEmpty()) isLastPage = true
            else {
                loadedBooks.addAll(newBooks)
                _booksState.value = Resource.Success(loadedBooks.toList())
            }
        } else if (currentPage == 1) {
            _booksState.value = Resource.Error(result.exceptionOrNull() ?: Exception("Error"))
        }
        isLoadingMore = false
    }

    private suspend fun fetchFriendsFavorites() {
        try {
            val friendIds = communityRepo.getMyFriendsIds().getOrDefault(emptySet())
            if (friendIds.isEmpty()) {
                _booksState.value = Resource.Success(emptyList())
                isLoadingMore = false
                return
            }

            val allBookIds = coroutineScope {
                friendIds.map { friendId ->
                    async { userRepo.getFavoriteIds(friendId).getOrDefault(emptyList()).take(5) }
                }.awaitAll().flatten().distinct().take(30)
            }

            val booksDetails = coroutineScope {
                allBookIds.map { bookId -> async { repository.getBookDetail(bookId).getOrNull() } }
                    .awaitAll().filterNotNull()
            }

            loadedBooks.clear()
            loadedBooks.addAll(booksDetails.sortedByDescending { Regex("\\d{4}").find(it.lanzamiento)?.value?.toIntOrNull() ?: 0 })

            _booksState.value = Resource.Success(loadedBooks.toList())
            isLastPage = true
        } catch (e: Exception) {
            _booksState.value = Resource.Error(e)
        } finally {
            isLoadingMore = false
        }
    }
}