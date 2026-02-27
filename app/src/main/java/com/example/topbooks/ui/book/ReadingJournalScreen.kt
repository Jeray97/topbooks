package com.example.topbooks.ui.book

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.topbooks.R
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.model.Journal
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.ui.search.SearchViewModel
import com.example.topbooks.ui.theme.*

// --- COLORES ADAPTADOS ---
val JournalDark = ColorTitleCategoryDetail
val JournalMedium = ColorBackGroundRecommendedSection
val JournalLight = Color.White.copy(alpha = 0.6f)
val JournalGridColor = ColorBackGroundRecommendedSection.copy(alpha = 0.3f)

@Composable
fun ReadingJournalScreen(
    bookId: String,
    initialTitle: String = "",
    initialAuthor: String = "",
    initialImage: String = "",
    initialPages: String = "",
    onBackClick: () -> Unit,
    viewModel: ReadingJournalViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSaving = uiState.isSaving
    val saveSuccess = uiState.saveSuccess
    val existingJournal = uiState.existingJournal
    val isLoadingJournal = uiState.isLoadingJournal

    // --- ESTADOS ---
    var title by remember { mutableStateOf(initialTitle) }
    var author by remember { mutableStateOf(initialAuthor) }
    var pages by remember { mutableStateOf(initialPages) }
    var coverUrl by remember { mutableStateOf(initialImage) }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(false) }

    var mainRating by remember { mutableIntStateOf(0) }
    var rRomance by remember { mutableIntStateOf(0) }
    var rHappy by remember { mutableIntStateOf(0) }
    var rSad by remember { mutableIntStateOf(0) }
    var rSpicy by remember { mutableIntStateOf(0) }

    var genre by remember { mutableStateOf("") }
    var format by remember { mutableStateOf("") }
    var characters by remember { mutableStateOf("") }
    var nicknames by remember { mutableStateOf("") }
    var moments by remember { mutableStateOf("") }

    var showSearchDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var expandedGenre by remember { mutableStateOf(false) }

    val genreOptions = listOf("Romance", "Fantasía", "Terror", "Misterio", "Ciencia Ficción", "Drama", "Aventura", "Historia", "Thriller")

    LaunchedEffect(bookId) { viewModel.loadJournal(bookId) }

    LaunchedEffect(existingJournal) {
        existingJournal?.let { journal ->
            title = journal.title.ifEmpty { initialTitle }
            author = journal.author.ifEmpty { initialAuthor }
            pages = journal.pages.ifEmpty { initialPages }
            coverUrl = journal.bookImageUrl.ifEmpty { initialImage }
            startDate = journal.startDate
            endDate = journal.endDate
            isPublic = journal.isPublic
            mainRating = journal.mainRating
            rRomance = journal.rRomance
            rHappy = journal.rHappy
            rSad = journal.rSad
            rSpicy = journal.rSpicy
            genre = journal.genre
            format = journal.format
            characters = journal.characters
            nicknames = journal.nicknames
            moments = journal.moments
        }
    }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            viewModel.resetSuccessState()
            onBackClick()
        }
    }

    if (showSearchDialog) {
        SearchBookDialog(onDismiss = { showSearchDialog = false }, onBookSelected = { b ->
            title = b.title; author = b.authors.firstOrNull() ?: ""; coverUrl = b.imageUrl
            pages = if (b.pageCount > 0) b.pageCount.toString() else pages
            showSearchDialog = false
        })
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Guardar en mi diario", fontFamily = GuardianCity, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("¿Quieres que esta entrada sea visible en tu perfil para tus amigos?", fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Publicar en mi perfil", modifier = Modifier.weight(1f), fontFamily = CenturyGotic)
                        Switch(
                            checked = isPublic,
                            onCheckedChange = { isPublic = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = ColorArcMediumBrown, checkedTrackColor = ColorArcMediumBrown.copy(alpha = 0.5f))
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val journal = Journal(
                            bookId = bookId,
                            bookTitle = title,
                            bookImageUrl = coverUrl,
                            title = title,
                            author = author,
                            pages = pages,
                            isPublic = isPublic,
                            mainRating = mainRating,
                            rRomance = rRomance,
                            rHappy = rHappy,
                            rSad = rSad,
                            rSpicy = rSpicy,
                            genre = genre,
                            format = format,
                            characters = characters,
                            nicknames = nicknames,
                            moments = moments,
                            quotes = moments, // Fusionamos frases aquí
                            notes = "",
                            startDate = startDate,
                            endDate = endDate
                        )
                        viewModel.saveJournal(journal)
                        showSaveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JournalDark)
                ) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("Cancelar", color = Color.Gray) } },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    Scaffold(
        containerColor = ColorBackGroundGeneral,
        topBar = { TopBar(onBackClick = onBackClick) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 16.dp.toPx()
                for (x in 0..size.width.toInt() step step.toInt()) drawLine(JournalGridColor, start = androidx.compose.ui.geometry.Offset(x.toFloat(), 0f), end = androidx.compose.ui.geometry.Offset(x.toFloat(), size.height))
                for (y in 0..size.height.toInt() step step.toInt()) drawLine(JournalGridColor, start = androidx.compose.ui.geometry.Offset(0f, y.toFloat()), end = androidx.compose.ui.geometry.Offset(size.width, y.toFloat()))
            }

            if (isLoadingJournal) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = JournalDark) }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                            // --- COLUMNA IZQUIERDA ---
                            Column(Modifier.weight(0.35f), Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().aspectRatio(0.65f).border(2.dp, Color.Black, RoundedCornerShape(4.dp)).background(Color.White).clickable { showSearchDialog = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (coverUrl.isNotEmpty()) AsyncImage(model = coverUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    else Icon(Icons.Default.Search, null, tint = Color.LightGray)
                                }

                                Row(Modifier.fillMaxWidth(), Arrangement.Center) {
                                    repeat(5) { k ->
                                        Icon(Icons.Default.Star, null, tint = if (k < mainRating) ColorJournalStar else Color.Gray.copy(alpha = 0.3f), modifier = Modifier.size(18.dp).clickable { mainRating = k + 1 })
                                    }
                                }

                                JournalSectionCard("Clasificación") {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SmallClassificationItem("Romance", rRomance, Icons.Default.Favorite, ColorJournalRomance) { rRomance = it }
                                        SmallClassificationItem("Alegre", rHappy, Icons.Default.Face, ColorJournalHappy) { rHappy = it }
                                        SmallClassificationItem("Triste", rSad, Icons.Default.Opacity, ColorJournalSad) { rSad = it }
                                        SmallClassificationItem("Spicy", rSpicy, Icons.Default.LocalFireDepartment, ColorJournalSpicy) { rSpicy = it }
                                    }
                                }

                                Box {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(JournalMedium, RoundedCornerShape(2.dp))
                                            .clickable { expandedGenre = true }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            if (genre.isNotEmpty()) {
                                                Image(
                                                    painter = painterResource(getGenreIcon(genre)),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = genre,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = JournalDark,
                                                    fontFamily = CenturyGotic,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            } else {
                                                Text(
                                                    text = "Género literario",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = JournalDark,
                                                    fontFamily = CenturyGotic
                                                )
                                            }
                                        }
                                    }

                                    DropdownMenu(expanded = expandedGenre, onDismissRequest = { expandedGenre = false }) {
                                        genreOptions.forEach { opt ->
                                            DropdownMenuItem(
                                                text = { Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Image(painterResource(getGenreIcon(opt)), null, Modifier.size(20.dp))
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(opt, fontFamily = CenturyGotic)
                                                }},
                                                onClick = { genre = opt; expandedGenre = false }
                                            )
                                        }
                                    }
                                }
                            }

                            // --- COLUMNA DERECHA ---
                            Column(Modifier.weight(0.65f), Arrangement.spacedBy(10.dp)) {
                                Box(Modifier.fillMaxWidth().background(JournalMedium, RoundedCornerShape(2.dp)).padding(vertical = 8.dp), Alignment.Center) {
                                    Text("Lectura terminada", fontFamily = GuardianCity, fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                JournalInlineField("Título:", title) { title = it }
                                JournalInlineField("Autor:", author) { author = it }
                                JournalInlineField("No. pág/cap:", pages) { pages = it }

                                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                                    JournalBox(Modifier.weight(1f)) {
                                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Fecha inicio", fontSize = 10.sp, color = JournalDark, fontWeight = FontWeight.Bold)
                                            BasicTextField(value = startDate, onValueChange = { startDate = it }, textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 12.sp, fontFamily = CenturyGotic))
                                        }
                                    }
                                    JournalBox(Modifier.weight(1f)) {
                                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Fecha fin", fontSize = 10.sp, color = JournalDark, fontWeight = FontWeight.Bold)
                                            BasicTextField(value = endDate, onValueChange = { endDate = it }, textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 12.sp, fontFamily = CenturyGotic))
                                        }
                                    }
                                }
                                FormatoLecturaCard(format) { format = it }
                                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                                    JournalLinedTextField("Personajes", characters, { characters = it }, Modifier.weight(1f), minLines = 5)
                                    JournalLinedTextField("Apodos", nicknames, { nicknames = it }, Modifier.weight(1f), minLines = 5)
                                }
                            }
                        }
                    }

                    // 🟢 CÓDIGO NUEVO: Campo de frases expandido al 100% del ancho
                    item {
                        JournalLinedTextField(
                            label = "Frases y momentos favoritos",
                            value = moments,
                            onValueChange = { moments = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 8
                        )
                    }

                    item {
                        Button(
                            onClick = { showSaveDialog = true },
                            modifier = Modifier.fillMaxWidth().height(50.dp).padding(bottom = 16.dp),
                            enabled = !isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = JournalDark),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            else Text("Guardar en mi diario", fontWeight = FontWeight.Bold, color = Color.White, fontFamily = CenturyGotic)
                        }
                    }
                }
            }
        }
    }
}

