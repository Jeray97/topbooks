package com.example.topbooks.data.model

import com.google.gson.annotations.SerializedName

data class GoogleBooksResponse(
    @SerializedName("items") val items: List<BookItem>?
)

data class BookItem(
    @SerializedName("id") val id: String?,
    @SerializedName("volumeInfo") val volumeInfo: VolumeInfo?
) {
    fun toDomain(): Book {
        return Book(
            id = id ?: "unknown_id",
            title = volumeInfo?.title ?: "Sin título",
            // 🔥 Añadimos el subtítulo al modelo
            subtitle = volumeInfo?.subtitle ?: "",
            authors = volumeInfo?.authors ?: emptyList(),
            description = volumeInfo?.description ?: "Sin descripción disponible.",
            imageUrl = volumeInfo?.imageLinks?.thumbnail?.replace("http:", "https:") ?: "",
            lanzamiento = volumeInfo?.publishedDate ?: "",
            averageRating = volumeInfo?.averageRating ?: 0.0,
            ratingsCount = volumeInfo?.ratingsCount ?: 0
        )
    }
}

data class VolumeInfo(
    @SerializedName("title") val title: String?,
    // 🔥 Añadimos la lectura del subtítulo desde el JSON
    @SerializedName("subtitle") val subtitle: String?,
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