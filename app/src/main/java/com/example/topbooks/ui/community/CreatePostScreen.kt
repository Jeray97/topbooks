package com.example.topbooks.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.topbooks.data.model.Post
import com.example.topbooks.data.model.PostType
import com.example.topbooks.data.repository.BooksRepository
import com.example.topbooks.data.repository.PostRepository
import com.example.topbooks.data.repository.PostRepositoryImpl
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.ui.theme.CenturyGotic
import com.example.topbooks.ui.theme.ColorArcDarkBrown
import com.example.topbooks.ui.theme.ColorArcMediumBrown
import com.example.topbooks.ui.theme.ColorBackGroundGeneral
import com.example.topbooks.ui.theme.ColorTextPrimary
import com.example.topbooks.ui.theme.GuardianCity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SearchFilter {
    GENERAL, TITLE, AUTHOR, ISBN, SERIES
}

data class CreatePostState(
    val isCreating: Boolean = false,
    val createSuccess: Boolean = false,
    val errorMessage: String? = null,
    val searchResults: List<com.example.topbooks.data.model.Book> = emptyList(),
    val isSearching: Boolean = false,
    val initialBook: com.example.topbooks.data.model.Book? = null,
    val isLoadingBook: Boolean = false
)

class CreatePostViewModel(
    private val postRepository: PostRepository = PostRepositoryImpl(),
    private val booksRepository: BooksRepository = BooksRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePostState())
    val uiState: StateFlow<CreatePostState> = _uiState.asStateFlow()

    fun searchBooks(query: String, filter: SearchFilter = SearchFilter.GENERAL) {
        if (query.length < 2) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            val searchQuery = when (filter) {
                SearchFilter.GENERAL -> query
                SearchFilter.TITLE -> "intitle:$query"
                SearchFilter.AUTHOR -> "inauthor:$query"
                SearchFilter.ISBN -> "isbn:$query"
                SearchFilter.SERIES -> "subject:$query"
            }
            booksRepository.getBooks(searchQuery, limit = 5).fold(
                onSuccess = { books ->
                    _uiState.update { it.copy(searchResults = books.take(5), isSearching = false) }
                },
                onFailure = {
                    _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
                }
            )
        }
    }

    fun clearSearch() {
        _uiState.update { it.copy(searchResults = emptyList()) }
    }

    fun loadBookById(bookId: String) {
        if (bookId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingBook = true) }
            booksRepository.getBookDetail(bookId).fold(
                onSuccess = { book ->
                    _uiState.update { it.copy(initialBook = book, isLoadingBook = false) }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoadingBook = false) }
                }
            )
        }
    }

    fun createPost(
        type: PostType,
        bookId: String,
        text: String,
        rating: Int,
        quoteSource: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, errorMessage = null) }
            val post = Post(
                type = type.name,
                bookId = bookId,
                text = text,
                rating = rating,
                quote = if (type == PostType.QUOTE) text else "",
                chapter = quoteSource
            )
            postRepository.createPost(post).fold(
                onSuccess = {
                    _uiState.update { it.copy(isCreating = false, createSuccess = true) }
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isCreating = false, errorMessage = error.message) }
                }
            )
        }
    }
}

