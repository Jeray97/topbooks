package com.example.topbooks.data.network

import com.example.topbooks.data.model.BookItem
import com.example.topbooks.data.model.GoogleBooksResponse
import com.example.topbooks.data.model.OpenLibrarySearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface BooksApiService {

    // Definimos la petición GET a la url "volumes" (que es donde Google tiene los libros)
    @GET("volumes")
    suspend fun searchBooks(
        @Query("q") query: String, // Lo que buscamos (ej: "Harry Potter")
        @Query("key") apiKey: String, // Clave API
        @Query("maxResults") maxResults: Int = 10, // Cuántos libros queremos (por defecto 10)
        @Query("orderBy") orderBy: String = "relevance", //Por defecto los más relevantes
        @Query("langRestrict") lang: String = "es" // Para que salgan en español (Predeterminado)
    ): Response<GoogleBooksResponse>

    // --- NUEVO: Función para buscar autor en Open Library ---
    @GET
    suspend fun searchAuthorExternal(
        @Url url: String
    ): Response<OpenLibrarySearchResponse>

    // La url final será: https://www.googleapis.com/books/v1/volumes/{id}
    @GET("volumes/{id}")
    suspend fun getBookDetail(
        @retrofit2.http.Path("id") id: String,
        @Query("key") apiKey: String
    ): Response<BookItem>
}

