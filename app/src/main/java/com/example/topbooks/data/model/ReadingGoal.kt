package com.example.topbooks.data.model

/**
 * Modelo para el objetivo de lectura anual del usuario.
 * 
 * @property year Año del objetivo (ej: 2024)
 * @property targetBooks Número de libros que el usuario quiere leer este año
 * @property booksRead Número de libros leídos hasta ahora este año
 */
data class ReadingGoal(
    val year: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
    val targetBooks: Int = 0,
    val booksRead: Int = 0
) {
    /**
     * Calcula el porcentaje de progreso hacia el objetivo.
     * Retorna 0 si no hay objetivo establecido.
     */
    fun getProgressPercentage(): Int {
        if (targetBooks == 0) return 0
        val percentage = (booksRead.toFloat() / targetBooks.toFloat() * 100).toInt()
        return percentage.coerceIn(0, 100)
    }
    
    /**
     * Verifica si el objetivo ha sido completado.
     */
    fun isCompleted(): Boolean {
        return targetBooks > 0 && booksRead >= targetBooks
    }
    
    /**
     * Verifica si hay un objetivo establecido.
     */
    fun hasGoal(): Boolean {
        return targetBooks > 0
    }
}
