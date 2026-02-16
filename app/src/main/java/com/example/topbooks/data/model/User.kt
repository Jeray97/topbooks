package com.example.topbooks.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Modelo de Usuario.
 */
data class User(
    val uid: String = "",
    val displayName: String = "",
    val displayNameLowercase: String = "",
    val email: String = "",
    val photoURL: String = "capibara_1",
    val role: String? = "user",

    // Preferencias y Tutorial
    val isTutorialCompleted: Boolean = false,
    val favoriteGenres: List<String> = emptyList(),
    val favoriteBooks: List<String> = emptyList(),
    val preferences: Map<String, Boolean> = emptyMap(),

    // Estadísticas
    val lastLogin: Date = Date(),
    val reviewsCount: Int = 0,
    val bookmarksCount: Int = 0,
    val commentsCount: Int = 0,
    val friendsCount: Int = 0,

    // Fecha de creación (automática por Firestore)
    @ServerTimestamp
    val createdAt: Date? = null
)