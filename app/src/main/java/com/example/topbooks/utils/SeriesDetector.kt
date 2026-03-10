package com.example.topbooks.utils

/**
 * UTILIDAD DE DETECCIÓN DE SAGAS Y SERIES.
 * * Analiza títulos de libros mediante expresiones regulares para identificar si
 * pertenecen a una serie y extraer su posición cronológica.
 */
object SeriesDetector {

    /**
     * Información estructurada de la serie detectada.
     * @property name Nombre normalizado de la saga.
     * @property index Número del volumen dentro de la serie.
     */
    data class SeriesInfo(
        val name: String,
        val index: Int
    )

    /**
     * Intenta detectar metadatos de serie en un título de libro.
     * * Procesa formatos comunes como:
     * - "Título (Saga 3)"
     * - "Título - Saga #3"
     * - "Título (Libro 3)"
     * - "Título Vol. 3"
     * * @param title El título completo del libro a analizar.
     * @return [SeriesInfo] si se detecta un patrón válido, de lo contrario null.
     */
    fun detect(title: String): SeriesInfo? {
        val cleanTitle = title.lowercase().trim()

        // LISTA DE PATRONES ORDENADOS POR PRIORIDAD
        val patterns = listOf(
            // 1. Busca "(NombreSaga 3)" en cualquier parte del título
            Regex("\\((.+?)\\s+(\\d+)\\)"),

            // 2. Busca "- NombreSaga #3"
            Regex("-\\s*(.+?)\\s*#(\\d+)"),

            // 3. Busca "(Book 3)" o "(Libro 3)"
            Regex("(.*)\\s*\\((?:book|libro)\\s*(\\d+)\\)", RegexOption.IGNORE_CASE),

            // 4. Busca "Texto #3" al final de la cadena
            Regex("(.*)\\s*#(\\d+)"),

            // 5. Busca "Texto Vol 3" o "Volume 3" al final
            Regex("(.*)\\s(vol\\.?|volume)\\s*(\\d+)", RegexOption.IGNORE_CASE)
        )

        for (regex in patterns) {
            val match = regex.find(cleanTitle)

            if (match != null) {
                // Limpieza del nombre: eliminamos restos de separadores y espacios
                var name = match.groupValues[1].replace(Regex(".*-\\s*"), "").trim()
                val index = match.groupValues.last().toIntOrNull() ?: 0

                // FILTRO DE SEGURIDAD
                // Si el nombre extraído es demasiado corto o es puramente numérico (como un año), se ignora
                if (name.length < 2 || name.matches(Regex("\\d+"))) {
                    continue
                }

                return SeriesInfo(
                    name.replaceFirstChar { it.uppercase() },
                    index
                )
            }
        }

        return null
    }
}