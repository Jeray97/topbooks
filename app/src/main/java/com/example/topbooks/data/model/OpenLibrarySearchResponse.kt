package com.example.topbooks.data.model

import com.google.gson.annotations.SerializedName

// --- RESULTADOS DE BÚSQUEDA ---
data class OpenLibrarySearchResponse(
    @SerializedName("docs") val docs: List<OpenLibraryDoc>?
)

data class OpenLibraryDoc(
    @SerializedName("key") val key: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("author_name") val authorName: List<String>?,
    @SerializedName("cover_i") val coverId: Int?,
    @SerializedName("first_publish_year") val firstPublishYear: Int?,
    @SerializedName("ratings_average") val ratingsAverage: Double?,
    @SerializedName("ratings_count") val ratingsCount: Int?
) {
    fun toDomain(): Book {
        val imageUrl = if (coverId != null) "https://covers.openlibrary.org/b/id/$coverId-L.jpg" else ""
        return Book(
            id = key?.replace("/works/", "") ?: "unknown",
            title = title ?: "Sin título",
            authors = authorName ?: emptyList(),
            description = "Toca para ver detalles...",
            imageUrl = imageUrl,
            lanzamiento = firstPublishYear?.toString() ?: "",
            // averageRating = ratingsAverage ?: 0.0 // Si añades rating al modelo Book
        )
    }
}

// --- DETALLE DE LIBRO (WORK API) ---
// Esta es la clase que faltaba o daba error
data class OpenLibraryWorkDetail(
    @SerializedName("title") val title: String?,
    // La descripción puede ser un String o un Objeto JSON { type: "text", value: "..." }
    // Usamos Any para evitar errores de parseo y lo tratamos manualmente
    @SerializedName("description") val description: Any?,
    @SerializedName("covers") val covers: List<Int>?
)