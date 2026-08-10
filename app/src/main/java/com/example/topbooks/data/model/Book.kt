package com.example.topbooks.data.model

/**
 * Representa un Libro dentro de la aplicación TopBooks.
 * * Esta clase de datos (data class) almacena toda la información relacionada con un libro,
 * combinando los datos obtenidos de las APIs (como Google Books u Open Library)
 * con los datos generados por la comunidad (como las ediciones de sagas y sus votos).
 *
 * @property id Identificador único del libro (normalmente el ID de la API).
 * @property title Título principal del libro.
 * @property subtitle Subtítulo del libro, si lo tiene.
 * @property authors Lista con los nombres de los autores del libro.
 * @property description Sinopsis o resumen del libro.
 * @property imageUrl URL de la portada del libro para mostrarla en la interfaz.
 * @property lanzamiento Fecha o año de publicación del libro.
 * @property averageRating Nota media del libro según la API externa.
 * @property ratingsCount Número total de calificaciones que tiene el libro.
 * @property pageCount Número total de páginas.
 * @property isMature Booleano que indica si el libro contiene contenido solo para adultos.
 * @property categories Lista de géneros o categorías a las que pertenece el libro.
 * @property seriesName Nombre de la saga o serie a la que pertenece (ej. "Harry Potter").
 * @property seriesIndex Número de volumen o entrega dentro de la saga (ej. 1, 2, 3...).
 * @property provider Indica de qué API externa provienen los datos (ej. "Google Books").
 * * -- Campos para el sistema de edición comunitaria de Sagas --
 * @property seriesEditorUid ID del usuario de Firebase que propuso la edición de la saga.
 * @property seriesEditorName Nombre del usuario que editó la saga.
 * @property seriesEditorAvatar URL del avatar del usuario que editó la saga.
 * @property seriesEditDate Fecha (en milisegundos) en la que se realizó la edición.
 * @property seriesUpvotes Cantidad de votos positivos del libro.
 * @property seriesDownvotes Cantidad de votos negativos del libro.
 * @property seriesVoters Lista de IDs de los usuarios que ya han emitido su voto en esta edición.
 */
data class Book(
    // Datos básicos del libro
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val authors: List<String> = emptyList(),
    val description: String = "",
    val imageUrl: String = "",
    val lanzamiento: String = "",
    val averageRating: Double = 0.0,
    val ratingsCount: Int = 0,
    val pageCount: Int = 0,
    val isMature: Boolean = false,
    val categories: List<String> = emptyList(),

    // Datos de la saga y origen
    val seriesName: String = "",
    val seriesIndex: Int = 0,
    val provider: String = "Desconocido",
    val purchaseUrl: String? = null,

    // Sistema social: Edición colaborativa de Sagas
    val seriesEditorUid: String? = null,
    val seriesEditorName: String? = null,
    val seriesEditorAvatar: String? = null,
    val seriesEditDate: Long? = null,
    val seriesUpvotes: Int = 0,
    val seriesDownvotes: Int = 0,
    val seriesVoters: List<String> = emptyList()
) {
    /**
     * Propiedad calculada que determina si el libro forma parte de una saga.
     * * Funcionamiento: Comprueba si la variable [seriesName] tiene algún texto.
     * [isNotBlank] devuelve 'true' si el texto no está vacío y no está compuesto solo por espacios en blanco.
     */
    val isSaga: Boolean
        get() = seriesName.isNotBlank()

}