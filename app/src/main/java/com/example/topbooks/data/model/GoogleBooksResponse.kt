package com.example.topbooks.data.model

import com.google.gson.annotations.SerializedName

/**
 * 1. CLASE PRINCIPAL: La respuesta raíz que nos da Google.
 * Contiene una lista de items.
 */
data class GoogleBooksResponse(
    @SerializedName("items") val items: List<BookItem>?
)

/**
 * 2. CLASE ITEM: Cada libro tal cual viene del JSON.
 */
data class BookItem(
    @SerializedName("id") val id: String?, // El ID también podría ser nulo a veces
    @SerializedName("volumeInfo") val volumeInfo: VolumeInfo? // Puede ser nulo
) {
    // Función para convertir estos datos "feos" al modelo "bonito" (Book)
    fun toDomain(): Book {
        return Book(
            id = id ?: "unknown_id", // Si no hay ID, ponemos uno por defecto
            title = volumeInfo?.title ?: "Sin título", // Si no hay título, ponemos texto por defecto
            authors = volumeInfo?.authors ?: emptyList(),
            description = volumeInfo?.description ?: "Sin descripción disponible.",
            // Acceso seguro a imageLinks
            imageUrl = volumeInfo?.imageLinks?.thumbnail?.replace("http:", "https:") ?: "",
            lanzamiento = volumeInfo?.publishedDate ?: "Sin fecha"
        )
    }
}

/**
 * 3. CLASE DETALLES (VolumeInfo)
 */
data class VolumeInfo(
    @SerializedName("title") val title: String?,
    @SerializedName("authors") val authors: List<String>?,
    @SerializedName("description") val description: String?,
    @SerializedName("imageLinks") val imageLinks: ImageLinks?,
    @SerializedName("publishedDate") val publishedDate: String?
)

/**
 * 4. CLASE IMÁGENES (ImageLinks)
 */
data class ImageLinks(
    @SerializedName("thumbnail") val thumbnail: String?
)

data class OpenLibrarySearchResponse(
    val docs: List<OpenLibraryAuthor>
)

data class OpenLibraryAuthor(
    val key: String?, // Esto nos dará algo como "/authors/OL26224A"
    val name: String?
)