package com.example.topbooks.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Story
import com.example.topbooks.data.model.StoryType
import com.example.topbooks.data.repository.BooksRepository
import com.example.topbooks.data.repository.StoryRepository
import com.example.topbooks.data.repository.StoryRepositoryImpl
import com.example.topbooks.data.repository.UserRepository
import com.example.topbooks.data.repository.UserRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StoryUiState(
    val isLoading: Boolean = false,
    val stories: List<Story> = emptyList(),
    val currentStoryIndex: Int = 0,
    val isCreating: Boolean = false,
    val createSuccess: Boolean = false,
    val errorMessage: String? = null
)

class StoryViewModel(
    private val storyRepository: StoryRepository = StoryRepositoryImpl(),
    private val booksRepository: BooksRepository = BooksRepository(),
    private val userRepository: UserRepository = UserRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoryUiState())
    val uiState: StateFlow<StoryUiState> = _uiState.asStateFlow()

    private var myUid: String = ""

    init {
        myUid = userRepository.getCurrentUserId() ?: ""
    }

    fun loadStories(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            storyRepository.getStoriesByUser(userId).fold(
                onSuccess = { stories ->
                    _uiState.update { it.copy(isLoading = false, stories = stories, currentStoryIndex = 0) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            )
        }
    }

    fun loadMyStories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            storyRepository.getMyStories().fold(
                onSuccess = { stories ->
                    _uiState.update { it.copy(isLoading = false, stories = stories, currentStoryIndex = 0) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            )
        }
    }

    fun createStory(
        bookId: String,
        type: StoryType,
        text: String,
        backgroundColor: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, errorMessage = null) }
            try {
                val book = booksRepository.getBookDetail(bookId).getOrNull()
                val story = Story(
                    bookId = bookId,
                    bookTitle = book?.title ?: "",
                    bookAuthor = book?.authors?.joinToString() ?: "",
                    bookImageUrl = book?.imageUrl ?: "",
                    type = type.name,
                    text = text,
                    backgroundColor = backgroundColor
                )
                storyRepository.createStory(story).fold(
                    onSuccess = {
                        _uiState.update { it.copy(isCreating = false, createSuccess = true) }
                        onSuccess()
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(isCreating = false, errorMessage = error.message) }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isCreating = false, errorMessage = e.message) }
            }
        }
    }

    fun markAsViewed(storyId: String) {
        viewModelScope.launch {
            storyRepository.markAsViewed(storyId, myUid)
        }
    }

    fun deleteStory(storyId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            storyRepository.deleteStory(storyId).fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(stories = state.stories.filter { it.id != storyId })
                    }
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
            )
        }
    }

    fun nextStory() {
        _uiState.update { state ->
            val nextIndex = state.currentStoryIndex + 1
            if (nextIndex < state.stories.size) state.copy(currentStoryIndex = nextIndex)
            else state
        }
    }

    fun previousStory() {
        _uiState.update { state ->
            val prevIndex = state.currentStoryIndex - 1
            if (prevIndex >= 0) state.copy(currentStoryIndex = prevIndex)
            else state
        }
    }

    fun isLastStory(): Boolean {
        val state = _uiState.value
        return state.currentStoryIndex >= state.stories.size - 1
    }

    fun resetCreateSuccess() {
        _uiState.update { it.copy(createSuccess = false) }
    }
}
