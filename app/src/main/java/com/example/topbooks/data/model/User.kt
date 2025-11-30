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
    val lastLogin: Date = Date()
)

    // Esta anotación permite que Firestore ponga la fecha del servidor automáticamente
    @ServerTimestamp
    val createAt: Date? = null
