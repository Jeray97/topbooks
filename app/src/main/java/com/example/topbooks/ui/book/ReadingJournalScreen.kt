package com.example.topbooks.ui.book

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.ui.theme.*

// TODO hacer iconos y agregarlos
val JournalPurpleLight = Color(0xFFF3E5F5)
val JournalPurpleMedium = Color(0xFFE1BEE7)
val JournalPurpleDark = Color(0xFF9575CD)
val JournalGridColor = Color(0xFFE0E0E0)

@Composable
fun ReadingJournalScreen(
    bookId: String,
    onBackClick: () -> Unit
) {
    // Estados para el formulario
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var pages by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var formatSelected by remember { mutableStateOf("Físico") }

    // Clasificaciones (1 a 5)
    var ratingRomance by remember { mutableIntStateOf(0) }
    var ratingHappy by remember { mutableIntStateOf(0) }
    var ratingSad by remember { mutableIntStateOf(0) }
    var ratingSpicy by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = Color.White,
        topBar = { TopBar(onBackClick = onBackClick) }
    ) { padding ->
        // Contenedor con fondo cuadriculado
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 20.dp.toPx()
                for (x in 0..size.width.toInt() step step.toInt()) {
                    drawLine(JournalGridColor, start = androidx.compose.ui.geometry.Offset(x.toFloat(), 0f), end = androidx.compose.ui.geometry.Offset(x.toFloat(), size.height), strokeWidth = 1f)
                }
                for (y in 0..size.height.toInt() step step.toInt()) {
                    drawLine(JournalGridColor, start = androidx.compose.ui.geometry.Offset(0f, y.toFloat()), end = androidx.compose.ui.geometry.Offset(size.width, y.toFloat()), strokeWidth = 1f)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    JournalHeader("Lectura terminada")
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Hueco para la portada
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(180.dp)
                                .border(2.dp, Color.Black, RoundedCornerShape(4.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Call, null, tint = Color.LightGray)
                                Text("Añadir portada", fontSize = 10.sp, color = Color.LightGray)
                            }
                        }

                        // Info principal
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            JournalTextField("Título:", title) { title = it }
                            JournalTextField("Autor:", author) { author = it }
                            JournalTextField("Páginas/Cap:", pages) { pages = it }
                        }
                    }
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        JournalDateField("Fecha inicio", startDate, Modifier.weight(1f)) { startDate = it }
                        JournalDateField("Fecha fin", endDate, Modifier.weight(1f)) { endDate = it }
                    }
                }

                item {
                    JournalSectionCard("Clasificación") {
                        ClassificationRow("Romántico:", ratingRomance, Icons.Default.Favorite) { ratingRomance = it }
                        ClassificationRow("Alegre:", ratingHappy, Icons.Default.Face) { ratingHappy = it }
                        ClassificationRow("Triste:", ratingSad, Icons.Default.Call) { ratingSad = it }
                        ClassificationRow("Spicy:", ratingSpicy, Icons.Default.ThumbUp) { ratingSpicy = it }
                    }
                }

                item {
                    JournalSectionCard("Formato de lectura") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            FormatItem("Físico", Icons.Default.Call, formatSelected == "Físico") { formatSelected = "Físico" }
                            FormatItem("Digital", Icons.Default.Call, formatSelected == "Digital") { formatSelected = "Digital" }
                            FormatItem("Audio", Icons.Default.Call, formatSelected == "Audio") { formatSelected = "Audio" }
                        }
                    }
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        JournalSmallBox("Personajes", Modifier.weight(1f))
                        JournalSmallBox("Apodos", Modifier.weight(1f))
                    }
                }

                item {
                    JournalSmallBox("Playlist / Frases favoritas", minHeight = 100.dp)
                }

                item {
                    JournalSmallBox("Notas finales", minHeight = 120.dp)
                }

                item {
                    Button(
                        onClick = { /* TODO: Guardar en Firebase */ },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = JournalPurpleDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Guardar en mi diario", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun JournalHeader(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp))
            .background(JournalPurpleMedium)
            .padding(20.dp)
    ) {
        Text(
            text = text.uppercase(),
            fontSize = 22.sp,
            color = Color.DarkGray,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = GuardianCity
        )
    }
}

@Composable
fun JournalTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = JournalPurpleDark)
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = JournalPurpleLight.copy(0.4f),
                focusedContainerColor = JournalPurpleLight,
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = JournalPurpleDark
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )
    }
}

@Composable
fun JournalDateField(label: String, value: String, modifier: Modifier, onValueChange: (String) -> Unit) {
    Box(
        modifier = modifier
            .background(JournalPurpleMedium.copy(0.3f), RoundedCornerShape(8.dp))
            .border(1.dp, JournalPurpleMedium, RoundedCornerShape(8.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, color = JournalPurpleDark, fontWeight = FontWeight.Bold)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            )
        }
    }
}

@Composable
fun JournalSectionCard(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("- $title -", modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Bold, color = JournalPurpleDark)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = JournalPurpleLight.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, JournalPurpleMedium)
        ) {
            Column(Modifier.padding(16.dp)) { content() }
        }
    }
}

@Composable
fun ClassificationRow(label: String, rating: Int, icon: ImageVector, onRatingChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.width(90.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Row {
            for (i in 1..5) {
                IconButton(onClick = { onRatingChange(i) }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (i <= rating) JournalPurpleDark else Color.LightGray.copy(0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FormatItem(label: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Surface(
            shape = CircleShape,
            color = if(isSelected) JournalPurpleMedium else Color.Transparent,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(icon, null, tint = if(isSelected) Color.White else Color.Gray, modifier = Modifier.padding(8.dp))
        }
        Text(label, fontSize = 11.sp, color = if(isSelected) JournalPurpleDark else Color.Gray, fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun JournalSmallBox(title: String, modifier: Modifier = Modifier, minHeight: androidx.compose.ui.unit.Dp = 80.dp) {
    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth().background(JournalPurpleMedium).padding(6.dp)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = minHeight).border(1.dp, JournalPurpleMedium).padding(8.dp)) {
            Text("Escribe aquí...", fontSize = 12.sp, color = Color.LightGray)
        }
    }
}

// --- EL PREVIEW QUE ME HAS PEDIDO ---
@Preview(showBackground = true, name = "Vista Previa Diario")
@Composable
fun ReadingJournalScreenPreview() {
    // Usamos un ID dummy para el preview
    ReadingJournalScreen(
        bookId = "test_book",
        onBackClick = {}
    )
}