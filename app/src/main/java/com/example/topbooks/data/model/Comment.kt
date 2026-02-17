package com.example.topbooks.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// Clase para las respuestas anidadas (hilos)
data class Reply(
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String = "capibara_1",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class Comment(
    val commentId: String = "",
    val bookId: String = "", // ID como String
    val userId: String = "", // ID como String
    val chapter: String = "",
    val text: String = "",
    val rating: Int = 0,
    val likes: Int = 0,
    val edited: Boolean = false,
    @ServerTimestamp
    val createAt: Date? = null,

    // Lista de respuestas para el hilo
    val replies: List<Reply> = emptyList(),

    // Campos para la UI
    var userName: String = "Usuario",
    var userPhotoUrl: String = "capibara_1",
    var bookTitle: String = "",
    var bookImageUrl: String = ""
)