package com.example.topbooks.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.example.topbooks.ui.theme.LoginColors

/**
 * Componente visual reutilizable que representa un libro en forma de tarjeta vertical.
 * * Es un componente "Stateless" (sin estado propio) ideal para usarse dentro de listas o cuadrículas.
 * * Muestra la portada del libro, su título, el primer autor y la fecha de lanzamiento.
 *
 * @param book Modelo de datos con la información del libro a renderizar.
 * @param onClick Acción que se ejecuta cuando el usuario toca la portada del libro (ej. navegar a la pantalla de detalles).
 */
@Composable
fun BookItem(
    book: Book,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .padding(end = 16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .height(210.dp)
                .fillMaxWidth()
                .clickable { onClick() }
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

        Text(
            text = book.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = LoginColors.PrimaryContainer
        )

        if (book.authors.isNotEmpty()) {
            Text(
                text = book.authors.first(),
                fontSize = 12.sp,
                color = LoginColors.PrimaryContainer.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Función de previsualización (Preview) para Android Studio.
 * * Permite ver cómo queda el diseño del componente sin tener que compilar y ejecutar la app entera en un teléfono.
 */
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

    // Renderizamos el componente pasándole los datos falsos (dummy)
    BookItem(book = dummyBook, onClick = {})
}