package com.example.topbooks.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.topbooks.data.model.Book
import com.example.topbooks.R // Asegúrate de importar tu R

@Composable
fun BookItem(
    book: Book,
    onClick: () -> Unit
) {
    // Un diseño vertical: Portada arriba, Título abajo
    Column(
        modifier = Modifier
            .width(120.dp) // Ancho fijo para cada libro en la lista horizontal
            .padding(end = 16.dp) // Espacio a la derecha
    ) {
        // 1. La Tarjeta con la Imagen (Portada)
        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .height(180.dp) // Altura de la portada
                .fillMaxWidth()
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(book.imageUrl)
                    .crossfade(true) // Efecto suave al aparecer
                    .error(R.drawable.icon_codigodebarras) // Imagen si falla la carga (usa tu icono o pon uno genérico)
                    .build(),
                contentDescription = book.title,
                contentScale = ContentScale.Crop, // Llena la tarjeta recortando si hace falta
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Título del Libro
        Text(
            text = book.title,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2, // Si es muy largo, lo corta en 2 líneas
            overflow = TextOverflow.Ellipsis, // Pone "..." al final
            color = Color.Black // O tu ColorTextPrimary si lo importas
        )

        // 3. Autor (Opcional, más pequeño)
        if (book.authors.isNotEmpty()) {
            Text(
                text = book.authors.first(),
                fontSize = 12.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// --- PREVIEW ---
@Preview(showBackground = true)
@Composable
fun BookItemPreview() {
    // Creamos un libro falso para probar el diseño
    val dummyBook = Book(
        id = "1",
        title = "El Quijote de la Mancha Edición Especial",
        authors = listOf("Miguel de Cervantes"),
        description = "Un clásico...",
        imageUrl = "" // Dejamos la URL vacía para ver el icono de error/placeholder
    )

    BookItem(
        book = dummyBook,
        onClick = {}
    )
}