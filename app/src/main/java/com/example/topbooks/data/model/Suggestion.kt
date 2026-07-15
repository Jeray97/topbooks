package com.example.topbooks.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Representa una sugerencia enviada por un usuario a través del buzón de sugerencias.
 * 
 * @property id Identificador único de la sugerencia (generado por Firestore).
 * @property userId ID del usuario que envió la sugerencia.
 * @property userName Nombre del usuario que envió la sugerencia.
 * @property userEmail Email del usuario para poder contactarle si es necesario.
 * @property category Categoría de la sugerencia (Bug, Mejora, Contenido, Otro).
 * @property title Título breve de la sugerencia.
 * @property message Descripción detallada de la sugerencia.
 * @property createdAt Fecha de creación de la sugerencia (generada automáticamente por Firestore).
 * @property status Estado de la sugerencia (Nueva, En revisión, Implementada, Rechazada).
 */
data class Suggestion(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val category: String = "Otro",
    val title: String = "",
    val message: String = "",
    @ServerTimestamp
    val createdAt: Date? = null,
    val status: String = "Nueva"
)
