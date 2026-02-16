package com.example.topbooks.data.network

import com.example.topbooks.data.model.BookItem
import com.example.topbooks.data.model.GoogleBooksResponse
import com.example.topbooks.data.model.OpenLibrarySearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface BooksApiService {

    // --- GOOGLE BOOKS ---
    @GET("https://www.googleapis.com/books/v1/volumes")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("key") apiKey: String,
        @Query("maxResults") maxResults: Int = 40,
        @Query("orderBy") orderBy: String = "relevance",
        @Query("langRestrict") lang: String = "es",
        @Query("printType") printType: String = "books"
    ): Response<GoogleBooksResponse>

    @GET("https://www.googleapis.com/books/v1/volumes/{id}")
    suspend fun getBookDetail(
        @retrofit2.http.Path("id") id: String,
        @Query("key") apiKey: String
    ): Response<BookItem>

    @GET
    suspend fun getBookDetailGoogle(@Url url: String): Response<BookItem>

    // --- OPEN LIBRARY (CORREGIDO) ---
    // Hemos quitado 'first_publish_year' como parámetro suelto para evitar el error 500.
    // Todo el filtro irá dentro de 'q'.
    @GET("https://openlibrary.org/search.json")
    suspend fun searchBooksOpenLibrary(
        @Query("q") query: String,       // ej: "language:spa first_publish_year:[2020 TO 2026]"
        @Query("sort") sort: String,     // "rating"
        @Query("limit") limit: Int
    ): Response<OpenLibrarySearchResponse>

    @GET
    suspend fun searchAuthorExternal(@Url url: String): Response<OpenLibrarySearchResponse>
}