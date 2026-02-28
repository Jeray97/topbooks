package com.example.topbooks.utils

import com.example.topbooks.R
import java.util.Locale

// Modelo de datos para manejar el icono y el texto limpio
data class CategoryData(
    val iconRes: Int,
    val nameRes: Int?
)

object CategoryProvider {

    // Lista completa de géneros basada en tus strings.
    // Útil para iterar sobre ellos en pantallas de búsqueda o tutoriales.
    val allCategories = listOf(
        "ROMANCE", "ADVENTURE", "BIBLIOGRAPHY", "SCIFI", "COMEDY",
        "DOCUMENTARY", "FANTASY", "PHILOSOPHY", "HISTORY", "HORROR",
        "KIDS", "MANGA", "THRILLER", "GRAPHIC_NOVEL", "POETRY",
        "RELIGION"
    )

    fun getCategoryResources(code: String): CategoryData {
        return when (code.uppercase(Locale.ROOT).trim()) {
            "ROMANCE" -> CategoryData(R.drawable.cat_romance_icon, R.string.cat_romance_text)

            "ADVENTURE", "AVENTURA", "AVENTURAS" -> CategoryData(R.drawable.cat_aventura_icon, R.string.cat_aventura_text)

            "BIBLIOGRAPHY", "BIBLIOGRAFIA", "BIBLIOGRAFÍA" -> CategoryData(R.drawable.cat_bibliografia_icon, R.string.cat_bibliografia_text)

            "SCIFI", "SCIENCE FICTION", "SCIENCE_FICTION", "CIENCIA_FICCION", "CIENCIA FICCION" ->
                CategoryData(R.drawable.cat_ciencia_ficcion_icon, R.string.cat_ciencia_ficcion_text)

            "COMEDY", "COMEDIA" -> CategoryData(R.drawable.cat_comedia_icon, R.string.cat_comedia_text)

            "DOCUMENTARY", "DOCUMENTAL" -> CategoryData(R.drawable.cat_documental_icon, R.string.cat_documental_text)

            "FANTASY", "FANTASIA", "FANTASÍA" -> CategoryData(R.drawable.cat_fantasia_icon, R.string.cat_fantasia_text)

            "PHILOSOPHY", "FILOSOFIA", "FILOSOFÍA" -> CategoryData(R.drawable.cat_filosofia_icon, R.string.cat_filosofia_text)

            "HISTORY", "HISTORIA" -> CategoryData(R.drawable.cat_historia_icon, R.string.cat_historia_text)

            "HORROR" -> CategoryData(R.drawable.cat_horror_icon, R.string.cat_horror_text)

            "KIDS", "CHILDREN", "INFANTIL" -> CategoryData(R.drawable.cat_infantil_icon, R.string.cat_infantil_text)

            "MANGA" -> CategoryData(R.drawable.cat_manga_icon, R.string.cat_manga_text)

            "THRILLER", "MISTERIO", "MYSTERY" -> CategoryData(R.drawable.cat_misterio_icon, R.string.cat_misterio_text)

            "GRAPHIC_NOVEL", "GRAPHIC NOVEL", "NOVELA_GRAFICA", "NOVELA GRAFICA" ->
                CategoryData(R.drawable.cat_novela_grafica_icon, R.string.cat_novela_grafica_text)

            "POETRY", "POESIA", "POESÍA" -> CategoryData(R.drawable.cat_poesia_icon, R.string.cat_poesia_text)

            "RELIGION", "RELIGIÓN" -> CategoryData(R.drawable.cat_religion_icon, R.string.cat_religion_text)

            //"ART", "ARTE" -> CategoryData(R.drawable.cat_arte_icon, R.string.cat_arte_text)

            // Fallback por si la API envía un género no registrado
            else -> CategoryData(R.drawable.home_icon, null)
        }
    }

    // Por si devuelve NULL el nameRes, esto formatea el texto en crudo para que quede bonito
    fun formatFallbackName(code: String): String {
        return code.lowercase()
            .replace("_", " ")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }
}