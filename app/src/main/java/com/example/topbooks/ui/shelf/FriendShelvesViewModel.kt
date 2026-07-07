package com.example.topbooks.ui.shelf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Shelf
import com.example.topbooks.data.repository.BooksRepository
import com.example.topbooks.data.repository.ShelfRepository
import com.example.topbooks.data.repository.ShelfRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FriendShelvesState(
    val shelves: List<Shelf> = emptyList(),
    val shelfBooks: Map<String, List<ShelfBook>> = emptyMap(),
    val isLoading: Boolean = true
)

class FriendShelvesViewModel(
    private val shelfRepo: ShelfRepository = ShelfRepositoryImpl(),
    private val booksRepo: BooksRepository = BooksRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendShelvesState())
    val uiState: StateFlow<FriendShelvesState> = _uiState.asStateFlow()

    fun loadFriendShelves(friendId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val shelves = shelfRepo.getPublicShelves(friendId).getOrDefault(emptyList())

            val booksMap = mutableMapOf<String, List<ShelfBook>>()
            for (shelf in shelves) {
                booksMap[shelf.id] = shelf.bookIds.mapNotNull { bookId ->
                    val meta = shelf.bookMetadata[bookId] ?: return@mapNotNull null
                    ShelfBook(
                        id = bookId,
                        title = meta.title,
                        imageUrl = meta.imageUrl,
                        pageCount = meta.pageCount,
                        authors = meta.authors,
                        spineColor = generateSpineColor(bookId)
                    )
                }
            }

            _uiState.update {
                it.copy(
                    shelves = shelves,
                    shelfBooks = booksMap,
                    isLoading = false
                )
            }
        }
    }

    private fun generateSpineColor(bookId: String): Long {
        val hash = bookId.hashCode()
        val colors = listOf(
            0xFF8D5B4CL, 0xFFC89B8CL, 0xFFB9836BL, 0xFFD9AD9AL,
            0xFF6B8E23L, 0xFFCD853FL, 0xFF8B4513L, 0xFFA0522DL,
            0xFFDEB887L, 0xFFD2691EL, 0xFFBC8F8FL, 0xFFF4A460L,
            0xFF6B4226L, 0xFF4A708BL, 0xFF556B2FL, 0xFF8B668BL,
            0xFF7B3F00L, 0xFF2F4F4FL, 0xFF800000L, 0xFF483D8BL
        )
        return colors[Math.abs(hash) % colors.size]
    }
}
