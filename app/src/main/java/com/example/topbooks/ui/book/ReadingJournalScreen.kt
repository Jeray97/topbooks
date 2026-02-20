package com.example.topbooks.ui.book

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.ui.theme.*

// --- CONSTANTES ---
val JournalPurpleLight = Color(0xFFF3E5F5)
val JournalPurpleMedium = Color(0xFFE1BEE7)
val JournalPurpleDark = Color(0xFF9575CD)
val JournalGridColor = Color(0xFFE0E0E0)

@Composable
fun ReadingJournalScreen(
    bookId: String,
    initialTitle: String = "",
    initialAuthor: String = "",
    initialImage: String = "",
    initialPages: String = "",
    onBackClick: () -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var author by remember { mutableStateOf(initialAuthor) }
    var pages by remember { mutableStateOf(initialPages) }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }

    var rRomance by remember { mutableIntStateOf(0) }
    var rHappy by remember { mutableIntStateOf(0) }
    var rSad by remember { mutableIntStateOf(0) }
    var rHot by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = Color.White,
        topBar = { TopBar(onBackClick = onBackClick) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 20.dp.toPx()
                for (x in 0..size.width.toInt() step step.toInt()) drawLine(JournalGridColor, start = androidx.compose.ui.geometry.Offset(x.toFloat(), 0f), end = androidx.compose.ui.geometry.Offset(x.toFloat(), size.height))
                for (y in 0..size.height.toInt() step step.toInt()) drawLine(JournalGridColor, start = androidx.compose.ui.geometry.Offset(0f, y.toFloat()), end = androidx.compose.ui.geometry.Offset(size.width, y.toFloat()))
            }
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { JournalHeader("Lectura terminada") }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(modifier = Modifier.width(120.dp).height(180.dp).border(2.dp, Color.Black, RoundedCornerShape(4.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                            if (initialImage.isNotEmpty()) {
                                AsyncImage(model = initialImage, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                // CORRECCIÓN: Usamos el parámetro horizontalAlignment correctamente
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Call, null, tint = Color.LightGray)
                                    Text("Portada", fontSize = 10.sp, color = Color.LightGray)
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            JournalTextField("Título:", title) { title = it }
                            JournalTextField("Autor:", author) { author = it }
                            JournalTextField("Páginas/Cap:", pages) { pages = it }
                        }
                    }
                }
                item { Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) { JournalDateField("Fecha inicio", startDate, Modifier.weight(1f)) { startDate = it }; JournalDateField("Fecha fin", endDate, Modifier.weight(1f)) { endDate = it } } }
                item { JournalSectionCard("Clasificación") {
                    ClassificationRow("Romántico:", rRomance, Icons.Default.Favorite) { rRomance = it }
                    ClassificationRow("Alegre:", rHappy, Icons.Default.Face) { rHappy = it }
                    ClassificationRow("Triste:", rSad, Icons.Default.Call) { rSad = it }
                    ClassificationRow("Hot:", rHot, Icons.Default.Call) { rHot = it }
                }}
                item { Button(onClick = { /* Save */ }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = JournalPurpleDark), shape = RoundedCornerShape(12.dp)) { Text("Guardar en mi diario", fontWeight = FontWeight.Bold) } }
            }
        }
    }
}

// --- HELPERS ---

@Composable fun JournalHeader(t: String) = Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 24.dp)).background(JournalPurpleMedium).padding(20.dp)) { Text(t.uppercase(), fontSize = 22.sp, color = Color.DarkGray, fontWeight = FontWeight.ExtraBold, fontFamily = GuardianCity) }

@Composable fun JournalTextField(l: String, v: String, onV: (String) -> Unit) = Column { Text(l, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = JournalPurpleDark); TextField(value = v, onValueChange = onV, modifier = Modifier.fillMaxWidth().height(50.dp), colors = TextFieldDefaults.colors(unfocusedContainerColor = JournalPurpleLight.copy(0.4f), focusedContainerColor = JournalPurpleLight, unfocusedIndicatorColor = Color.Transparent, focusedIndicatorColor = JournalPurpleDark), shape = RoundedCornerShape(8.dp), singleLine = true) }

@Composable fun JournalDateField(l: String, v: String, m: Modifier, onV: (String) -> Unit) = Box(
    modifier = m.background(JournalPurpleMedium.copy(0.3f), RoundedCornerShape(8.dp)).border(1.dp, JournalPurpleMedium, RoundedCornerShape(8.dp)).padding(12.dp),
    contentAlignment = Alignment.Center
) {
    // CORRECCIÓN: Usamos el parámetro horizontalAlignment correctamente aquí también
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(l, fontSize = 10.sp, color = JournalPurpleDark, fontWeight = FontWeight.Bold)
        BasicTextField(value = v, onValueChange = onV, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center))
    }
}

@Composable fun JournalSectionCard(t: String, c: @Composable () -> Unit) = Column(Modifier.fillMaxWidth()) { Text("- $t -", Modifier.fillMaxWidth().padding(bottom = 4.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = JournalPurpleDark); Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = JournalPurpleLight.copy(0.2f)), border = BorderStroke(1.dp, JournalPurpleMedium)) { Column(Modifier.padding(16.dp)) { c() } } }

@Composable fun ClassificationRow(l: String, r: Int, i: ImageVector, onR: (Int) -> Unit) = Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) { Text(l, modifier = Modifier.width(90.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium); Row { for (k in 1..5) IconButton(onClick = { onR(k) }, modifier = Modifier.size(28.dp)) { Icon(i, null, tint = if (k <= r) JournalPurpleDark else Color.LightGray.copy(0.5f), modifier = Modifier.size(20.dp)) } } }