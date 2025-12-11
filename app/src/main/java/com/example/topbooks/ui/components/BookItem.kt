package com.example.topbooks.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.example.topbooks.R

@Composable
fun BookItem(
    book: Book,
    onClick: () -> Unit
) {
    // Un diseño vertical: Portada arriba, Título abajo
    Column(
        modifier = Modifier
            .width(120.dp)
            .padding(end = 16.dp)
    ) {
        // 1. La Tarjeta con la Imagen (Portada)
        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .height(180.dp)
                .fillMaxWidth()
                .clickable { onClick() } //Activamos el clic
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(book.imageUrl)
                    .crossfade(true)
                    .error(R.drawable.icon_codigodebarras)
                    .build(),
                contentDescription = book.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Título del Libro
        Text(
            text = book.title,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = Color.Black
        )

        // 3. Autor
        if (book.authors.isNotEmpty()) {
            Text(
                text = book.authors.first(),
                fontSize = 12.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 4. Fecha
        if(book.lanzamiento.isNotEmpty()) {
            Text(
                text = book.lanzamiento,
                fontSize = 12.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BookItemPreview() {
    val dummyBook = Book(
        id = "1",
        title = "El Quijote",
        authors = listOf("Cervantes"),
        description = "...",
        imageUrl = "",
        lanzamiento = "2025"
    )

    BookItem(book = dummyBook, onClick = {})
}