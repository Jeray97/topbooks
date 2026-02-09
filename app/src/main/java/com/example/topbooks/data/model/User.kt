package com.example.topbooks.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Representa al usuario de la aplicación.
 * Coincide con los campos que guardaremos en Firestore.
 */
data class User (
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoURL: String = "",
    val role: String? = "user",
    val preferences: Map<String, Boolean> = emptyMap(),
    val lastLogin: Date = Date(),
    val favoriteBooks: List<String> = emptyList(),
    val reviewsCount: Int = 0,
    val bookmarksCount: Int = 0,
    val commentsCount: Int = 0,
    val friendsCount: Int = 0
)

    // Esta anotación permite que Firestore ponga la fecha del servidor automáticamente
    @ServerTimestamp
    val createAt: Date? = null
