package com.example.topbooks.data.repository

import android.util.Log
import com.example.topbooks.BuildConfig
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.network.RetrofitClient
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Locale
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.awaitAll

class BooksRepository {

    private val apiService = RetrofitClient.instance
    private val API_KEY = BuildConfig.API_KEY
    private val db = FirebaseFirestore.getInstance() // 🔥 Conexión a tu Base de Datos

    suspend fun getBooks(
        query: String,
        orderBy: String = "relevance",
        filterModern: Boolean = false,
        page: Int = 1,
        limit: Int = 20
    ): Result<List<Book>> {
        return try {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)

            // ====================================================================
            // 🔥 FASE 1: BUSCAMOS EN NUESTRA PROPIA BASE DE DATOS (FIREBASE)
            // ====================================================================
            var localBooks = fetchFromFirebase(query)

            if (filterModern) {
                // Filtramos los de Firebase para que también sean modernos (Últimos 5 años)
                localBooks = localBooks.filter { book ->
                    val year = Regex("\\d{4}").find(book.lanzamiento)?.value?.toIntOrNull() ?: 0
                    year >= currentYear - 5
                }
            }

            // Si nuestra comunidad ya ha guardado al menos 4 libros de esta categoría...
            // ¡Nos ahorramos llamar a Google y mostramos los nuestros!
            if (localBooks.size >= 4) {
                return Result.success(localBooks.take(limit))
            }


            // ====================================================================
            // 🔥 FASE 2: PLAN DE EMERGENCIA (GOOGLE BOOKS)
            // Si la comunidad aún no ha guardado suficientes libros de esto, vamos a Google
            // ====================================================================
            val langCode = Locale.getDefault().language
            val startIndex = (page - 1) * limit
            var apiQuery = query

            if (filterModern) {
                apiQuery = "$query ${currentYear} OR ${currentYear - 1} OR ${currentYear - 2}"
            }

            val response = apiService.searchBooksGoogle(
                query = apiQuery,
                apiKey = API_KEY,
                startIndex = startIndex,
                maxResults = 40,
                orderBy = "relevance",
                lang = langCode
            )

            if (response.isSuccessful) {
                var books = response.body()?.items?.map { it.toDomain() } ?: emptyList()

                // ESCUDO ANTI +18 y Filtro Básico
                books = books.filter { it.imageUrl.isNotEmpty() && it.authors.isNotEmpty() && !it.isMature }

                // FILTRO DE ACTUALIDAD
                if (filterModern) {
                    var recentBooks = books.filter { book ->
                        val year = Regex("\\d{4}").find(book.lanzamiento)?.value?.toIntOrNull() ?: 0
                        year >= currentYear - 5
                    }

                    if (recentBooks.size < 5) {
                        recentBooks = books.filter { book ->
                            val year = Regex("\\d{4}").find(book.lanzamiento)?.value?.toIntOrNull() ?: 0
                            year >= currentYear - 10
                        }
                    }
                    books = recentBooks
                }

                // ORDENAMOS POR FAMA
                books = books.sortedByDescending { it.ratingsCount }

                // FILTRO ANTI-SAGAS REPETIDAS
                if (filterModern || query.contains("subject:") || query.contains("Bestseller")) {
                    books = applyVarietyFilter(books)
                }

                // FALLBACK: Si tras limpiar todo quedan muy pocos, rellenamos
                if (books.size < 3) {
                    var fallbackBooks = response.body()?.items?.map { it.toDomain() } ?: emptyList()
                    fallbackBooks = fallbackBooks.filter { it.imageUrl.isNotEmpty() && it.authors.isNotEmpty() && !it.isMature }

                    if (filterModern) {
                        fallbackBooks = fallbackBooks.filter { book ->
                            val year = Regex("\\d{4}").find(book.lanzamiento)?.value?.toIntOrNull() ?: 0
                            year >= currentYear - 10
                        }
                    }
                    books = fallbackBooks.sortedByDescending { it.ratingsCount }
                }

                Result.success(books.take(limit))
            } else {
                Result.failure(Exception("Google API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 🔥 NUEVA FUNCIÓN: Lee los libros guardados en Firebase y los convierte a objetos Book
    private suspend fun fetchFromFirebase(query: String): List<Book> {
        return try {
            val snapshot = db.collection("books").get().await()
            val allBooks = snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                val title = doc.getString("title") ?: ""
                val subtitle = doc.getString("subtitle") ?: ""
                val authors = doc.get("authors") as? List<String> ?: emptyList()
                val description = doc.getString("description") ?: ""
                val imageUrl = doc.getString("imageUrl") ?: ""
                val lanzamiento = doc.getString("lanzamiento") ?: ""
                val averageRating = doc.getDouble("averageRating") ?: 0.0
                val ratingsCount = doc.getLong("ratingsCount")?.toInt() ?: 0
                val pageCount = doc.getLong("pageCount")?.toInt() ?: 0
                val isMature = doc.getBoolean("isMature") ?: false
                val categories = doc.get("categories") as? List<String> ?: emptyList()

                val seriesName = doc.getString("seriesName") ?: ""
                val seriesIndex = doc.getLong("seriesIndex")?.toInt() ?: 0

                // No mostramos libros +18 aunque se hayan guardado en Firebase
                if (isMature) return@mapNotNull null

                Book(id, title, subtitle, authors, description, imageUrl, lanzamiento, averageRating, ratingsCount, pageCount, isMature, categories, seriesName, seriesIndex)
            }

            // Limpiamos la búsqueda (quitamos el "subject:" si lo tiene) para comparar textos
            val cleanQuery = query.replace("subject:", "").replace("Bestseller", "").trim().lowercase()

            // Si no hay filtro, devolvemos los más famosos
            if (cleanQuery.isEmpty()) return allBooks.sortedByDescending { it.ratingsCount }

            // Filtramos en la app: buscamos si la categoría, el título o la descripción coinciden
            allBooks.filter { book ->
                book.categories.any { it.lowercase().contains(cleanQuery) } ||
                        book.title.lowercase().contains(cleanQuery) ||
                        book.description.lowercase().contains(cleanQuery)
            }.sortedByDescending { it.ratingsCount } // Los ordenamos para que los mejores salgan primero

        } catch (e: Exception) {
            emptyList() // Si Firebase falla o está vacío, devolvemos lista vacía y entra el Plan B de Google
        }
    }

    // --- EL CEREBRO DEL FILTRO DE VARIEDAD (Google Books) ---
    private fun applyVarietyFilter(books: List<Book>): List<Book> {
        val filteredList = mutableListOf<Book>()
        val authorCounts = mutableMapOf<String, Int>()

        for (book in books) {
            val author = book.authors.firstOrNull() ?: "Unknown"

            val cleanTitle = book.title.lowercase().replace(Regex("[^a-z0-9áéíóúñ ]"), "")
            val words = cleanTitle.split(" ").filter { it.length > 2 }
            val prefix = words.take(2).joinToString(" ")

            val authorCount = authorCounts.getOrDefault(author, 0)

            val hasSameSaga = filteredList.any { existingBook ->
                val existingClean = existingBook.title.lowercase().replace(Regex("[^a-z0-9áéíóúñ ]"), "")
                val existingWords = existingClean.split(" ").filter { it.length > 2 }
                val existingPrefix = existingWords.take(2).joinToString(" ")

                prefix.isNotEmpty() && prefix == existingPrefix
            }

            if (authorCount < 2 && !hasSameSaga) {
                filteredList.add(book)
                authorCounts[author] = authorCount + 1
            }
        }

        if (filteredList.size < 4 && books.size >= 4) {
            return books.distinctBy { it.id }
        }

        return filteredList
    }

    suspend fun searchHybrid(query: String): Result<List<Book>> = coroutineScope {
        try {

            // 1️⃣ BUSCAR PRIMERO EN FIREBASE
            val localBooks = fetchFromFirebase(query)

            if (localBooks.size >= 8) {
                return@coroutineScope Result.success(localBooks.take(20))
            }

            val lang = Locale.getDefault().language

            // 2️⃣ BUSCAR EN GOOGLE Y OPENLIBRARY EN PARALELO
            val googleJob = async {
                apiService.searchBooksGoogle(
                    query = query,
                    apiKey = API_KEY,
                    startIndex = 0,
                    maxResults = 30,
                    orderBy = "relevance",
                    lang = lang,
                    printType = "books"
                )
            }

            val olJob = async {
                val olLang = if (lang == "es") "spa" else "eng"
                apiService.searchBooksOpenLibrary("$query language:$olLang", limit = 20)
            }

            val googleResp = googleJob.await()
            val olResp = olJob.await()

            val googleBooks = googleResp.body()?.items?.map { it.toDomain() } ?: emptyList()
            val openLibraryBooks = olResp.body()?.docs?.map { it.toDomain() } ?: emptyList()

            val sortedGoogle = googleBooks.sortedByDescending { it.ratingsCount }

            val combined = (localBooks + sortedGoogle + openLibraryBooks)
                .filter {
                    it.imageUrl.isNotEmpty() &&
                            it.authors.isNotEmpty() &&
                            !it.isMature
                }
                .distinctBy { it.title.lowercase().trim() }


            Result.success(combined.take(30))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBookDetail(id: String): Result<Book> {
        return try {
            // 🔥 FASE 1: Buscar detalles en Firebase primero
            val snapshot = db.collection("books").document(id).get().await()
            if (snapshot.exists()) {
                val title = snapshot.getString("title") ?: ""
                val subtitle = snapshot.getString("subtitle") ?: ""
                val authors = snapshot.get("authors") as? List<String> ?: emptyList()
                val description = snapshot.getString("description") ?: ""
                val imageUrl = snapshot.getString("imageUrl") ?: ""
                val lanzamiento = snapshot.getString("lanzamiento") ?: ""
                val averageRating = snapshot.getDouble("averageRating") ?: 0.0
                val ratingsCount = snapshot.getLong("ratingsCount")?.toInt() ?: 0
                val pageCount = snapshot.getLong("pageCount")?.toInt() ?: 0
                val isMature = snapshot.getBoolean("isMature") ?: false
                val categories = snapshot.get("categories") as? List<String> ?: emptyList()
                val seriesName = snapshot.getString("seriesName") ?: ""
                val seriesIndex = snapshot.getLong("seriesIndex")?.toInt() ?: 0

                val book = Book(id, title, subtitle, authors, description, imageUrl, lanzamiento, averageRating, ratingsCount, pageCount, isMature, categories, seriesName, seriesIndex)
                return Result.success(book)
            }

            // Si no está en Firebase, lo pedimos a OpenLibrary o Google
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

    fun saveBookToFirebase(book: Book) {
        // Creamos un mapa con los datos exactos que nuestra app necesita leer luego
        val bookData = hashMapOf(
            "id" to book.id,
            "title" to book.title,
            "subtitle" to book.subtitle,
            "authors" to book.authors,
            "description" to book.description,
            "imageUrl" to book.imageUrl,
            "lanzamiento" to book.lanzamiento,
            "averageRating" to book.averageRating,
            "ratingsCount" to book.ratingsCount,
            "pageCount" to book.pageCount,
            "isMature" to book.isMature,
            "categories" to book.categories,
            "seriesName" to book.seriesName,
            "seriesIndex" to book.seriesIndex
        )

        // Usamos SetOptions.merge() para que, si el libro ya existe porque otro
        // usuario lo guardó antes, no se borren datos extra que pudiera tener,
        // sino que solo se actualice.
        db.collection("books").document(book.id)
            .set(bookData, SetOptions.merge())
            .addOnSuccessListener {
                // Libro guardado en la comunidad con éxito
            }
            .addOnFailureListener {
                // Error silencioso, no pasa nada si falla una vez
            }
    }

    suspend fun getBooksByGenres(genres: List<String>): List<Book> = coroutineScope {

        val jobs = genres.take(3).map { genre ->
            async {
                getBooks("subject:$genre", filterModern = true, limit = 10)
                    .getOrNull() ?: emptyList()
            }
        }

        jobs.awaitAll().flatten()
    }

    suspend fun ensureBookExists(book: Book) {

        try {

            val db = FirebaseFirestore.getInstance()

            val doc = db.collection("books")
                .document(book.id)
                .get()
                .await()

            if (!doc.exists()) {

                val categories = normalizeCategories(book.categories)

                val bookMap = mapOf(
                    "id" to book.id,
                    "title" to book.title,
                    "subtitle" to book.subtitle,
                    "authors" to book.authors,
                    "description" to book.description,
                    "categories" to categories,
                    "imageUrl" to book.imageUrl,
                    "publishedDate" to book.lanzamiento,
                    "averageRating" to book.averageRating,
                    "ratingsCount" to book.ratingsCount,
                    "pageCount" to book.pageCount,
                    "isMature" to book.isMature,
                    "seriesName" to book.seriesName,
                    "seriesIndex" to book.seriesIndex
                )

                db.collection("books")
                    .document(book.id)
                    .set(bookMap)
                    .await()

                Log.d("BooksRepository", "Libro guardado con categorias: $categories")
            }

        } catch (e: Exception) {
            Log.e("BooksRepository", "Error guardando libro: ${e.message}")
        }
    }

    suspend fun getPopularBooks(limit: Int = 10): List<Book> {

        val db = FirebaseFirestore.getInstance()

        val snapshot = db.collection("books")
            .orderBy("ratingsCount", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()

        return snapshot.documents.mapNotNull {
            it.toObject(Book::class.java)
        }
    }

    private fun normalizeCategories(rawCategories: List<String>?): List<String> {

        if (rawCategories.isNullOrEmpty()) {
            return listOf("general")
        }

        val normalized = rawCategories
            .flatMap { it.split("/", ",", ";") }
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()

        return if (normalized.isEmpty()) listOf("general") else normalized
    }
}