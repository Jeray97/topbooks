package com.example.topbooks.ui.community

import androidx.compose.ui.graphics.Color

/* =============================================================================
 *  HELPERS DE LA PANTALLA COMUNIDAD
 * =============================================================================
 *  Funciones puras (no Composables) que se usan en varios sitios de la UI.
 * ============================================================================= */

/**
 * Convierte un timestamp en milisegundos a un texto relativo en español casual.
 * Ejemplos: "hace un ratito", "hace 2h", "ayer", "hace 3 días", "el 4 mar".
 *
 * No usamos DateUtils.getRelativeTimeSpanString porque su español es muy
 * formal ("hace 2 horas") y queremos algo más cercano ("hace 2h").
 */
fun formatRelativeTime(timestampMillis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestampMillis

    val minutes = diff / 60_000
    val hours = diff / 3_600_000
    val days = diff / 86_400_000

    return when {
        diff < 60_000 -> "ahora mismo"
        diff < 600_000 -> "hace un ratito"     // < 10 min
        minutes < 60 -> "hace ${minutes}min"
        hours < 5 && hours >= 1 -> "hace ${hours}h"
        hours < 24 -> when {
            isToday(timestampMillis) -> "esta mañana"
            else -> "hace ${hours}h"
        }
        days == 1L -> "ayer"
        days < 7 -> "hace ${days} días"
        days < 30 -> "hace ${days / 7} semanas"
        else -> "hace ${days / 30} meses"
    }
}

/**
 * Determina si el timestamp pertenece al día de hoy (mismo día calendario).
 */
private fun isToday(millis: Long): Boolean {
    val now = java.util.Calendar.getInstance()
    val that = java.util.Calendar.getInstance().apply { timeInMillis = millis }
    return now.get(java.util.Calendar.YEAR) == that.get(java.util.Calendar.YEAR) &&
            now.get(java.util.Calendar.DAY_OF_YEAR) == that.get(java.util.Calendar.DAY_OF_YEAR)
}

/**
 * Devuelve un color de fondo cálido y consistente para representar una portada
 * de libro cuando no hay coverUrl disponible. Se basa en el hash del bookId,
 * así un mismo libro siempre tendrá el mismo "color de respaldo".
 *
 * La paleta está pensada para que combine con tu identidad TopBooks (tonos
 * tierra y profundos, nada estridente).
 */
fun fallbackCoverColor(bookId: String): Color {
    val palette = listOf(
        Color(0xFF4A6B8A),  // Azul mediodía
        Color(0xFFC44545),  // Rojo terracota
        Color(0xFF6B4A8A),  // Morado uva
        Color(0xFF2D5F3F),  // Verde bosque
        Color(0xFFC73670),  // Rosa intenso
        Color(0xFFD4A56A),  // Mostaza
        Color(0xFF8B5A3C),  // Marrón canela
        Color(0xFF3F4A5F)   // Azul pizarra
    )
    val idx = (kotlin.math.abs(bookId.hashCode())) % palette.size
    return palette[idx]
}

/**
 * Texto del tag de acción según el tipo de post. Determina cómo se renderiza
 * en la card (color, palabra mostrada). Tipos diferentes tienen colores
 * diferentes para diferenciarse visualmente al echar un ojo rápido al feed.
 */
fun postTypeTagLabel(type: PostType): String = when (type) {
    PostType.REVIEW -> "reseñó"
    PostType.QUOTE -> "cita"
    PostType.FINISHED -> "terminó"
    PostType.READING -> "está leyendo"
}