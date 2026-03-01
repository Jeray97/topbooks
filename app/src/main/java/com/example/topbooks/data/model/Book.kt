package com.example.topbooks.data.model

data class Book(
    val id: String,
    val title: String,
    // 🔥 Añadido para ayudar en la detección
    val subtitle: String = "",
    val authors: List<String>,
    val description: String,
    val imageUrl: String,
    val lanzamiento: String,
    val averageRating: Double = 0.0,
    val ratingsCount: Int = 0,
    val pageCount: Int = 0
) {
    // 🔥 DETECTOR INTELIGENTE DE SAGAS
    val isSaga: Boolean
        get() {
            // Unimos título y subtítulo y lo pasamos a minúsculas para analizarlo
            val fullText = "$title $subtitle".lowercase()

            // 1. Buscamos palabras clave obvias
            val sagaKeywords = listOf("saga", "serie", "trilogía", "trilogy", "crónicas", "chronicles", "colección")
            if (sagaKeywords.any { fullText.contains(it) }) return true

            // 2. Buscamos patrones de numeración comunes usando Regex
            // Detecta cosas como: "vol. 1", "volumen 2", "libro 3", "book 4", "parte 1", "part 2"
            val numberingRegex = Regex("(vol\\.|volumen|libro|book|parte|part)\\s*\\d+", RegexOption.IGNORE_CASE)
            if (numberingRegex.containsMatchIn(fullText)) return true

            // 3. Buscamos texto entre paréntesis que contenga un número
            // Muchos libros ponen la saga así: "Título del libro (Nombre de la saga, 1)"
            val parenthesesRegex = Regex("\\(.*?\\d+.*?\\)")
            if (parenthesesRegex.containsMatchIn(fullText)) return true

            // Si no cumple nada de lo anterior, asumimos que es autoconclusivo (libro único)
            return false
        }
}