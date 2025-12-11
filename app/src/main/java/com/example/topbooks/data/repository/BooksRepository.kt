package com.example.topbooks.data.repository

// Importamos BuildConfig para acceder a la clave secreta
import android.util.Log
import com.example.topbooks.BuildConfig
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.network.RetrofitClient
import java.util.Locale

class BooksRepository {

    private val apiService = RetrofitClient.instance

    //Usamos la clave que inyectamos desde local.properties
    private val API_KEY = BuildConfig.API_KEY

    suspend fun getBooks(query: String, orderBy: String = "relevance"): Result<List<Book>> {
        return try {
            // Detectamos idioma del usuario
            val language = Locale.getDefault().language

            Log.d("Query", query)

            val response = apiService.searchBooks(
                query = query,
                orderBy = orderBy,
                apiKey = API_KEY,
                lang = language
            )

            if (response.isSuccessful) {
                // Mapeamos a nuestro modelo limpio
                val books = response.body()?.items?.map { it.toDomain() } ?: emptyList()
                Result.success(books)
            } else {
                Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBookDetail(id: String): Result<Book> {
        return try {
            val response = apiService.getBookDetail(
                id = id,
                apiKey = API_KEY
            )

            if (response.isSuccessful) {
                // response.body() es un BookItem único
                val bookItem = response.body()
                if (bookItem != null) {
                    Result.success(bookItem.toDomain())
                } else {
                    Result.failure(Exception("El libro llegó vacío"))
                }
            } else {
                Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}