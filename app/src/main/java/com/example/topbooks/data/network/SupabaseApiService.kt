package com.example.topbooks.data.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

data class SupabaseBookDto(
    val isbn: String? = null,
    val product_name: String? = null,
    val author: String? = null,
    val publisher: String? = null,
    val description: String? = null,
    val large_image: String? = null,
    val aw_image_url: String? = null,
    val merchant_image_url: String? = null,
    val aw_deep_link: String? = null,
    val merchant_deep_link: String? = null,
    val merchant_name: String? = null,
    val language: String? = null
)

interface SupabaseApiService {

    @GET("books")
    suspend fun getBookByIsbn(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("isbn") isbn: String,
        @Query("limit") limit: Int = 1
    ): Response<List<SupabaseBookDto>>
}