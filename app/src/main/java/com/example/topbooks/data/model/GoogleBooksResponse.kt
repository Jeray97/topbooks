package com.example.topbooks.data.model

import com.example.topbooks.utils.SeriesDetector
import com.google.gson.annotations.SerializedName
import android.util.Log


/**
 * Representa la respuesta principal (raíz) que devuelve la API de Google Books al realizar una búsqueda.
 *
 * @property items Lista de libros encontrados. Puede ser nula si la búsqueda no arroja resultados.
 */
data class GoogleBooksResponse(
    @SerializedName("items") val items: List<BookItem>?
)

/**
 * Representa un libro individual devuelto dentro de la lista de resultados de Google Books.
 * * Actúa como un DTO (Data Transfer Object) intermedio antes de convertirse en el modelo de dominio [Book].
 *
 * @property id Identificador único del libro en la base de datos de Google.
 * @property volumeInfo Objeto que contiene toda la información detallada del libro (título, autores, etc.).
 */
data class BookItem(
    @SerializedName("id") val id: String?,
    @SerializedName("volumeInfo") val volumeInfo: VolumeInfo?
) {
    // Extracción segura del título y subtítulo para evitar nulos durante la detección de sagas.
    val title = volumeInfo?.title ?: "Sin título"
    val subtitle = volumeInfo?.subtitle ?: ""

    /** * Texto combinado que se enviará al detector de sagas.
     * Muchas veces el número de la saga viene escondido en el subtítulo.
     */
    val fullTextForDetection = "$title $subtitle"

    /** * Objeto que contiene el nombre de la saga y el número de volumen,
     * calculado automáticamente al instanciar la clase.
     */
    val series = SeriesDetector.detect(fullTextForDetection)

    init {
        // Actualizamos el log para ver el texto completo que estamos analizando
        // Esto es útil para depurar por qué una saga no se detectó correctamente.
        Log.d("SERIES_DEBUG", "Analizando: $fullTextForDetection -> series: $series")
    }

    /**
     * Convierte este modelo de red (Google Books) en nuestro modelo de dominio principal ([Book]).
     * * Es una excelente práctica de arquitectura porque aísla al resto de la app
     * de los posibles cambios o nulos que devuelva la API de Google.
     *
     * @return Una instancia limpia y segura de la clase [Book].
     */
    fun toDomain(): Book {
        // Aseguramos que tenemos textos válidos, nunca nulos
        val safeTitle = volumeInfo?.title ?: "Sin título"
        val safeSubtitle = volumeInfo?.subtitle ?: ""

        // SANITIZACIÓN: Limpiamos el HTML de Google Books
        val rawDescription = volumeInfo?.description ?: "Sin descripción disponible."
        val cleanDescription = com.example.topbooks.utils.HtmlCleaner.clean(rawDescription)

        return Book(
            id = id ?: "unknown_id",
            title = safeTitle,
            subtitle = safeSubtitle,
            authors = volumeInfo?.authors ?: emptyList(),
            description = cleanDescription,

            // TRUCO DE SEGURIDAD: Reemplazamos "http" por "https"
            // porque Android bloquea por defecto las imágenes que no son seguras (Cleartext traffic).
            imageUrl = volumeInfo?.imageLinks?.thumbnail?.replace("http:", "https:") ?: "",

            lanzamiento = volumeInfo?.publishedDate ?: "",
            averageRating = volumeInfo?.averageRating ?: 0.0,
            ratingsCount = volumeInfo?.ratingsCount ?: 0,
            isMature = volumeInfo?.maturityRating == "MATURE",
            categories = volumeInfo?.categories ?: emptyList(),

            // Asignamos los datos de la saga detectados por nuestro algoritmo
            seriesName = series?.name ?: "",
            seriesIndex = series?.index ?: 0,
            provider = "Google Books"
        )
    }
}

/**
 * Contiene la información bibliográfica del libro proporcionada por Google.
 * * Utilizamos [@SerializedName] para mapear exactamente cómo se llama la variable en el JSON de Google
 * a nuestra variable en Kotlin, permitiendo usar nombres diferentes si quisiéramos.
 */
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

/**
 * Contiene las URLs de las portadas del libro.
 *
 * @property thumbnail URL de la imagen en miniatura (portada).
 */
data class ImageLinks(
    @SerializedName("thumbnail") val thumbnail: String?
)