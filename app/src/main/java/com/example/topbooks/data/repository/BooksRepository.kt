package com.example.topbooks.data.repository

import android.util.Log
import com.example.topbooks.BuildConfig
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.network.RetrofitClient
import java.util.Calendar
import java.util.Locale

class BooksRepository {

    private val apiService = RetrofitClient.instance
    private val API_KEY = BuildConfig.API_KEY

    // --- 1. GOOGLE BOOKS (Categorías, Buscador, Scanner) ---
    suspend fun getBooks(
        query: String,
        orderBy: String = "relevance",
        filterModern: Boolean = false
    ): Result<List<Book>> {
        return try {
            val language = Locale.getDefault().language

            val response = apiService.searchBooks(
                query = query,
                orderBy = orderBy,
                apiKey = API_KEY,
                lang = language,
                maxResults = 40
            )

            if (response.isSuccessful) {
                var books = response.body()?.items?.map { it.toDomain() } ?: emptyList()

                // Filtros locales
                books = books.filter { it.imageUrl.isNotEmpty() && it.authors.isNotEmpty() }

                if (filterModern) {
                    books = books.filter { book ->
                        val year = book.lanzamiento.take(4).toIntOrNull() ?: 0
                        year >= 2010
                    }
                }
                Result.success(books)
            } else {
                Result.failure(Exception("Google Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- 2. OPEN LIBRARY (Recomendados - CORREGIDO) ---
    suspend fun getBestRatedModernBooks(): Result<List<Book>> {
        return try {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            // CAMBIO: Calculamos dinámicamente los últimos 3 años
            val startYear = currentYear - 3

            // Detectamos idioma (spa, eng, fre...)
            val langCode = if (Locale.getDefault().language == "es") "spa" else "eng"

            // CONSTRUCCIÓN DE LA QUERY SEGURA
            // Sintaxis: language:spa first_publish_year:[2023 TO 2026]
            val finalQuery = "language:$langCode first_publish_year:[$startYear TO $currentYear]"

            Log.d("REPO", "OpenLibrary Query: $finalQuery")

            val response = apiService.searchBooksOpenLibrary(
                query = finalQuery,
                sort = "rating",
                limit = 20
            )

            if (response.isSuccessful) {
                var books = response.body()?.docs?.map { it.toDomain() } ?: emptyList()

                // Filtro extra: OpenLibrary a veces trae cosas sin portada
                books = books.filter { it.imageUrl.isNotEmpty() }

                Result.success(books)
            } else {
                // Si OpenLibrary falla (500, 503), lanzamos excepción para que el ViewModel
                // capture y haga fallback a Google Books si quieres implementarlo allí.
                Result.failure(Exception("OpenLib Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- DETALLE (Google Books) ---
    suspend fun getBookDetail(id: String): Result<Book> {
        return try {
            // Si el ID no parece de Google (no es alfanumérico corto), podría fallar.
            // Google IDs suelen ser como "zyTCAlFPjgYC". OpenLib son "OL27349W".
            // De momento intentamos Google.
            val response = apiService.getBookDetail(id = id, apiKey = API_KEY)

            if (response.isSuccessful) {
                val item = response.body()
                if (item != null) Result.success(item.toDomain())
                else Result.failure(Exception("Libro vacío"))
            } else {
                Result.failure(Exception("Error Detalle: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}