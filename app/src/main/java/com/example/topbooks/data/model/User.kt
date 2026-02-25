package com.example.topbooks.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Modelo de Usuario.
 * Refleja los datos guardados en la colección "users" de Firestore.
 */
data class User(
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

    // Estadísticas
    val lastLogin: Date = Date(),
    val reviewsCount: Int = 0,    // Total de reseñas escritas
    val bookmarksCount: Int = 0,  // Total de libros guardados/pendientes
    val commentsCount: Int = 0,   // Total de comentarios en capítulos
    val friendsCount: Int = 0,    // Total de amigos seguidos
    val booksCompleted: Int = 0,  // Total de libros marcados como "Leídos"

    // Notificaciones
    val fcmToken : String = "",

    // Fecha de creación (automática por Firestore)
    @ServerTimestamp
    val createdAt: Date? = null
)