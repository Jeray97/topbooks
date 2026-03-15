package com.example.topbooks.data.model

import android.util.Log
import com.example.topbooks.utils.SeriesDetector
import com.google.gson.annotations.SerializedName

/**
 * Representa la respuesta principal que devuelve la API de Open Library al realizar una búsqueda general.
 *
 * @property docs Lista de documentos (libros) encontrados en la búsqueda.
 */
data class OpenLibrarySearchResponse(
    @SerializedName("docs") val docs: List<OpenLibraryDoc>?
)

/**
 * Representa un libro individual devuelto en los resultados de búsqueda de Open Library.
 * * Sirve como un DTO (Data Transfer Object) para mapear el JSON temporalmente antes de
 * convertirlo a nuestro modelo principal.
 *
 * @property key Identificador único del libro en Open Library (suele venir con el prefijo "/works/").
 * @property title Título del libro.
 * @property authorName Lista de nombres de los autores.
 * @property coverId ID numérico de la portada. Se utiliza luego para construir la URL visual.
 * @property firstPublishYear Año de la primera publicación.
 * @property ratingsAverage Calificación media de los usuarios en la plataforma.
 * @property ratingsCount Número total de calificaciones recibidas.
 */
data class OpenLibraryDoc(
    @SerializedName("key") val key: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("author_name") val authorName: List<String>?,
    @SerializedName("cover_i") val coverId: Int?,
    @SerializedName("first_publish_year") val firstPublishYear: Int?,
    @SerializedName("ratings_average") val ratingsAverage: Double?,
    @SerializedName("ratings_count") val ratingsCount: Int?
) {
    /**
     * Convierte este modelo de red de Open Library en el modelo de dominio interno [Book].
     * * Es una capa de seguridad y formato crucial para la app.
     *
     * @return Una instancia limpia, adaptada y segura de la clase [Book].
     */
    fun toDomain(): Book {

        // Construimos la URL de la portada basándonos en el ID proporcionado por la API
        val imageUrl =
            if (coverId != null)
                "https://covers.openlibrary.org/b/id/$coverId-L.jpg"
            else ""

        // Obtener título real asegurando que nunca sea nulo
        val bookTitle = title ?: "Sin título"

        // Detectar saga analizando el título
        val series = SeriesDetector.detect(bookTitle)

        Log.d("SERIES_DEBUG", "Title: $title -> series: $series")

        // ARREGLO CRÍTICO: A veces la API devuelve "/books/OL..." en lugar de "/works/OL...".
        // Extraemos solo lo que hay después de la última barra para no romper Compose Navigation.
        val cleanId = key?.substringAfterLast("/") ?: "unknown"

        return Book(
            id = cleanId,
            title = bookTitle,
            authors = authorName ?: emptyList(),
            description = "Toca para ver detalles...", // Texto por defecto en la vista de lista
            imageUrl = imageUrl,
            lanzamiento = firstPublishYear?.toString() ?: "",
            categories = emptyList(),

            // Asignamos los datos de la saga detectados
            seriesName = series?.name ?: "",
            seriesIndex = series?.index ?: 0,
            provider = "Open Library"
        )
    }
}

/**
 * Representa la información detallada de un libro obtenida a través de la "Work API" de Open Library.
 * * Se llama de forma independiente a la búsqueda para obtener la sinopsis completa.
 *
 * @property title Título del libro.
 * @property description Descripción o sinopsis del libro. Se usa [Any] en lugar de String porque
 * la API de Open Library es inconsistente: a veces devuelve un texto plano (String) y otras veces
 * devuelve un objeto JSON complejo ({ "type": "text", "value": "..." }).
 * @property covers Lista de IDs de las distintas portadas disponibles para esta obra.
 */
data class OpenLibraryWorkDetail(
    @SerializedName("title") val title: String?,

    // Puede ser String o JSON {type:"text",value:"..."}
    @SerializedName("description") val description: Any?,

    @SerializedName("covers") val covers: List<Int>?
)