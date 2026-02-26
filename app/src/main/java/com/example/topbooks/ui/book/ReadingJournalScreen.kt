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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.model.Journal
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.ui.search.SearchViewModel
import com.example.topbooks.ui.theme.*

// --- COLORES ADAPTADOS A TOPBOOKS ---
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
    // --- ESTADOS PRINCIPALES ---
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
    var playlist by remember { mutableStateOf("") }
    var format by remember { mutableStateOf("") }
    var characters by remember { mutableStateOf("") }
    var nicknames by remember { mutableStateOf("") }
    var quotes by remember { mutableStateOf("") }
    var moments by remember { mutableStateOf("") }

    // --- ESTADOS DEL VIEWMODEL ---
    val isSaving by viewModel.isSaving.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val existingJournal by viewModel.existingJournal.collectAsState()
    val isLoadingJournal by viewModel.isLoadingJournal.collectAsState()

    // Buscador
    var showSearchDialog by remember { mutableStateOf(false) }

    // 1. Al abrir la pantalla, pedimos los datos
    LaunchedEffect(bookId) {
        viewModel.loadJournal(bookId)
    }

    // 2. Cuando llegan los datos, los inyectamos en la UI
    LaunchedEffect(existingJournal) {
        existingJournal?.let { journal ->
            Log.d("JournalDebug", "Volcando datos a la UI. Titulo: ${journal.title}")
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
            playlist = journal.playlist
            format = journal.format
            characters = journal.characters
            nicknames = journal.nicknames
            quotes = journal.quotes
            moments = journal.moments
        }
    }

    // 3. Volver atrás al guardar con éxito
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            viewModel.resetSuccessState()
            onBackClick()
        }
    }

    if (showSearchDialog) {
        SearchBookDialog(
            onDismiss = { showSearchDialog = false },
            onBookSelected = { selectedBook ->
                title = selectedBook.title
                author = selectedBook.authors.firstOrNull() ?: ""
                coverUrl = selectedBook.imageUrl
                pages = if (selectedBook.pageCount > 0) selectedBook.pageCount.toString() else pages
                showSearchDialog = false
            }
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = JournalDark)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                            Column(modifier = Modifier.weight(0.35f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.65f)
                                        .border(2.dp, Color.Black, RoundedCornerShape(4.dp))
                                        .background(Color.White)
                                        .clickable { showSearchDialog = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (coverUrl.isNotEmpty()) {
                                        AsyncImage(model = coverUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.Search, null, tint = Color.LightGray)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Buscar\nPortada", fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.Center, lineHeight = 12.sp)
                                        }
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                    for (k in 1..5) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            // 🟢 APLICAMOS COLOR ESTRELLA AQUÍ
                                            tint = if (k <= mainRating) ColorJournalStar else Color.Gray.copy(alpha = 0.3f),
                                            modifier = Modifier.size(20.dp).clickable { mainRating = k }
                                        )
                                    }
                                }

                                JournalPrivacyToggle(isPublic = isPublic, onToggle = { isPublic = it })

                                JournalHeaderTape("Género literario")
                                JournalBox {
                                    BasicTextField(value = genre, onValueChange = { genre = it }, modifier = Modifier.fillMaxWidth().height(24.dp), textStyle = TextStyle(fontSize = 12.sp, color = Color.Black, fontFamily = CenturyGotic, textAlign = TextAlign.Center))
                                }

                                JournalLinedTextField("Playlist", playlist, { playlist = it }, minLines = 6)
                            }

                            Column(modifier = Modifier.weight(0.65f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(modifier = Modifier.fillMaxWidth().background(JournalMedium, RoundedCornerShape(2.dp)).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    Text("Lectura terminada", fontFamily = GuardianCity, fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }

                                JournalInlineField("Título:", title) { title = it }
                                JournalInlineField("Autor:", author) { author = it }
                                JournalInlineField("No. pág/cap:", pages) { pages = it }

                                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                                    JournalBox(Modifier.weight(1f)) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Fecha inicio", fontSize = 10.sp, color = JournalDark, fontFamily = CenturyGotic, fontWeight = FontWeight.Bold)
                                            BasicTextField(value = startDate, onValueChange = { startDate = it }, textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 12.sp, fontFamily = CenturyGotic), modifier = Modifier.fillMaxWidth().padding(top = 2.dp), singleLine = true)
                                        }
                                    }
                                    JournalBox(Modifier.weight(1f)) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Fecha fin", fontSize = 10.sp, color = JournalDark, fontFamily = CenturyGotic, fontWeight = FontWeight.Bold)
                                            BasicTextField(value = endDate, onValueChange = { endDate = it }, textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 12.sp, fontFamily = CenturyGotic), modifier = Modifier.fillMaxWidth().padding(top = 2.dp), singleLine = true)
                                        }
                                    }
                                }

                                FormatoLecturaCard(format) { format = it }

                                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                                    JournalLinedTextField("Personajes", characters, { characters = it }, Modifier.weight(1f), minLines = 5)
                                    JournalLinedTextField("Apodos favoritos", nicknames, { nicknames = it }, Modifier.weight(1f), minLines = 5)
                                }

                                JournalLinedTextField("Frases favoritas", quotes, { quotes = it }, minLines = 5)
                            }
                        }
                    }

                    item {
                        JournalSectionCard("Clasificación") {
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    // 🟢 APLICAMOS COLORES ESPECÍFICOS A CADA FILA
                                    ClassificationRow("Romántico", rRomance, Icons.Default.Favorite, Modifier.weight(1f), activeColor = ColorJournalRomance) { rRomance = it }
                                    ClassificationRow("Alegre", rHappy, Icons.Default.Face, Modifier.weight(1f), activeColor = ColorJournalHappy) { rHappy = it }
                                }
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    ClassificationRow("Triste", rSad, Icons.Default.Opacity, Modifier.weight(1f), activeColor = ColorJournalSad) { rSad = it }
                                    ClassificationRow("Spicy", rSpicy, Icons.Default.LocalFireDepartment, Modifier.weight(1f), activeColor = ColorJournalSpicy) { rSpicy = it }
                                }
                            }
                        }
                    }

                    item { JournalLinedTextField("Momentos favoritos", moments, { moments = it }, minLines = 4) }

                    item {
                        Button(
                            onClick = {
                                val newJournal = Journal(
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
                                    playlist = playlist,
                                    format = format,
                                    characters = characters,
                                    nicknames = nicknames,
                                    quotes = quotes,
                                    moments = moments,
                                    startDate = startDate,
                                    endDate = endDate
                                )
                                viewModel.saveJournal(newJournal)
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
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

// --- BUSCADOR DE LIBROS FLOTANTE ---
@Composable
fun SearchBookDialog(
    onDismiss: () -> Unit,
    onBookSelected: (Book) -> Unit,
    searchViewModel: SearchViewModel = viewModel()
) {
    var localQuery by remember { mutableStateOf("") }
    val searchResults by searchViewModel.searchResults.collectAsState()
    val isLoading by searchViewModel.isLoading.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = ColorBackGroundGeneral,
            modifier = Modifier.fillMaxWidth().height(450.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = localQuery,
                    onValueChange = {
                        localQuery = it
                        searchViewModel.onQueryChange(it)
                    },
                    label = { Text("Buscar título o autor...", fontFamily = CenturyGotic, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = JournalDark) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JournalDark,
                        focusedLabelColor = JournalDark,
                        cursorColor = JournalDark
                    )
                )

                Spacer(Modifier.height(12.dp))

                if (isLoading) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = JournalDark) }
                } else if (searchResults.isEmpty() && localQuery.length >= 3) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No se encontraron libros.", color = Color.Gray, fontFamily = CenturyGotic) }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(searchResults) { book ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { onBookSelected(book) },
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(model = book.imageUrl, contentDescription = null, modifier = Modifier.size(40.dp, 60.dp).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop)
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(book.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = CenturyGotic, fontSize = 14.sp)
                                        Text(book.authors.firstOrNull() ?: "Desconocido", fontSize = 12.sp, color = Color.Gray, maxLines = 1, fontFamily = CenturyGotic)
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

// --- HELPERS EXISTENTES ---
@Composable
fun JournalPrivacyToggle(isPublic: Boolean, onToggle: (Boolean) -> Unit) {
    val backgroundColor by animateColorAsState(if (isPublic) JournalDark else JournalLight)
    val textColor by animateColorAsState(if (isPublic) Color.White else Color.Gray)
    val borderColor by animateColorAsState(if (isPublic) JournalDark else JournalMedium)
    val icon = if (isPublic) Icons.Default.Public else Icons.Default.Lock

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(backgroundColor).border(1.dp, borderColor, RoundedCornerShape(8.dp)).clickable { onToggle(!isPublic) }.padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = if (isPublic) "Público" else "Privado", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor, fontFamily = CenturyGotic)
        }
    }
}

