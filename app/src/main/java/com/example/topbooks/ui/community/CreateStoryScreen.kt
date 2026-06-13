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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.model.StoryType
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.ui.search.SearchViewModel
import com.example.topbooks.ui.theme.ColorArcDarkBrown
import com.example.topbooks.ui.theme.ColorArcMediumBrown
import com.example.topbooks.ui.theme.ColorBackGroundGeneral
import com.example.topbooks.ui.theme.ColorTextPrimary
import com.example.topbooks.ui.theme.GuardianCity
import com.example.topbooks.ui.theme.CenturyGotic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStoryScreen(
    onBackClick: () -> Unit,
    onStoryCreated: () -> Unit,
    viewModel: StoryViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedBook by remember { mutableStateOf<Book?>(null) }
    var selectedType by remember { mutableStateOf(StoryType.BOOK_COVER) }
    var storyText by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#F6E6DD") }
    var showBookSearch by remember { mutableStateOf(false) }

    LaunchedEffect(state.createSuccess) {
        if (state.createSuccess) {
            viewModel.resetCreateSuccess()
            onStoryCreated()
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
                text = "Compartir historia",
                fontFamily = GuardianCity,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = ColorArcDarkBrown
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tipo de historia",
                    fontFamily = CenturyGotic,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = ColorTextPrimary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StoryTypeCard(
                        type = StoryType.BOOK_COVER,
                        icon = Icons.Default.Book,
                        label = "Portada",
                        isSelected = selectedType == StoryType.BOOK_COVER,
                        onClick = { selectedType = StoryType.BOOK_COVER },
                        modifier = Modifier.weight(1f)
                    )
                    StoryTypeCard(
                        type = StoryType.QUOTE,
                        icon = Icons.Default.FormatQuote,
                        label = "Cita",
                        isSelected = selectedType == StoryType.QUOTE,
                        onClick = { selectedType = StoryType.QUOTE },
                        modifier = Modifier.weight(1f)
                    )
                    StoryTypeCard(
                        type = StoryType.READING_STATUS,
                        icon = Icons.Default.MenuBook,
                        label = "Leyendo",
                        isSelected = selectedType == StoryType.READING_STATUS,
                        onClick = { selectedType = StoryType.READING_STATUS },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Libro",
                    fontFamily = CenturyGotic,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = ColorTextPrimary
                )
                Button(
                    onClick = { showBookSearch = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedBook != null) ColorArcMediumBrown else Color.White,
                        contentColor = if (selectedBook != null) Color.White else ColorArcMediumBrown
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = selectedBook?.title ?: "Buscar libro...",
                        fontFamily = CenturyGotic,
                        fontSize = 14.sp
                    )
                }
            }

            if (selectedType == StoryType.QUOTE || selectedType == StoryType.READING_STATUS) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (selectedType == StoryType.QUOTE) "Cita favorita" else "Estado de lectura",
                        fontFamily = CenturyGotic,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = ColorTextPrimary
                    )
                    OutlinedTextField(
                        value = storyText,
                        onValueChange = { storyText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = if (selectedType == StoryType.QUOTE)
                                    "Escribe la cita que te marcó..."
                                else
                                    "¿Qué estás leyendo ahora?",
                                fontFamily = CenturyGotic
                            )
                        },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorArcMediumBrown,
                            unfocusedBorderColor = ColorArcMediumBrown.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Color de fondo",
                    fontFamily = CenturyGotic,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = ColorTextPrimary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(STORY_COLORS) { color ->
                        ColorPickerItem(
                            color = color,
                            isSelected = selectedColor == color,
                            onClick = { selectedColor = color }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    selectedBook?.let { book ->
                        viewModel.createStory(
                            bookId = book.id,
                            type = selectedType,
                            text = storyText,
                            backgroundColor = selectedColor,
                            onSuccess = { }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = selectedBook != null && !state.isCreating,
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
                        text = "Publicar historia",
                        fontFamily = CenturyGotic,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }

    if (showBookSearch) {
        SearchBookDialog(
            onDismiss = { showBookSearch = false },
            onBookSelected = { book ->
                selectedBook = book
                showBookSearch = false
            }
        )
    }
}

@Composable
private fun StoryTypeCard(
    type: StoryType,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(80.dp)
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
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontFamily = CenturyGotic,
                fontSize = 11.sp,
                color = if (isSelected) Color.White else ColorTextPrimary
            )
        }
    }
}

@Composable
private fun ColorPickerItem(
    color: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(parseColor(color))
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) ColorArcDarkBrown else Color.LightGray,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

private fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color.LightGray
    }
}

private val STORY_COLORS = listOf(
    "#F6E6DD",
    "#FFE5EE",
    "#E3F0D8",
    "#E5EDFA",
    "#FFF4E6",
    "#F0E6FF"
)

@Composable
private fun SearchBookDialog(
    onDismiss: () -> Unit,
    onBookSelected: (Book) -> Unit,
    searchViewModel: SearchViewModel = viewModel()
) {
    val searchResults by searchViewModel.searchResults.collectAsState()
    val isLoading by searchViewModel.isLoading.collectAsState()
    var query by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Buscar libro",
                fontFamily = GuardianCity,
                fontWeight = FontWeight.Bold,
                color = ColorArcDarkBrown
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        searchViewModel.onQueryChange(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Título o autor...", fontFamily = CenturyGotic) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorArcMediumBrown
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ColorArcMediumBrown)
                    }
                } else {
                    searchResults.take(5).forEach { book ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onBookSelected(book) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = book.title,
                                    fontFamily = CenturyGotic,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = ColorTextPrimary
                                )
                                Text(
                                    text = book.authors.joinToString(),
                                    fontFamily = CenturyGotic,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar", fontFamily = CenturyGotic)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}
