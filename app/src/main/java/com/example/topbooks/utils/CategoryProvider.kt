package com.example.topbooks.utils

import com.example.topbooks.R
import java.util.Locale

/**
 * Modelo de datos que vincula una categoría con sus recursos visuales.
 * * @property iconRes Identificador del recurso gráfico (Drawable) representativo.
 * @property nameRes Identificador del recurso de cadena (String) para el nombre localizado.
 */
data class CategoryData(
    val iconRes: Int,
    val nameRes: Int?
)

/**
 * Proveedor centralizado de categorías y géneros literarios.
 * * Este objeto gestiona el mapeo entre las etiquetas de texto (a menudo inconsistentes)
 * provenientes de APIs externas y los recursos internos de la aplicación.
 */
object CategoryProvider {

    /**
     * Lista maestra de identificadores de categoría.
     * Utilizada principalmente para la inicialización de perfiles o filtros de búsqueda.
     */
    val allCategories = listOf(
        "ROMANCE", "ADVENTURE", "BIBLIOGRAPHY", "SCIFI", "COMEDY",
        "DOCUMENTARY", "FANTASY", "PHILOSOPHY", "HISTORY", "HORROR",
        "KIDS", "MANGA", "THRILLER", "GRAPHIC_NOVEL", "POETRY",
        "RELIGION"
    )

    /**
     * Mapea un código de texto a un objeto [CategoryData].
     * * Implementa lógica de normalización para aceptar múltiples variantes de escritura,
     * idiomas y formatos (con o sin guiones bajos).
     * * @param code El nombre del género tal como viene de la fuente de datos.
     * @return Objeto con los recursos de icono y texto correspondientes.
     */
    fun getCategoryResources(code: String): CategoryData {
        return when (code.uppercase(Locale.ROOT).trim()) {
            "ROMANCE" -> CategoryData(R.drawable.cat_romance_icon, R.string.cat_romance_text)

            "ADVENTURE", "AVENTURA", "AVENTURAS" ->
                CategoryData(R.drawable.cat_aventura_icon, R.string.cat_aventura_text)

            "BIBLIOGRAPHY", "BIBLIOGRAFIA", "BIBLIOGRAFÍA" ->
                CategoryData(R.drawable.cat_bibliografia_icon, R.string.cat_bibliografia_text)

            "SCIFI", "SCIENCE FICTION", "SCIENCE_FICTION", "CIENCIA_FICCION", "CIENCIA FICCION" ->
                CategoryData(R.drawable.cat_ciencia_ficcion_icon, R.string.cat_ciencia_ficcion_text)

            "COMEDY", "COMEDIA" ->
                CategoryData(R.drawable.cat_comedia_icon, R.string.cat_comedia_text)

            "DOCUMENTARY", "DOCUMENTAL" ->
                CategoryData(R.drawable.cat_documental_icon, R.string.cat_documental_text)

            "FANTASY", "FANTASIA", "FANTASÍA" ->
                CategoryData(R.drawable.cat_fantasia_icon, R.string.cat_fantasia_text)

            "PHILOSOPHY", "FILOSOFIA", "FILOSOFÍA" ->
                CategoryData(R.drawable.cat_filosofia_icon, R.string.cat_filosofia_text)

            "HISTORY", "HISTORIA" ->
                CategoryData(R.drawable.cat_historia_icon, R.string.cat_historia_text)

            "HORROR" ->
                CategoryData(R.drawable.cat_horror_icon, R.string.cat_horror_text)

            "KIDS", "CHILDREN", "INFANTIL" ->
                CategoryData(R.drawable.cat_infantil_icon, R.string.cat_infantil_text)

            "MANGA" ->
                CategoryData(R.drawable.cat_manga_icon, R.string.cat_manga_text)

            "THRILLER", "MISTERIO", "MYSTERY" ->
                CategoryData(R.drawable.cat_misterio_icon, R.string.cat_misterio_text)

            "GRAPHIC_NOVEL", "GRAPHIC NOVEL", "NOVELA_GRAFICA", "NOVELA GRAFICA" ->
                CategoryData(R.drawable.cat_novela_grafica_icon, R.string.cat_novela_grafica_text)

            "POETRY", "POESIA", "POESÍA" ->
                CategoryData(R.drawable.cat_poesia_icon, R.string.cat_poesia_text)

            "RELIGION", "RELIGIÓN" ->
                CategoryData(R.drawable.cat_religion_icon, R.string.cat_religion_text)

            // Caso por defecto para géneros no registrados o errores de API
            else -> CategoryData(R.drawable.home_icon, null)
        }
    }

    /**
     * Genera un nombre legible a partir de un código de género crudo.
     * * Se utiliza cuando [getCategoryResources] no encuentra una coincidencia directa.
     * Convierte códigos como "HISTORICAL_FICTION" en "Historical fiction".
     * * @param code El string original de la categoría.
     * @return El texto formateado para su visualización.
     */
    fun formatFallbackName(code: String): String {
        return code.lowercase()
            .replace("_", " ")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }
}