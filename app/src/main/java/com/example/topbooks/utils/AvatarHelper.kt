package com.example.topbooks.utils

import com.example.topbooks.R

// Objeto para gestionar los avatares disponibles
object AvatarHelper {

    // Lista de todos los avatares disponibles para elegir
    // Asegúrate de tener estas imágenes en res/drawable
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

        Pair("default", R.drawable.capibara_1)
    )

    // Función para obtener el ID del recurso a partir del nombre guardado en Firebase
    fun getDrawableId(avatarName: String?): Int {
        return avatars.find { it.first == avatarName }?.second
            ?: R.drawable.capibara_1 // Fallback por defecto
    }
}