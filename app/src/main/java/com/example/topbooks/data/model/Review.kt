package com.example.topbooks.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Representa una Reseña general de un libro escrita por un usuario.
 * * A diferencia de los comentarios, una reseña suele incluir una valoración global ([rating])
 * y una opinión completa sobre la obra.
 *
 * @property id Identificador único de la reseña en la colección de Firestore.
 * @property bookId Identificador del libro que se está reseñando.
 * @property userId Identificador del usuario autor de la reseña.
 * @property rating Puntuación otorgada al libro (generalmente de 1 a 5 estrellas).
 * @property text Contenido en texto de la reseña.
 * @property likes Cantidad de "Me gusta" o votos útiles que ha recibido esta reseña por parte de la comunidad.
 * * * -- Metadatos --
 * @property createAt Fecha y hora en la que se publicó la reseña.
 * * La anotación [@ServerTimestamp] asegura que Firebase asigne la hora exacta del servidor, evitando problemas de zonas horarias.
 * * * -- Campos enriquecidos para la Interfaz de Usuario (UI) --
 * * Estas variables son 'var' (mutables) porque se suelen cruzar y rellenar a posteriori
 * con la información del perfil del usuario y del libro para pintarlas fácilmente en las listas.
 * @property userName Nombre visible del autor de la reseña.
 * @property userPhotoUrl URL o identificador del avatar del autor.
 * @property bookTitle Título del libro reseñado (útil para el feed de actividad).
 * @property bookImageUrl URL de la portada del libro reseñado.
 */
data class Review(
    // Datos base de la reseña
    val id: String = "",
    val bookId: String = "",
    val userId: String = "",
    val rating: Int = 0,
    val text: String = "",
    val likes: Int = 0,

    // Metadatos gestionados por Firestore
    @ServerTimestamp
    val createAt: Date? = null,

    // Datos cruzados para facilitar la muestra en la UI
    var userName: String = "Usuario",
    var userPhotoUrl: String = "",
    var bookTitle: String = "",
    var bookImageUrl: String = ""
)