@Composable
fun CreatePostScreen(
    onBackClick: () -> Unit,
    onPostCreated: () -> Unit,
    initialBookId: String? = null,
    initialType: String? = null,
    viewModel: CreatePostViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    var selectedType by remember { mutableStateOf(PostType.REVIEW) }
    var selectedBook by remember { mutableStateOf<com.example.topbooks.data.model.Book?>(null) }
    var bookSearchQuery by remember { mutableStateOf("") }
    var postText by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(0) }
    var quoteSource by remember { mutableStateOf("") }
    var searchFilter by remember { mutableStateOf(SearchFilter.GENERAL) }

    LaunchedEffect(initialBookId) {
        if (!initialBookId.isNullOrBlank()) {
            viewModel.loadBookById(initialBookId)
        }
    }

    LaunchedEffect(state.initialBook) {
        state.initialBook?.let { book ->
            selectedBook = book
        }
    }

    LaunchedEffect(initialType) {
        initialType?.let { type ->
            selectedType = when (type.uppercase()) {
                "REVIEW" -> PostType.REVIEW
                "QUOTE" -> PostType.QUOTE
                "FINISHED" -> PostType.FINISHED
                "READING" -> PostType.READING
                else -> PostType.REVIEW
            }
        }
    }

    LaunchedEffect(bookSearchQuery, searchFilter) {
        if (bookSearchQuery.length >= 2) {
            viewModel.searchBooks(bookSearchQuery, searchFilter)
        }
    }

    Scaffold(
        containerColor = ColorBackGroundGeneral,
        topBar = { TopBar(onBackClick = onBackClick) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Compartir con la comunidad",
                fontFamily = GuardianCity,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = ColorArcDarkBrown
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tipo de publicación",
                    fontFamily = CenturyGotic,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = ColorTextPrimary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PostTypeCard(
                        label = "Reseña",
                        icon = Icons.Default.RateReview,
                        isSelected = selectedType == PostType.REVIEW,
                        onClick = { selectedType = PostType.REVIEW },
                        modifier = Modifier.weight(1f)
                    )
                    PostTypeCard(
                        label = "Cita",
                        icon = Icons.Default.FormatQuote,
                        isSelected = selectedType == PostType.QUOTE,
                        onClick = { selectedType = PostType.QUOTE },
                        modifier = Modifier.weight(1f)
                    )
                    PostTypeCard(
                        label = "Terminé",
                        icon = Icons.Default.MenuBook,
                        isSelected = selectedType == PostType.FINISHED,
                        onClick = { selectedType = PostType.FINISHED },
                        modifier = Modifier.weight(1f)
                    )
                    PostTypeCard(
                        label = "Leyendo",
                        icon = Icons.Default.Book,
                        isSelected = selectedType == PostType.READING,
                        onClick = { selectedType = PostType.READING },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (selectedType != PostType.QUOTE || selectedBook != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Libro",
                        fontFamily = CenturyGotic,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = ColorTextPrimary
                    )
                    if (selectedBook == null) {
                        Text(
                            text = "Buscar por",
                            fontFamily = CenturyGotic,
                            fontSize = 12.sp,
                            color = Color(0xFF8D5B4C),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                label = "General",
                                icon = Icons.Default.Search,
                                isSelected = searchFilter == SearchFilter.GENERAL,
                                onClick = { searchFilter = SearchFilter.GENERAL },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                label = "Título",
                                icon = Icons.Default.Title,
                                isSelected = searchFilter == SearchFilter.TITLE,
                                onClick = { searchFilter = SearchFilter.TITLE },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                label = "Autor",
                                icon = Icons.Default.Person,
                                isSelected = searchFilter == SearchFilter.AUTHOR,
                                onClick = { searchFilter = SearchFilter.AUTHOR },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                label = "ISBN",
                                icon = Icons.Default.Numbers,
                                isSelected = searchFilter == SearchFilter.ISBN,
                                onClick = { searchFilter = SearchFilter.ISBN },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                label = "Saga",
                                icon = Icons.Default.CollectionsBookmark,
                                isSelected = searchFilter == SearchFilter.SERIES,
                                onClick = { searchFilter = SearchFilter.SERIES },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = bookSearchQuery,
                            onValueChange = { bookSearchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    when (searchFilter) {
                                        SearchFilter.GENERAL -> "Buscar libro..."
                                        SearchFilter.TITLE -> "Ej: Cien años de soledad"
                                        SearchFilter.AUTHOR -> "Ej: Gabriel García Márquez"
                                        SearchFilter.ISBN -> "Ej: 9780307474728"
                                        SearchFilter.SERIES -> "Ej: Harry Potter"
                                    },
                                    fontFamily = CenturyGotic
                                )
                            },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ColorArcMediumBrown,
                                unfocusedBorderColor = ColorArcMediumBrown.copy(alpha = 0.3f)
                            )
                        )

                        if (state.isSearching) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = ColorArcMediumBrown, modifier = Modifier.size(24.dp))
                            }
                        }

                        state.searchResults.forEach { book ->
                            BookSearchResultItem(
                                book = book,
                                onClick = {
                                    selectedBook = book
                                    bookSearchQuery = ""
                                    viewModel.clearSearch()
                                }
                            )
                        }
                    } else {
                        SelectedBookCard(
                            book = selectedBook!!,
                            onRemove = { selectedBook = null }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = when (selectedType) {
                        PostType.REVIEW -> "Tu reseña"
                        PostType.QUOTE -> "Cita del libro"
                        PostType.FINISHED -> "¿Qué te pareció?"
                        PostType.READING -> "Comentario (opcional)"
                    },
                    fontFamily = CenturyGotic,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = ColorTextPrimary
                )
                OutlinedTextField(
                    value = postText,
                    onValueChange = { postText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            when (selectedType) {
                                PostType.REVIEW -> "Escribe tu opinión sobre el libro..."
                                PostType.QUOTE -> "Escribe la cita que te marcó..."
                                PostType.FINISHED -> "Comparte tu experiencia..."
                                PostType.READING -> "¿Qué estás leyendo?"
                            },
                            fontFamily = CenturyGotic
                        )
                    },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorArcMediumBrown,
                        unfocusedBorderColor = ColorArcMediumBrown.copy(alpha = 0.3f)
                    )
                )
            }

            if (selectedType == PostType.QUOTE) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Autor / Fuente de la cita",
                        fontFamily = CenturyGotic,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = ColorTextPrimary
                    )
                    OutlinedTextField(
                        value = quoteSource,
                        onValueChange = { quoteSource = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Ej: Gabriel García Márquez", fontFamily = CenturyGotic) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorArcMediumBrown,
                            unfocusedBorderColor = ColorArcMediumBrown.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            if (selectedType == PostType.REVIEW || selectedType == PostType.FINISHED) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Calificación",
                        fontFamily = CenturyGotic,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = ColorTextPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(5) { index ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (index < rating) Color(0xFFFFD54F) else Color.LightGray,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { rating = index + 1 }
                            )
                        }
                    }
                }
            }

            state.errorMessage?.let { error ->
                Text(
                    text = error,
                    fontFamily = CenturyGotic,
                    fontSize = 12.sp,
                    color = Color.Red
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.createPost(
                        type = selectedType,
                        bookId = selectedBook?.id ?: "",
                        text = postText,
                        rating = rating,
                        quoteSource = quoteSource
                    ) { }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = postText.isNotBlank() && !state.isCreating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorArcDarkBrown,
                    contentColor = Color.White,
                    disabledContainerColor = Color.LightGray
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (state.isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Publicar",
                        fontFamily = CenturyGotic,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
            }

            if (state.createSuccess) {
                LaunchedEffect(Unit) {
                    onPostCreated()
                }
            }
        }
    }
}

