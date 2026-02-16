package com.example.topbooks.data.model

import com.google.gson.annotations.SerializedName

/**
 * 1. CLASE PRINCIPAL
 */
data class GoogleBooksResponse(
    @SerializedName("items") val items: List<BookItem>?
)

/**
 * 2. CLASE ITEM
 */
data class BookItem(
    @SerializedName("id") val id: String?,
    @SerializedName("volumeInfo") val volumeInfo: VolumeInfo?
) {
    fun toDomain(): Book {
        return Book(
            id = id ?: "unknown_id",
            title = volumeInfo?.title ?: "Sin título",
            authors = volumeInfo?.authors ?: emptyList(),
            description = volumeInfo?.description ?: "Sin descripción disponible.",
            // Forzamos HTTPS para que las imágenes carguen siempre
            imageUrl = volumeInfo?.imageLinks?.thumbnail?.replace("http:", "https:") ?: "",
            lanzamiento = volumeInfo?.publishedDate ?: "",
            // Mapeamos el rating, si no viene es 0.0
            averageRating = volumeInfo?.averageRating ?: 0.0,
            ratingsCount = volumeInfo?.ratingsCount ?: 0
        )
    }
}

/**
 * 3. CLASE DETALLES
 */
data class VolumeInfo(
    @SerializedName("title") val title: String?,
    @SerializedName("authors") val authors: List<String>?,
    @SerializedName("description") val description: String?,
    @SerializedName("imageLinks") val imageLinks: ImageLinks?,
    @SerializedName("publishedDate") val publishedDate: String?,
    @SerializedName("averageRating") val averageRating: Double?,
    @SerializedName("ratingsCount") val ratingsCount: Int?
)

data class ImageLinks(
    @SerializedName("thumbnail") val thumbnail: String?
)