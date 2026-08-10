package com.example.topbooks.data.network

import com.example.topbooks.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object SupabaseRetrofitClient {

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("${BuildConfig.SUPABASE_URL}/rest/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val instance: SupabaseApiService by lazy {
        retrofit.create(SupabaseApiService::class.java)
    }
}