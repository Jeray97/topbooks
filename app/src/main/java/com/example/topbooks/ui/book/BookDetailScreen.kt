package com.example.topbooks.ui.book

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.topbooks.R
import com.example.topbooks.data.model.Book
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.ui.theme.*

// --- 1. PANTALLA PRINCIPAL ---
@Composable
fun BookDetailScreen(
    bookId: String,
    onBackClick: () -> Unit,
    viewModel: BookDetailViewModel = viewModel()
) {
    // 1. Al entrar, pedimos los datos al ViewModel
    LaunchedEffect(bookId) {
        viewModel.getBookById(bookId)
    }

    // 2. Observamos el estado (que incluye libro y foto autor)
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = ColorBackGroundGeneral, // Fondo Beige General
        topBar = { TopBar(onBackClick) }
    ) { paddingValues ->

        Box(modifier = Modifier.padding(paddingValues)) {
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ColorTituloCategoriaDetalle)
                }
            } else if (state.error != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.error!!, color = Color.Red)
                }
            } else if (state.book != null) {
                // Si tenemos libro, pintamos el contenido pasando también la foto del autor
                BookDetailContent(
                    book = state.book!!,
                    authorPhotoUrl = state.authorImageUrl
                )
            }
        }
    }
}

// --- 2. CONTENIDO VISUAL (La Tarjeta Marrón) ---
@Composable
fun BookDetailContent(book: Book, authorPhotoUrl: String?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Título fuera de la tarjeta
        Text(
            text = "Información del libro",
            fontFamily = CenturyGotic,
            fontSize = 24.sp,
            color = ColorTituloTopBooks,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // TARJETA MARRÓN PRINCIPAL
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ColorTituloCategoriaDetalle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Título del Libro
                Text(
                    text = "- ${book.title}",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontFamily = GuardianCity,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // FILA: Imagen + Sinopsis (Mitad y Mitad)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Imagen Portada
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        modifier = Modifier
                            .weight(0.4f)
                            .aspectRatio(0.65f)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(book.imageUrl)
                                .crossfade(true)
                                .error(R.drawable.icon_codigodebarras)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // 2. Columna derecha: Texto Sinopsis (Recortado)
                    Column(modifier = Modifier.weight(0.6f)) {
                        Text(
                            text = "- Sinopsis:",
                            color = Color.White,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = book.description.take(250) + "...",
                            color = Color.White,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Justify
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECCIÓN RESUMEN COMPLETO
                Text(
                    text = "- Resumen:",
                    color = Color.White,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = book.description,
                    color = Color.White,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Justify
                )

                Spacer(modifier = Modifier.height(24.dp))

                // SECCIÓN AUTOR Y BOTONES
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Autor (Izquierda)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "- Autor: ${book.authors.firstOrNull() ?: "Desconocido"}",
                            color = Color.White,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .align(Alignment.Start)
                        )

                        // FOTO REDONDA DEL AUTOR
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)), // Fondo semi-transparente
                            contentAlignment = Alignment.Center
                        ) {
                            if (authorPhotoUrl != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(authorPhotoUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Foto Autor",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                // Icono por defecto si no hay foto
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Autor",
                                    tint = Color.White,
                                    modifier = Modifier.size(50.dp)
                                )
                            }
                        }
                    }

                    // Botones (Derecha)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        BotonPersonalizado(texto = "COMENTARIOS")
                        BotonPersonalizado(texto = "RESEÑAS")
                    }
                }
            }
        }
    }
}

@Composable
fun BotonPersonalizado(texto: String) {
    Button(
        onClick = { /* TODO */ },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFE6B8A2), // Color Salmón claro
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .width(160.dp)
            .height(45.dp)
    ) {
        Text(text = texto, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

// --- PREVIEW ---
@Preview(showBackground = true)
@Composable
fun BookDetailPreview() {
    val dummyBook = Book(
        id = "1",
        title = "Los pilares de la tierra",
        authors = listOf("Ken Follett"),
        description = "Los pilares de la Tierra es una novela histórica...",
        imageUrl = "",
        lanzamiento = "1989"
    )
    MaterialTheme {
        BookDetailContent(book = dummyBook, authorPhotoUrl = null)
    }
}