fun getGenreIcon(genre: String): Int {
    return when (genre.uppercase()) {
        "ROMANCE" -> R.drawable.cat_romance_icon
        "FANTASÍA" -> R.drawable.cat_fantasia_icon
        "TERROR" -> R.drawable.cat_horror_icon
        "MISTERIO" -> R.drawable.cat_misterio_icon
        "CIENCIA FICCIÓN" -> R.drawable.cat_ciencia_ficcion_icon
        "HISTORIA" -> R.drawable.cat_historia_icon
        else -> R.drawable.cat_misterio_icon
    }
}

@Composable
fun SmallClassificationItem(label: String, rating: Int, icon: ImageVector, activeColor: Color, onRatingChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = JournalDark, fontFamily = CenturyGotic)
        Row {
            repeat(5) { k ->
                Icon(icon, null, tint = if (k < rating) activeColor else Color.Gray.copy(alpha = 0.2f), modifier = Modifier.size(14.dp).clickable { onRatingChange(k+1) })
            }
        }
    }
}

@Composable
fun JournalBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.background(JournalLight, RoundedCornerShape(4.dp)).border(1.dp, JournalMedium, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 6.dp)) { content() }
}

@Composable
fun JournalInlineField(label: String, value: String, onValueChange: (String) -> Unit) {
    JournalBox(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = JournalDark, fontFamily = CenturyGotic)
            Spacer(modifier = Modifier.width(4.dp))
            BasicTextField(value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = TextStyle(fontSize = 12.sp, color = Color.Black, fontFamily = CenturyGotic))
        }
    }
}

