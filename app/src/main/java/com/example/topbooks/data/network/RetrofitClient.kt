package com.example.topbooks.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Cliente Singleton (única instancia) de Retrofit.
 * * Es el motor principal que gestiona las conexiones HTTP de la aplicación hacia el exterior.
 * Al usar la palabra clave 'object', Kotlin asegura que solo exista una instancia
 * de este cliente durante to-do el ciclo de vida de la app, optimizando los recursos.
 */
object RetrofitClient {

    /**
     * URL base por defecto para las peticiones de red.
     * * Nota: Aunque aquí definimos la de Google Books, Retrofit es capaz de ignorarla
     * si en la interfaz [BooksApiService] le pasamos una URL completa (como hacemos con Open Library).
     */
    private const val BASE_URL = "https://www.googleapis.com/books/v1/"


    /**
     * Instancia construida y lista para usar de [BooksApiService].
     * * Se inicializa de forma "perezosa" (by lazy), lo que significa que Retrofit no consumirá
     * memoria construyendo el cliente hasta que el usuario intente hacer la primera búsqueda de un libro.
     */
    val instance: BooksApiService by lazy {
        Retrofit.Builder()
            // 1. Establecemos la URL base
            .baseUrl(BASE_URL)
            // 2. Le indicamos a Retrofit que use Gson para traducir los JSON a nuestras Data Classes
            .addConverterFactory(GsonConverterFactory.create())
            // 3. Ensamblamos el motor
            .build()
            // 4. Lo vinculamos con nuestra interfaz para que sepa qué rutas existen
            .create(BooksApiService::class.java)
    }
}