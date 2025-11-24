package com.example.topbooks.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Representa al usuario de la aplicación.
 * Coincide con los campos que guardaremos en Firestore.
 */
data class User (
    val id: String = "",
    val name: String = "",
    val email: String = "",

    // Esta anotación permite que Firestore ponga la fecha del servidor automáticamente
    @ServerTimestamp
    val date_created: Date? = null
)