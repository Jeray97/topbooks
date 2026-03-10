package com.example.topbooks.utils

import com.example.topbooks.R

/**
 * OBJETO DE UTILIDAD PARA GESTIÓN DE AVATARES.
 * * Este objeto centraliza la lógica de mapeo entre los identificadores de texto guardados
 * en la base de datos (Firebase) y los recursos gráficos (Drawables) locales.
 */
object AvatarHelper {

    /**
     * Lista maestra de avatares disponibles en la aplicación.
     * Cada elemento es un [Pair] que vincula el nombre técnico con el recurso gráfico.
     * * Nota: Los archivos deben estar presentes en res/drawable con los nombres exactos.
     */
    val avatars = listOf(
        Pair("capibara_1", R.drawable.capibara_1),
        Pair("capibara_2", R.drawable.capibara_2),
        Pair("capibara_3", R.drawable.capibara_3),
        Pair("capibara_4", R.drawable.capibara_4),
        Pair("capibara_5", R.drawable.capibara_5),
        Pair("capibara_6", R.drawable.capibara_6),
        Pair("capibara_7", R.drawable.capibara_7),
        Pair("capibara_8", R.drawable.capibara_8),
        Pair("capibara_9", R.drawable.capibara_9),
        Pair("capibara_10", R.drawable.capibara_10),
    )

    /**
     * Recupera el identificador de recurso drawable asociado a un nombre de avatar.
     * * @param avatarName El nombre del avatar (String) recuperado del perfil de usuario.
     * @return El ID numérico del recurso (Int). Si no se encuentra, devuelve el avatar por defecto.
     */
    fun getDrawableId(avatarName: String?): Int {
        return avatars.find { it.first == avatarName }?.second
            ?: R.drawable.capibara_1 // Fallback de seguridad
    }
}