@Composable
fun JournalSectionCard(t: String, c: @Composable () -> Unit) = Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
    Text(t, modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = JournalDark, fontSize = 10.sp, fontFamily = CenturyGotic)
    JournalBox(Modifier.fillMaxWidth()) { Column(Modifier.fillMaxWidth()) { c() } }
}

@Composable
fun FormatoLecturaCard(selected: String, onSelect: (String) -> Unit) {
    JournalBox(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Text("Formato de lectura", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = JournalDark, fontSize = 11.sp, fontFamily = CenturyGotic)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                FormatOption("Físico", Icons.AutoMirrored.Filled.MenuBook, selected == "Físico") { onSelect("Físico") }
                FormatOption("Digital", Icons.Default.TabletMac, selected == "Digital") { onSelect("Digital") }
                FormatOption("Audio", Icons.Default.Headphones, selected == "Audio") { onSelect("Audio") }
            }
        }
    }
}

@Composable
fun FormatOption(label: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }.padding(2.dp)) {
        Icon(icon, null, tint = if (isSelected) JournalDark else Color.Gray, modifier = Modifier.size(18.dp))
        Text(label, fontSize = 9.sp, color = if (isSelected) JournalDark else Color.Gray, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontFamily = CenturyGotic)
    }
}

@Composable
fun JournalLinedTextField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, minLines: Int = 5) {
    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth().background(JournalMedium, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).padding(4.dp), contentAlignment = Alignment.Center) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = JournalDark, fontFamily = CenturyGotic)
        }
        Box(modifier = Modifier.fillMaxWidth().background(JournalLight, RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)).border(1.dp, JournalMedium, RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val lineHeight = 20.dp.toPx()
                var y = lineHeight
                while (y < size.height) {
                    drawLine(color = JournalMedium, start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1f)
                    y += lineHeight
                }
            }
            BasicTextField(value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp).defaultMinSize(minHeight = (20 * minLines).dp), textStyle = TextStyle(lineHeight = 20.sp, fontSize = 12.sp, color = Color.Black, fontFamily = CenturyGotic))
        }
    }
}

@Composable
fun SearchBookDialog(onDismiss: () -> Unit, onBookSelected: (Book) -> Unit, searchViewModel: SearchViewModel = viewModel()) {
    var localQuery by remember { mutableStateOf("") }
    val searchResults by searchViewModel.searchResults.collectAsState()
    val isLoading by searchViewModel.isLoading.collectAsState()
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(12.dp), color = ColorBackGroundGeneral, modifier = Modifier.fillMaxWidth().height(450.dp)) {
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = localQuery,
                    onValueChange = { localQuery = it; searchViewModel.onQueryChange(it) },
                    label = { Text("Buscar libro...", fontFamily = CenturyGotic, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = JournalDark) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JournalDark, focusedLabelColor = JournalDark, cursorColor = JournalDark)
                )
                Spacer(Modifier.height(12.dp))
                if (isLoading) { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = JournalDark) } }
                else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(searchResults) { book ->
                            Card(modifier = Modifier.fillMaxWidth().clickable { onBookSelected(book) }, colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(model = book.imageUrl, null, Modifier.size(40.dp, 60.dp).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop)
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(book.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = CenturyGotic, fontSize = 14.sp)
                                        Text(book.authors.firstOrNull() ?: "", fontSize = 12.sp, color = Color.Gray, fontFamily = CenturyGotic)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}