package com.example.topbooks.data.model

import com.google.gson.annotations.SerializedName

/**
 * Modelo para mapear la respuesta de OpenLibrary (Mejores valorados/Modernos)
 */
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
        // OpenLibrary usa IDs de portada para construir la URL
        val imageUrl = if (coverId != null) {
            "https://covers.openlibrary.org/b/id/$coverId-L.jpg"
        } else {
            ""
        }

        return Book(
            // El ID viene como "/works/OL123", lo limpiamos
            id = key?.replace("/works/", "") ?: "unknown",
            title = title ?: "Sin título",
            authors = authorName ?: emptyList(),
            description = "Descripción disponible en detalle.",
            imageUrl = imageUrl,
            lanzamiento = firstPublishYear?.toString() ?: "",
            averageRating = ratingsAverage ?: 0.0,
            ratingsCount = ratingsCount ?: 0
        )
    }
}