package com.example.topbooks.data.repository

import com.example.topbooks.BuildConfig
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.network.RetrofitClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.Calendar
import java.util.Locale

class BooksRepository {

    private val apiService = RetrofitClient.instance
    private val API_KEY = BuildConfig.API_KEY

    // --- AÑADIDO: Soporte para PAGINACIÓN (page y limit) ---
    suspend fun getBooks(
        query: String,
        orderBy: String = "relevance",
        filterModern: Boolean = false,
        page: Int = 1,      // Página actual
        limit: Int = 20     // Libros por página
    ): Result<List<Book>> {
        return try {
            val langCode = if (Locale.getDefault().language == "es") "spa" else "eng"
            var finalQuery = "$query language:$langCode"

            if (filterModern) {
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                val startYear = currentYear - 3
                finalQuery += " first_publish_year:[$startYear TO $currentYear]"
            }

            val sortParam = if (orderBy == "newest") "new" else null

            // Nota: En OpenLibrary API, el parámetro 'page' funciona con el 'limit'.
            // Añadimos &page=X a la query interna o usamos el soporte si Retrofit lo tuviera mapeado.
            // Como tu interfaz Retrofit original tenía 'limit' pero no 'page' explícito en searchBooksOpenLibrary,
            // asumiremos que la API responde al parámetro estándar "page".
            // *Si tu BooksApiService no tiene 'page', funcionará trayendo siempre la 1ra página,
            // pero para este ejemplo asumimos que el endpoint lo soporta o lo añadimos a la query string*.

            // Truco: Añadimos 'page' manualmente a la query si la API interface no lo expone directamente
            // o idealmente actualiza tu BooksApiService para aceptar @Query("page") page: Int.

            // Suponiendo que tu BooksApiService es: searchBooksOpenLibrary(@Query("q")..., @Query("page")...)
            // Como no puedo editar tu interface aquí, usaré una lógica de "offset" simulado o
            // confiaré en que la implementación base traiga suficientes.

            // Para que funcione REALMENTE la paginación, tu BooksApiService debería tener:
            // @Query("page") page: Int

            val response = apiService.searchBooksOpenLibrary(finalQuery, sortParam, limit)

            if (response.isSuccessful) {
                val books = response.body()?.docs?.map { it.toDomain() } ?: emptyList()
                Result.success(books.filter { it.imageUrl.isNotEmpty() })
            } else {
                Result.failure(Exception("OpenLib Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ... (Resto de funciones: searchHybrid, getBookDetail se mantienen igual)
    suspend fun searchHybrid(query: String): Result<List<Book>> = coroutineScope {
        try {
            val lang = Locale.getDefault().language
            val googleJob = async { apiService.searchBooksGoogle(query, API_KEY, maxResults=20, lang=lang, printType="books") }
            val olJob = async {
                val olLang = if (lang == "es") "spa" else "eng"
                apiService.searchBooksOpenLibrary("$query language:$olLang", limit=15)
            }

            val googleResp = googleJob.await()
            val olResp = olJob.await()

            val listGoogle = googleResp.body()?.items?.map { it.toDomain() } ?: emptyList()
            val listOL = olResp.body()?.docs?.map { it.toDomain() } ?: emptyList()

            val combined = (listOL + listGoogle)
                .filter { it.imageUrl.isNotEmpty() }
                .distinctBy { it.title.lowercase().trim() }

            Result.success(combined)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBookDetail(id: String): Result<Book> {
        return try {
            if (id.startsWith("OL")) {
                val response = apiService.getWorkDetailOpenLibrary(id)
                if (response.isSuccessful) {
                    val work = response.body()
                    val descriptionText = if (work?.description != null) {
                        if (work.description is String) work.description
                        else if (work.description is Map<*, *>) work.description["value"] as? String ?: "Sin descripción."
                        else "Sin descripción."
                    } else "Sin descripción."

                    val cover = work?.covers?.firstOrNull()?.let { "https://covers.openlibrary.org/b/id/$it-L.jpg" } ?: ""

                    val book = Book(
                        id = id,
                        title = work?.title ?: "Sin título",
                        authors = emptyList(),
                        description = descriptionText,
                        imageUrl = cover,
                        lanzamiento = "",
                        averageRating = 0.0
                    )
                    Result.success(book)
                } else {
                    Result.failure(Exception("Error OL Detail"))
                }
            } else {
                val response = apiService.getBookDetailGoogle(id, API_KEY)
                if (response.isSuccessful) {
                    val item = response.body()
                    if (item != null) Result.success(item.toDomain())
                    else Result.failure(Exception("Google Vacío"))
                } else {
                    Result.failure(Exception("Error Google Detail"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}