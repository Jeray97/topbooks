package com.example.topbooks.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Representa el Diario de Lectura de un usuario para un libro específico.
 * * Esta clase almacena las notas personales, calificaciones detalladas (tropes, emociones)
 * y otros detalles que el usuario registra mientras lee o después de leer un libro.
 *
 * @property bookId Identificador del libro al que pertenece este diario.
 * @property userId Identificador del usuario dueño del diario.
 * @property bookTitle Título del libro (guardado aquí para mostrarlo rápido en listas sin consultar la API).
 * @property bookImageUrl Portada del libro (para mostrarlo en la lista de diarios del usuario).
 * @property title Título o nombre que el usuario le da a su entrada de diario.
 * @property author Nombre del autor del libro.
 * @property pages Número de páginas o formato de páginas registradas.
 * * * -- Privacidad --
 * @property isPublic Indica si el usuario permite que otros vean este diario.
 * * (Usamos [@PropertyName] para evitar que Firestore lo renombre accidentalmente a "public").
 * * * -- Calificaciones (Estrellas / Puntuaciones) --
 * @property mainRating Calificación general del libro (ej. 1 a 5 estrellas).
 * @property rRomance Nivel de romance del libro (ej. 1 a 5).
 * @property rHappy Nivel de felicidad/comedia que transmite.
 * @property rSad Nivel de tristeza/drama.
 * @property rSpicy Nivel de contenido adulto/spicy (🌶️).
 * * * -- Detalles de la lectura --
 * @property genre Género literario asignado por el usuario.
 * @property playlist Canciones o playlist que el usuario asoció a la lectura.
 * @property format Formato en el que se leyó (Físico, Digital, Audiolibro).
 * @property characters Personajes favoritos o notas sobre ellos.
 * @property nicknames Apodos o nombres cariñosos de los personajes.
 * @property quotes Citas favoritas destacadas por el usuario.
 * @property moments Momentos o escenas favoritas.
 * @property startDate Fecha en la que el usuario empezó a leer el libro (en formato texto).
 * @property endDate Fecha en la que el usuario terminó de leer el libro.
 * * * -- Metadatos y notas finales --
 * @property createdAt Fecha exacta de creación del diario, gestionada automáticamente por Firebase.
 * @property notes Notas generales, resumen o reseña extensa privada.
 */
data class Journal(
    // Datos de vinculación y UI
    var bookId: String = "",
    var userId: String = "",
    var bookTitle: String = "",
    var bookImageUrl: String = "",
    var title: String = "",
    var author: String = "",
    var pages: String = "",

    // Privacidad (Anotado para seguridad en Firestore)
    @get:PropertyName("isPublic")
    @set:PropertyName("isPublic")
    var isPublic: Boolean = false,

    // Calificaciones generales y por "subcategorias"
    var mainRating: Int = 0,

    @get:PropertyName("rRomance")
    @set:PropertyName("rRomance")
    var rRomance: Int = 0,

    @get:PropertyName("rHappy")
    @set:PropertyName("rHappy")
    var rHappy: Int = 0,

    @get:PropertyName("rSad")
    @set:PropertyName("rSad")
    var rSad: Int = 0,

    @get:PropertyName("rSpicy")
    @set:PropertyName("rSpicy")
    var rSpicy: Int = 0,

    // Desglose de contenido personal
    var genre: String = "",
    var playlist: String = "",
    var format: String = "",
    var characters: String = "",
    var nicknames: String = "",
    var quotes: String = "",
    var moments: String = "",
    var startDate: String = "",
    var endDate: String = "",

    // Metadatos
    @ServerTimestamp
    var createdAt: Date? = null,

    var notes: String = ""
)