package com.example.topbooks.utils

object SeriesDetector {

    data class SeriesInfo(
        val name: String,
        val index: Int
    )

    fun detect(title: String): SeriesInfo? {
        val cleanTitle = title.lowercase().trim()

        // LISTA DE PATRONES ORDENADOS POR PRIORIDAD
        val patterns = listOf(
            // 1. Busca "(CualquierSaga 3)" en cualquier parte del título
            Regex("\\((.+?)\\s+(\\d+)\\)"),

            // 2. Busca "- CualquierSaga #3" en cualquier parte
            Regex("-\\s*(.+?)\\s*#(\\d+)"),

            // 3. Busca "(Book 3)" o "(Libro 3)"
            Regex("(.*)\\s*\\((?:book|libro)\\s*(\\d+)\\)", RegexOption.IGNORE_CASE),

            // 4. Busca "Texto #3" al final
            Regex("(.*)\\s*#(\\d+)"),

            // 5. Busca "Texto Vol 3" al final
            Regex("(.*)\\s(vol\\.?|volume)\\s*(\\d+)", RegexOption.IGNORE_CASE)
        )

        for (regex in patterns) {
            val match = regex.find(cleanTitle)

            if (match != null) {
                // Extraemos el nombre (Grupo 1) y el número (Último grupo)
                var name = match.groupValues[1].replace(Regex(".*-\\s*"), "").trim()
                val index = match.groupValues.last().toIntOrNull() ?: 0

                // Filtro de seguridad: si el nombre extraído es muy corto o es solo un año/número, lo ignoramos
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