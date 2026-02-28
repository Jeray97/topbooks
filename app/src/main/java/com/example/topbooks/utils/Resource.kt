package com.example.topbooks.utils

/**
 * Una clase sellada (sealed class) que nos ayuda a gestionar los estados de la UI.
 * T: Es el tipo de dato que esperamos recibir cuando to-do va bien (ej: un Usuario, un Boolean, etc).
 */
sealed class Resource<out T> {
    // Cuando la operación fue exitosa y traemos datos
    data class Success<out T>(val data: T) : Resource<T>()

    // Cuando ocurrió un fallo
    data class Error(val exception: Throwable) : Resource<Nothing>()

    // Cuando estamos esperando respuesta (para mostrar el spinner de carga)
    object Loading : Resource<Nothing>()

    // Estado inicial (cuando no ha pasado nada aún)
    object Idle : Resource<Nothing>()
}