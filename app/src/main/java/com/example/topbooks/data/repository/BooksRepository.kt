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

    // ... (Métodos getBooks y searchHybrid igual que antes) ...
    // Solo pongo getBooks y searchHybrid resumidos para contexto,
    // pero el foco es getBookDetail abajo.

    suspend fun getBooks(query: String, orderBy: String = "relevance", filterModern: Boolean = false): Result<List<Book>> {
        return try {
            val langCode = if (Locale.getDefault().language == "es") "spa" else "eng"
            var finalQuery = "$query language:$langCode"

            if (filterModern) {
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                val startYear = currentYear - 3
                finalQuery += " first_publish_year:[$startYear TO $currentYear]"
            }

            val sortParam = if (orderBy == "rating") "rating" else null

            val response = apiService.searchBooksOpenLibrary(finalQuery, sortParam, 20)

            if (response.isSuccessful) {
                val books = response.body()?.docs?.map { it.toDomain() } ?: emptyList()
                Result.success(books.filter { it.imageUrl.isNotEmpty() })
            } else {
                Result.failure(Exception("OpenLib Error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchHybrid(query: String): Result<List<Book>> = coroutineScope {
        try {
            val lang = Locale.getDefault().language
            // CORRECCIÓN: Cambiado 'langRestrict=lang' por 'lang=lang'
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

    // --- CORRECCIÓN DEL ERROR DE DETALLE ---
    suspend fun getBookDetail(id: String): Result<Book> {
        return try {
            if (id.startsWith("OL")) {
                val response = apiService.getWorkDetailOpenLibrary(id)
                if (response.isSuccessful) {
                    val work = response.body() // OpenLibraryWorkDetail

                    // CORRECCIÓN: Tratamos 'description' como Any?
                    // OpenLibrary devuelve la descripción como String O como Map { "type": "text", "value": "..." }
                    val descriptionText = if (work?.description != null) {
                        if (work.description is String) {
                            work.description
                        } else if (work.description is Map<*, *>) {
                            // Si es un mapa, intentamos sacar el valor "value"
                            work.description["value"] as? String ?: "Sin descripción."
                        } else {
                            "Sin descripción."
                        }
                    } else {
                        "Sin descripción."
                    }

                    // CORRECCIÓN: Acceso seguro a covers (List<Int>?)
                    val cover = work?.covers?.firstOrNull()?.let {
                        "https://covers.openlibrary.org/b/id/$it-L.jpg"
                    } ?: ""

                    val book = Book(
                        id = id,
                        title = work?.title ?: "Sin título",
                        authors = emptyList(), // Work API no da autores fácil, dejamos vacío
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