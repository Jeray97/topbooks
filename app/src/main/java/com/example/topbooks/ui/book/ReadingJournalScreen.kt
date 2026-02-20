package com.example.topbooks.ui.book

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.topbooks.data.model.Journal
import com.example.topbooks.ui.components.TopBar
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
    viewModel: ReadingJournalViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    // Estados principales
    var title by remember { mutableStateOf(initialTitle) }
    var author by remember { mutableStateOf(initialAuthor) }
    var pages by remember { mutableStateOf(initialPages) }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    val isSaving by viewModel.isSaving.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()

    // Si se guarda con éxito, volvemos a la pantalla anterior automáticamente
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            Log.d("JournalDebug", "LaunchedEffect: Detectado saveSuccess = true. Volviendo atrás...")
            viewModel.resetSuccessState() // <-- Reset vital para no quedarnos atrapados en un loop
            onBackClick()
        }
    }

    // ESTADO: Privacidad (Por defecto Privado)
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

    Scaffold(
        containerColor = ColorBackGroundGeneral,
        topBar = { TopBar(onBackClick = onBackClick) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Fondo cuadriculado
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 16.dp.toPx()
                for (x in 0..size.width.toInt() step step.toInt()) drawLine(JournalGridColor, start = androidx.compose.ui.geometry.Offset(x.toFloat(), 0f), end = androidx.compose.ui.geometry.Offset(x.toFloat(), size.height))
                for (y in 0..size.height.toInt() step step.toInt()) drawLine(JournalGridColor, start = androidx.compose.ui.geometry.Offset(0f, y.toFloat()), end = androidx.compose.ui.geometry.Offset(size.width, y.toFloat()))
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                        // --- COLUMNA IZQUIERDA ---
                        Column(modifier = Modifier.weight(0.35f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // 1. Portada
                            Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.65f).border(2.dp, Color.Black, RoundedCornerShape(4.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                                if (initialImage.isNotEmpty()) {
                                    AsyncImage(model = initialImage, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.AddPhotoAlternate, null, tint = Color.LightGray)
                                        Text("Portada", fontSize = 10.sp, color = Color.LightGray)
                                    }
                                }
                            }

                            // 2. Estrellas Principales
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                for (k in 1..5) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (k <= mainRating) JournalDark else Color.Gray.copy(alpha = 0.3f),
                                        modifier = Modifier.size(20.dp).clickable { mainRating = k }
                                    )
                                }
                            }

                            // 3. Toggle de Privacidad
                            JournalPrivacyToggle(isPublic = isPublic, onToggle = { isPublic = it })

                            // 4. Género
                            JournalHeaderTape("Género literario")
                            JournalBox {
                                BasicTextField(
                                    value = genre, onValueChange = { genre = it },
                                    modifier = Modifier.fillMaxWidth().height(24.dp),
                                    textStyle = TextStyle(fontSize = 12.sp, color = Color.Black, fontFamily = CenturyGotic, textAlign = TextAlign.Center)
                                )
                            }

                            // 5. Playlist
                            JournalLinedTextField("Playlist", playlist, { playlist = it }, minLines = 6)
                        }

                        // --- COLUMNA DERECHA ---
                        Column(modifier = Modifier.weight(0.65f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Header inclinado estilo cinta
                            Box(
                                modifier = Modifier.fillMaxWidth().graphicsLayer { rotationZ = -2f }.background(JournalMedium, RoundedCornerShape(2.dp)).padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Lectura terminada", fontFamily = GuardianCity, fontSize = 22.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            // Campos de texto internos
                            JournalInlineField("Título:", title) { title = it }
                            JournalInlineField("Autor:", author) { author = it }
                            JournalInlineField("No. pág/cap:", pages) { pages = it }

                            // Fechas
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

                            // Personajes y Apodos
                            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                                JournalLinedTextField("Personajes", characters, { characters = it }, Modifier.weight(1f), minLines = 5)
                                JournalLinedTextField("Apodos favoritos", nicknames, { nicknames = it }, Modifier.weight(1f), minLines = 5)
                            }

                            // Frases Favoritas
                            JournalLinedTextField("Frases favoritas", quotes, { quotes = it }, minLines = 5)
                        }
                    }
                }

                // --- CLASIFICACIÓN (Ancho completo) ---
                item {
                    JournalSectionCard("Clasificación") {
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                ClassificationRow("Romántico", rRomance, Icons.Default.Favorite, Modifier.weight(1f)) { rRomance = it }
                                ClassificationRow("Alegre", rHappy, Icons.Default.Face, Modifier.weight(1f)) { rHappy = it }
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                ClassificationRow("Triste", rSad, Icons.Default.Opacity, Modifier.weight(1f)) { rSad = it }
                                ClassificationRow("Spicy", rSpicy, Icons.Default.LocalFireDepartment, Modifier.weight(1f)) { rSpicy = it }
                            }
                        }
                    }
                }

                item {
                    JournalLinedTextField("Momentos favoritos", moments, { moments = it }, minLines = 4)
                }

                item {
                    Button(
                        onClick = {
                            // AQUÍ ESTÁ EL LOG AÑADIDO
                            Log.d("JournalDebug", "0. Botón 'Guardar en mi diario' pulsado.")

                            val newJournal = Journal(
                                bookId = bookId,
                                bookTitle = initialTitle,
                                bookImageUrl = initialImage,
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
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Guardar en mi diario", fontWeight = FontWeight.Bold, color = Color.White, fontFamily = CenturyGotic)
                        }
                    }
                }
            }
        }
    }
}

