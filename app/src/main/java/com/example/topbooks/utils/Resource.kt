package com.example.topbooks.utils

/**
 * REPRESENTADOR DE ESTADO DE RECURSOS (Generic State Wrapper).
 * * Esta clase sellada se utiliza para encapsular la comunicación de datos entre
 * las capas de la aplicación (Repository -> ViewModel -> UI).
 * Provee una forma estructurada de manejar operaciones asíncronas.
 *
 * @param T El tipo de dato esperado en caso de éxito.
 */
sealed class Resource<out T> {

    /**
     * Representa un estado de éxito donde se han obtenido los datos correctamente.
     * @property data El contenido del recurso de tipo [T].
     */
    data class Success<out T>(val data: T) : Resource<T>()

    /**
     * Representa un fallo en la operación o una excepción de red/base de datos.
     * @property exception El error capturado durante la ejecución.
     */
    data class Error(val exception: Throwable) : Resource<Nothing>()

    /**
     * Indica que el recurso se está cargando o procesando actualmente.
     * Se utiliza para activar indicadores de progreso en la UI.
     */
    object Loading : Resource<Nothing>()

    /**
     * Estado inicial o de espera.
     * Indica que la operación aún no se ha disparado.
     */
    object Idle : Resource<Nothing>()
}