@Composable
private fun PostTypeCard(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) ColorArcMediumBrown else Color.White)
            .border(
                width = 1.dp,
                color = if (isSelected) ColorArcMediumBrown else ColorArcMediumBrown.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else ColorArcMediumBrown,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontFamily = CenturyGotic,
                fontSize = 10.sp,
                color = if (isSelected) Color.White else ColorTextPrimary
            )
        }
    }
}

@Composable
private fun BookSearchResultItem(
    book: com.example.topbooks.data.model.Book,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, ColorArcMediumBrown.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (book.imageUrl.isNotBlank()) {
            AsyncImage(
                model = book.imageUrl,
                contentDescription = book.title,
                modifier = Modifier
                    .size(width = 40.dp, height = 60.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 60.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ColorArcMediumBrown.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Book, contentDescription = null, tint = ColorArcMediumBrown)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book.title,
                fontFamily = CenturyGotic,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = ColorTextPrimary,
                maxLines = 1
            )
            Text(
                text = book.authors.joinToString(),
                fontFamily = CenturyGotic,
                fontSize = 11.sp,
                color = Color(0xFF8D5B4C),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SelectedBookCard(
    book: com.example.topbooks.data.model.Book,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF6E6DD))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (book.imageUrl.isNotBlank()) {
            AsyncImage(
                model = book.imageUrl,
                contentDescription = book.title,
                modifier = Modifier
                    .size(width = 40.dp, height = 60.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 60.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ColorArcMediumBrown.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Book, contentDescription = null, tint = ColorArcMediumBrown)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book.title,
                fontFamily = CenturyGotic,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = ColorTextPrimary,
                maxLines = 1
            )
            Text(
                text = book.authors.joinToString(),
                fontFamily = CenturyGotic,
                fontSize = 11.sp,
                color = Color(0xFF8D5B4C),
                maxLines = 1
            )
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, contentDescription = "Quitar libro", tint = ColorArcMediumBrown)
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (isSelected) ColorArcMediumBrown else Color.White)
            .border(
                width = 1.dp,
                color = if (isSelected) ColorArcMediumBrown else ColorArcMediumBrown.copy(alpha = 0.3f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else ColorArcMediumBrown,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontFamily = CenturyGotic,
                fontSize = 10.sp,
                color = if (isSelected) Color.White else ColorTextPrimary,
                maxLines = 1
            )
        }
    }
}