// --- COMPONENTE: TOGGLE PRIVACIDAD ---
@Composable
fun JournalPrivacyToggle(isPublic: Boolean, onToggle: (Boolean) -> Unit) {
    val backgroundColor by animateColorAsState(if (isPublic) JournalDark else JournalLight)
    val textColor by animateColorAsState(if (isPublic) Color.White else Color.Gray)
    val borderColor by animateColorAsState(if (isPublic) JournalDark else JournalMedium)
    val icon = if (isPublic) Icons.Default.Public else Icons.Default.Lock

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onToggle(!isPublic) }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isPublic) "Público" else "Privado",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontFamily = CenturyGotic
            )
        }
    }
}

// --- HELPERS ---

@Composable
fun JournalBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(JournalLight, RoundedCornerShape(4.dp))
            .border(1.dp, JournalMedium, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        content()
    }
}

@Composable
fun JournalInlineField(label: String, value: String, onValueChange: (String) -> Unit) {
    JournalBox {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = JournalDark, fontFamily = CenturyGotic)
            Spacer(modifier = Modifier.width(4.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = TextStyle(fontSize = 12.sp, color = Color.Black, fontFamily = CenturyGotic)
            )
        }
    }
}

@Composable
fun JournalHeaderTape(label: String) {
    Box(
        modifier = Modifier.fillMaxWidth().background(JournalMedium, RoundedCornerShape(2.dp)).padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = JournalDark, fontFamily = CenturyGotic)
    }
}

@Composable
fun JournalSectionCard(t: String, c: @Composable () -> Unit) = Column(Modifier.fillMaxWidth()) {
    Text("-$t-", Modifier.fillMaxWidth().padding(bottom = 2.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = JournalDark, fontSize = 12.sp, fontFamily = CenturyGotic)
    JournalBox { Column(Modifier.fillMaxWidth()) { c() } }
}

@Composable
fun ClassificationRow(l: String, r: Int, i: ImageVector, modifier: Modifier = Modifier, onR: (Int) -> Unit) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(l, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = JournalDark, fontFamily = CenturyGotic)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.Center) {
            for (k in 1..5) {
                Icon(
                    imageVector = i,
                    contentDescription = null,
                    tint = if (k <= r) JournalDark else Color.Gray.copy(alpha = 0.3f),
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
        Box(
            modifier = Modifier.fillMaxWidth().background(JournalMedium, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = JournalDark, fontFamily = CenturyGotic)
        }
        Box(
            modifier = Modifier.fillMaxWidth().background(JournalLight, RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)).border(1.dp, JournalMedium, RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val lineHeight = 20.dp.toPx()
                var y = lineHeight
                while (y < size.height) {
                    drawLine(color = JournalMedium, start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1f)
                    y += lineHeight
                }
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp).defaultMinSize(minHeight = (20 * minLines).dp),
                textStyle = TextStyle(lineHeight = 20.sp, fontSize = 12.sp, color = Color.Black, fontFamily = CenturyGotic)
            )
        }
    }
}