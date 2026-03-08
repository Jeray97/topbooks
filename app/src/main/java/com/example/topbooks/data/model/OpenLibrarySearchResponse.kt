package com.example.topbooks.data.model

import android.util.Log
import com.example.topbooks.utils.SeriesDetector
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

        val imageUrl =
            if (coverId != null)
                "https://covers.openlibrary.org/b/id/$coverId-L.jpg"
            else ""

        // ✔ obtener título real
        val bookTitle = title ?: "Sin título"

        // ✔ detectar saga
        val series = SeriesDetector.detect(bookTitle)

        Log.d("SERIES_DEBUG", "Title: $title -> series: $series")

        return Book(
            id = key?.replace("/works/", "") ?: "unknown",
            title = bookTitle,
            authors = authorName ?: emptyList(),
            description = "Toca para ver detalles...",
            imageUrl = imageUrl,
            lanzamiento = firstPublishYear?.toString() ?: "",
            categories = emptyList(),
            seriesName = series?.name ?: "",
            seriesIndex = series?.index ?: 0,
            provider = "Open Library"
        )
    }
}


// --- DETALLE DE LIBRO (WORK API) ---
data class OpenLibraryWorkDetail(
    @SerializedName("title") val title: String?,

    // Puede ser String o JSON {type:"text",value:"..."}
    @SerializedName("description") val description: Any?,

    @SerializedName("covers") val covers: List<Int>?
)