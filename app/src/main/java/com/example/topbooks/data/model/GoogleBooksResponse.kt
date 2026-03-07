package com.example.topbooks.data.model

import com.example.topbooks.utils.SeriesDetector
import com.google.gson.annotations.SerializedName
import android.util.Log


data class GoogleBooksResponse(
    @SerializedName("items") val items: List<BookItem>?
)

data class BookItem(
    @SerializedName("id") val id: String?,
    @SerializedName("volumeInfo") val volumeInfo: VolumeInfo?
) {
    val title = volumeInfo?.title ?: "Sin título"
    val subtitle = volumeInfo?.subtitle ?: ""

    val fullTextForDetection = "$title $subtitle"
    val series = SeriesDetector.detect(fullTextForDetection)

    init {
        // Actualizamos el log para ver el texto completo que estamos analizando
        Log.d("SERIES_DEBUG", "Analizando: $fullTextForDetection -> series: $series")
    }

    fun toDomain(): Book {
        // Aseguramos que tenemos textos válidos, nunca nulos
        val safeTitle = volumeInfo?.title ?: "Sin título"
        val safeSubtitle = volumeInfo?.subtitle ?: ""

        return Book(
            id = id ?: "unknown_id",
            title = safeTitle,
            subtitle = safeSubtitle,
            authors = volumeInfo?.authors ?: emptyList(),
            description = volumeInfo?.description ?: "Sin descripción disponible.",
            imageUrl = volumeInfo?.imageLinks?.thumbnail?.replace("http:", "https:") ?: "",
            lanzamiento = volumeInfo?.publishedDate ?: "",
            averageRating = volumeInfo?.averageRating ?: 0.0,
            ratingsCount = volumeInfo?.ratingsCount ?: 0,
            isMature = volumeInfo?.maturityRating == "MATURE",
            categories = volumeInfo?.categories ?: emptyList(),
            seriesName = series?.name ?: "",
            seriesIndex = series?.index ?: 0
        )
    }
}

data class VolumeInfo(
    @SerializedName("title") val title: String?,
    @SerializedName("subtitle") val subtitle: String?,
    @SerializedName("authors") val authors: List<String>?,
    @SerializedName("description") val description: String?,
    @SerializedName("imageLinks") val imageLinks: ImageLinks?,
    @SerializedName("publishedDate") val publishedDate: String?,
    @SerializedName("averageRating") val averageRating: Double?,
    @SerializedName("ratingsCount") val ratingsCount: Int?,
    @SerializedName("maturityRating") val maturityRating: String?,
    @SerializedName("categories") val categories: List<String>?
)

data class ImageLinks(
    @SerializedName("thumbnail") val thumbnail: String?
)