@Composable
fun JournalBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxWidth().background(JournalLight, RoundedCornerShape(4.dp)).border(1.dp, JournalMedium, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 6.dp)) { content() }
}

@Composable
fun JournalInlineField(label: String, value: String, onValueChange: (String) -> Unit) {
    JournalBox {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = JournalDark, fontFamily = CenturyGotic)
            Spacer(modifier = Modifier.width(4.dp))
            BasicTextField(value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = TextStyle(fontSize = 12.sp, color = Color.Black, fontFamily = CenturyGotic))
        }
    }
}

@Composable
fun JournalHeaderTape(label: String) {
    Box(modifier = Modifier.fillMaxWidth().background(JournalMedium, RoundedCornerShape(2.dp)).padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = JournalDark, fontFamily = CenturyGotic)
    }
}

@Composable
fun JournalSectionCard(t: String, c: @Composable () -> Unit) = Column(Modifier.fillMaxWidth()) {
    Text("-$t-", Modifier.fillMaxWidth().padding(bottom = 2.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = JournalDark, fontSize = 12.sp, fontFamily = CenturyGotic)
    JournalBox { Column(Modifier.fillMaxWidth()) { c() } }
}

// 🟢 ACTUALIZADO: Ahora ClassificationRow acepta el color como parámetro (con un valor por defecto)
@Composable
fun ClassificationRow(l: String, r: Int, i: ImageVector, modifier: Modifier = Modifier, activeColor: Color = JournalDark, onR: (Int) -> Unit) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(l, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = JournalDark, fontFamily = CenturyGotic)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.Center) {
            for (k in 1..5) {
                Icon(
                    imageVector = i,
                    contentDescription = null,
                    tint = if (k <= r) activeColor else Color.Gray.copy(alpha = 0.3f), // 🟢 Aplica el color aquí
                    modifier = Modifier.size(24.dp).clickable { onR(k) }.padding(2.dp)
                )
            }
        }
    }
}

@Composable
fun FormatoLecturaCard(selected: String, onSelect: (String) -> Unit) {
    JournalBox {
        Column(Modifier.fillMaxWidth()) {
            Text("Formato de lectura", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = JournalDark, fontSize = 11.sp, fontFamily = CenturyGotic)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                FormatOption("Físico", Icons.Default.MenuBook, selected == "Físico") { onSelect("Físico") }
                FormatOption("Digital", Icons.Default.TabletMac, selected == "Digital") { onSelect("Digital") }
                FormatOption("Audio", Icons.Default.Headphones, selected == "Audio") { onSelect("Audio") }
            }
        }
    }
}

@Composable
fun FormatOption(label: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }.padding(2.dp)) {
        Icon(icon, contentDescription = label, tint = if (isSelected) JournalDark else Color.Gray, modifier = Modifier.size(20.dp))
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