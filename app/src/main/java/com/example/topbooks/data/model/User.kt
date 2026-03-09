package com.example.topbooks.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Representa a un Usuario dentro de la aplicación.
 * * Este modelo (data class) refleja exactamente la estructura de los documentos
 * almacenados en la colección "users" de Firestore.
 *
 * @property uid Identificador único del usuario proporcionado por Firebase Authentication.
 * @property displayName Nombre público que el usuario elige mostrar en su perfil.
 * @property displayNameLowercase Versión en minúsculas del nombre.
 * * (Es una técnica excelente para facilitar las búsquedas de texto en Firestore ignorando mayúsculas/minúsculas).
 * @property email Dirección de correo electrónico asociada a la cuenta del usuario.
 * @property photoURL URL o nombre del recurso del avatar elegido por el usuario (por defecto "capibara_1").
 * @property role Rol del usuario dentro del sistema (por defecto "user"). Útil si en un futuro añades roles de "admin" o "moderator".
 * @property bio Pequeña descripción o biografía que el usuario puede escribir para su perfil público.
 *
 * * * -- Preferencias y Tutorial --
 * @property isTutorialCompleted Booleano que indica si el usuario ya ha pasado por el flujo inicial de selección de gustos (Onboarding).
 * @property favoriteGenres Lista de géneros literarios favoritos que el usuario seleccionó en el tutorial.
 * @property favoriteBooks Lista de identificadores (IDs) de los libros que el usuario marcó como favoritos.
 * @property preferences Mapa (clave-valor) para guardar ajustes adicionales o "flags" del usuario en la base de datos si fuera necesario.
 *
 * * * -- Estadísticas de Actividad --
 * @property lastLogin Fecha en la que el usuario inició sesión por última vez.
 * @property reviewsCount Número total de reseñas globales escritas por este usuario.
 * @property bookmarksCount Número de libros que el usuario ha guardado o puesto en pendientes.
 * @property commentsCount Número de comentarios, notas de capítulos o interacciones en hilos realizados.
 * @property friendsCount Cantidad de amigos, seguidores o personas a las que sigue en la comunidad.
 * @property booksCompleted Cantidad total de libros que el usuario ha marcado como terminados o "Leídos".
 *
 * * * -- Notificaciones y Metadatos --
 * @property fcmToken Token de Firebase Cloud Messaging. Es esencial para saber a qué dispositivo exacto se deben enviar las notificaciones Push (Deep Links).
 * @property createdAt Fecha en la que se creó la cuenta en el sistema. [@ServerTimestamp] asigna la hora exacta del servidor de forma automática.
 */
data class User(
    // Datos Básicos
    val uid: String = "",
    val displayName: String = "",
    val displayNameLowercase: String = "",
    val email: String = "",
    val photoURL: String = "capibara_1",
    val role: String? = "user",
    val bio: String = "",

    // Preferencias y Tutorial
    val isTutorialCompleted: Boolean = false,
    val favoriteGenres: List<String> = emptyList(),
    val favoriteBooks: List<String> = emptyList(),
    val preferences: Map<String, Boolean> = emptyMap(),

    // Estadísticas de la Comunidad
    val lastLogin: Date = Date(),
    val reviewsCount: Int = 0,    // Total de reseñas escritas
    val bookmarksCount: Int = 0,  // Total de libros guardados/pendientes
    val commentsCount: Int = 0,   // Total de comentarios en capítulos
    val friendsCount: Int = 0,    // Total de amigos seguidos
    val booksCompleted: Int = 0,  // Total de libros marcados como "Leídos"

    // Notificaciones y Metadatos
    val fcmToken : String = "",

    @ServerTimestamp
    val createdAt: Date? = null
)