package com.example.topbooks.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "https://www.googleapis.com/books/v1/"

    // Creamos la instancia de Retrofit (El motor de conexión)
    val instance: BooksApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // Usamos Gson para entender el JSON
            .build()
            .create(BooksApiService::class.java)
    }
}