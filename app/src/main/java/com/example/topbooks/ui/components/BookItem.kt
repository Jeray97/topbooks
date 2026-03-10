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
    // Un diseño en columna vertical: Portada arriba, Título y metadatos abajo
    Column(
        modifier = Modifier
            .width(120.dp) // Ancho fijo para mantener uniformidad en las estanterías
            .padding(end = 16.dp)
    ) {

        // 1. La Tarjeta con la Imagen (Portada)
        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .height(180.dp)
                .fillMaxWidth()
                // Activamos el clic en toda la superficie de la portada
                .clickable { onClick() }
        ) {
            // Utilizamos Coil para cargar imágenes asíncronas desde internet
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(book.imageUrl)
                    .crossfade(true) // Animación suave al aparecer la imagen
                    // Imagen de seguridad por si la API no nos devuelve ninguna portada
                    .error(R.drawable.icon_codigodebarras)
                    .build(),
                contentDescription = book.title,
                contentScale = ContentScale.Crop, // Recorta la imagen para que llene la tarjeta sin deformarse
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Título del Libro
        Text(
            text = book.title,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            // Restringimos a 2 líneas máximo. Si es más largo, se añaden puntos suspensivos (...)
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = Color.Black
        )

        // 3. Autor
        // Solo mostramos el autor si la lista no está vacía
        if (book.authors.isNotEmpty()) {
            Text(
                // Extraemos únicamente el primer autor para ahorrar espacio visual
                text = book.authors.first(),
                fontSize = 12.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 4. Fecha de lanzamiento
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