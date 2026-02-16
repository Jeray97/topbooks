package com.example.topbooks.data.network

import com.example.topbooks.data.model.BookItem
import com.example.topbooks.data.model.GoogleBooksResponse
import com.example.topbooks.data.model.OpenLibrarySearchResponse
import com.example.topbooks.data.model.OpenLibraryWorkDetail
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface BooksApiService {

    // --- GOOGLE BOOKS ---
    @GET("https://www.googleapis.com/books/v1/volumes")
    suspend fun searchBooksGoogle(
        @Query("q") query: String,
        @Query("key") apiKey: String,
        @Query("maxResults") maxResults: Int = 40,
        @Query("langRestrict") lang: String = "es",
        @Query("printType") printType: String = "books"
    ): Response<GoogleBooksResponse>

    @GET("https://www.googleapis.com/books/v1/volumes/{id}")
    suspend fun getBookDetailGoogle(
        @Path("id") id: String,
        @Query("key") apiKey: String
    ): Response<BookItem>

    // --- OPEN LIBRARY ---
    @GET("https://openlibrary.org/search.json")
    suspend fun searchBooksOpenLibrary(
        @Query("q") query: String,
        @Query("sort") sort: String? = null,
        @Query("limit") limit: Int = 20
    ): Response<OpenLibrarySearchResponse>

    // Endpoint para el detalle
    @GET("https://openlibrary.org/works/{id}.json")
    suspend fun getWorkDetailOpenLibrary(
        @Path("id") id: String
    ): Response<OpenLibraryWorkDetail>

    @GET
    suspend fun searchAuthorExternal(@Url url: String): Response<OpenLibrarySearchResponse>
}