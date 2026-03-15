package com.example.topbooks.utils

import androidx.core.text.HtmlCompat

/**
 * Utilidad encargada de sanitizar textos provenientes de internet.
 * Traduce etiquetas HTML a texto plano respetando los saltos de línea y párrafos.
 */
object HtmlCleaner {

    fun clean(html: String?): String {
        if (html.isNullOrBlank()) return ""

        // FROM_HTML_MODE_COMPACT es el estándar moderno para procesar HTML en Android
        return HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim()
    }
}