package com.example.topbooks.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Representa una respuesta individual dentro de un hilo de comentarios.
 * * Esta clase se utiliza para anidar respuestas dentro de un [Comment] principal.
 *
 * @property userId ID del usuario que escribe la respuesta.
 * @property userName Nombre visible del usuario.
 * @property userPhotoUrl URL o identificador del avatar del usuario.
 * @property text Contenido de la respuesta.
 * @property timestamp Fecha y hora exacta en la que se creó la respuesta (en milisegundos).
 */
data class Reply(
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String = "capibara_1",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Representa un comentario principal sobre un capítulo específico de un libro.
 * * Soporta hilos de conversación anidando listas de tipo [Reply].
 *
 * @property commentId Identificador único del comentario generado en Firestore.
 * @property bookId Identificador del libro al que pertenece el comentario.
 * @property userId Identificador del usuario que creó el comentario.
 * @property chapter Capítulo o sección del libro sobre la que se comenta.
 * @property text Contenido principal del comentario.
 * @property rating Calificación (estrellas) dada por el usuario en este comentario.
 * @property likes Cantidad de "Me gusta" que ha recibido el comentario.
 * @property edited Booleano que indica si el comentario ha sido modificado tras su publicación.
 * @property participantIds Lista de IDs de los usuarios que han participado en el hilo de este comentario (útil para notificaciones).
 * @property createAt Fecha de creación. La etiqueta [@ServerTimestamp] le dice a Firebase que asigne la hora del servidor automáticamente.
 * @property replies Lista de respuestas asociadas a este comentario (hilo de conversación).
 *
 * * -- Campos para la Interfaz de Usuario (UI) --
 * * Estas variables son 'var' porque normalmente se rellenan a posteriori, cruzando datos
 * con las colecciones de "users" o "books" para mostrar la información completa en pantalla.
 * @property userName Nombre del autor del comentario para mostrar en pantalla.
 * @property userPhotoUrl Avatar del autor del comentario.
 * @property bookTitle Título del libro para mostrar en las listas de actividad.
 * @property bookImageUrl Portada del libro para mostrar en las listas de actividad.
 */
data class Comment(
    // Datos principales del comentario
    val commentId: String = "",
    val bookId: String = "",
    val userId: String = "",
    val chapter: String = "",
    val text: String = "",
    val rating: Int = 0,
    val likes: Int = 0,
    val edited: Boolean = false,

    // Participantes del hilo (para notificaciones)
    val participantIds: List<String> = emptyList(),

    // Fecha de creación gestionada por Firestore
    @ServerTimestamp
    val createAt: Date? = null,

    // Lista de respuestas anidadas (hilos)
    val replies: List<Reply> = emptyList(),

    // Campos enriquecidos para la UI (Mutables para ser actualizados)
    var userName: String = "Usuario",
    var userPhotoUrl: String = "capibara_1",
    var bookTitle: String = "",
    var bookImageUrl: String = ""
)