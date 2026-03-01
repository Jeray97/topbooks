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

    suspend fun getBooks(
        query: String,
        orderBy: String = "relevance",
        filterModern: Boolean = false,
        page: Int = 1,
        limit: Int = 20
    ): Result<List<Book>> {
        return try {
            val langCode = Locale.getDefault().language
            val startIndex = (page - 1) * limit

            var finalQuery = query

            if (filterModern) {
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                val lastYear = currentYear - 1
                finalQuery = "$query $lastYear"
            }

            val response = apiService.searchBooksGoogle(
                query = finalQuery,
                apiKey = API_KEY,
                startIndex = startIndex,
                // 🔥 Pedimos el máximo permitido a Google (40) para tener de sobra tras filtrar
                maxResults = 40,
                orderBy = "relevance",
                lang = langCode
            )

            if (response.isSuccessful) {
                var books = response.body()?.items?.map { it.toDomain() } ?: emptyList()
                // Solo aceptamos libros con portada y autor
                books = books.filter { it.imageUrl.isNotEmpty() && it.authors.isNotEmpty() }

                // 🔥 APLICAMOS EL FILTRO INTELIGENTE DE VARIEDAD
                // Solo lo aplicamos en las secciones de recomendados y categorías, no en búsquedas directas
                if (filterModern || query.contains("subject:")) {
                    books = applyVarietyFilter(books)
                }

                // Devolvemos la cantidad exacta que nos pidió la pantalla
                Result.success(books.take(limit))
            } else {
                Result.failure(Exception("Google API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- EL CEREBRO DEL FILTRO DE VARIEDAD ---
    private fun applyVarietyFilter(books: List<Book>): List<Book> {
        val filteredList = mutableListOf<Book>()
        val authorCounts = mutableMapOf<String, Int>()

        for (book in books) {
            val author = book.authors.firstOrNull() ?: "Unknown"

            // 1. Limpiamos el título de símbolos y sacamos las palabras clave (>2 letras)
            val cleanTitle = book.title.lowercase().replace(Regex("[^a-z0-9áéíóúñ ]"), "")
            val words = cleanTitle.split(" ").filter { it.length > 2 } // ignoramos la, el, y, de...

            // 2. Tomamos las 2 primeras palabras fuertes como "Identificador de Saga"
            val prefix = words.take(2).joinToString(" ")

            val authorCount = authorCounts.getOrDefault(author, 0)

            // 3. Comprobamos si ya añadimos un libro con este mismo identificador (Ej: "harry potter")
            val hasSameSaga = filteredList.any { existingBook ->
                val existingClean = existingBook.title.lowercase().replace(Regex("[^a-z0-9áéíóúñ ]"), "")
                val existingWords = existingClean.split(" ").filter { it.length > 2 }
                val existingPrefix = existingWords.take(2).joinToString(" ")

                prefix.isNotEmpty() && prefix == existingPrefix
            }

            // 4. Regla estricta: Máximo 2 libros del mismo autor y NUNCA de la misma saga
            if (authorCount < 2 && !hasSameSaga) {
                filteredList.add(book)
                authorCounts[author] = authorCount + 1
            }
        }

        // Medida de seguridad: Si el filtro fue demasiado agresivo, devolvemos la lista original sin duplicados
        if (filteredList.size < 4 && books.size >= 4) {
            return books.distinctBy { it.title.lowercase().trim() }
        }

        return filteredList
    }

    suspend fun searchHybrid(query: String): Result<List<Book>> = coroutineScope {
        try {
            val lang = Locale.getDefault().language
            val googleJob = async { apiService.searchBooksGoogle(query, API_KEY, startIndex = 0, maxResults = 20, orderBy = "relevance", lang = lang, printType = "books") }
            val olJob = async {
                val olLang = if (lang == "es") "spa" else "eng"
                apiService.searchBooksOpenLibrary("$query language:$olLang", limit = 15)
            }

            val googleResp = googleJob.await()
            val olResp = olJob.await()

            val listGoogle = googleResp.body()?.items?.map { it.toDomain() } ?: emptyList()
            val listOL = olResp.body()?.docs?.map { it.toDomain() } ?: emptyList()

            // En la barra de búsqueda manual SÍ dejamos que salgan libros de la misma saga
            val combined = (listGoogle + listOL)
                .filter { it.imageUrl.isNotEmpty() && it.authors.isNotEmpty() }
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