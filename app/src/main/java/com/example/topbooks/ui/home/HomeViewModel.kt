package com.example.topbooks.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.R
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.repository.BooksRepository
import com.example.topbooks.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BooksRepository()

    private val _categoryBooks = MutableStateFlow<Resource<List<Book>>>(Resource.Loading)
    val categoryBooks: StateFlow<Resource<List<Book>>> = _categoryBooks.asStateFlow()

    private val _recommendedBooks = MutableStateFlow<Resource<List<Book>>>(Resource.Loading)
    val recommendedBooks: StateFlow<Resource<List<Book>>> = _recommendedBooks.asStateFlow()

    private val _friendsBooks = MutableStateFlow<Resource<List<Book>>>(Resource.Loading)
    val friendsBooks: StateFlow<Resource<List<Book>>> = _friendsBooks.asStateFlow()

    init {
        loadInitialData()
        fetchRecommendedBooks()
        fetchFriendsBooks()
    }

    private fun loadInitialData() {
        val initialCategory = getApplication<Application>().getString(R.string.cat_fantasia_text)
        fetchBooks("subject:$initialCategory", _categoryBooks)
    }

    fun onCategorySelected(categoryTerm: String) {
        fetchBooks("subject:$categoryTerm", _categoryBooks)
    }

    private fun fetchRecommendedBooks() {
        viewModelScope.launch {
            _recommendedBooks.value = Resource.Loading

            // CAMBIO: Usamos la función específica de OpenLibrary
            val result = repository.getBestRatedModernBooks()

            if (result.isSuccess) {
                _recommendedBooks.value = Resource.Success(result.getOrDefault(emptyList()))
            } else {
                _recommendedBooks.value = Resource.Error(result.exceptionOrNull() ?: Exception("Error"))
            }
        }
    }

    private fun fetchFriendsBooks() {
        // Amigos sigue con Google Books (Aventuras)//TODO
        viewModelScope.launch {
            _friendsBooks.value = Resource.Loading
            val baseQuery = getApplication<Application>().getString(R.string.cat_aventura_text)
            val result = repository.getBooks("subject:$baseQuery")
            if (result.isSuccess) {
                _friendsBooks.value = Resource.Success(result.getOrDefault(emptyList()).take(10))
            } else {
                _friendsBooks.value = Resource.Error(Exception("Error"))
            }
        }
    }

    private fun fetchBooks(query: String, state: MutableStateFlow<Resource<List<Book>>>) {
        viewModelScope.launch {
            state.value = Resource.Loading
            val result = repository.getBooks(query)
            if (result.isSuccess) {
                state.value = Resource.Success(result.getOrDefault(emptyList()))
            } else {
                state.value = Resource.Error(result.exceptionOrNull() ?: Exception("Error"))
            }
        }
    }
}