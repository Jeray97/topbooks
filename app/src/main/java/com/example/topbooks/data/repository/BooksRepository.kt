package com.example.topbooks.data.repository

import android.content.Context
import android.util.Log
import com.example.topbooks.BuildConfig
import com.example.topbooks.data.local.AppDatabase
import com.example.topbooks.data.local.BookDao
import com.example.topbooks.data.local.NetworkMonitor
import com.example.topbooks.data.local.toDomain
import com.example.topbooks.data.local.toEntity
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.network.RetrofitClient
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Locale
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Repositorio central encargado de la gestión, búsqueda y filtrado de Libros.
 * * Implementa una arquitectura híbrida: prioriza los datos guardados por la comunidad
 * en Firebase Firestore y utiliza Google Books y Open Library como respaldo (Fallback).
 * * Modo offline: Cache local con Room para acceso sin conexión.
 */
class BooksRepository(context: Context? = null) {

    private val apiService = RetrofitClient.instance
    private val API_KEY = BuildConfig.API_KEY
    private val db = FirebaseFirestore.getInstance()

    private val bookDao: BookDao? = context?.let { AppDatabase.getInstance(it).bookDao() }
    private val networkMonitor: NetworkMonitor? = context?.let { NetworkMonitor(it) }

    companion object {
        var lastScannedBook: Book? = null
        private const val CACHE_VALIDITY_MS = 24 * 60 * 60 * 1000L // 24 horas
    }

    /**
     * Obtiene una lista de libros basada en una consulta (query).
     * * Fase 1: Busca en la base de datos de Firebase. Si la comunidad ya ha guardado suficientes libros, los devuelve.
     * * Fase 2: Si no hay suficientes, hace una petición a Google Books aplicando filtros de calidad, actualidad y variedad.
     */
    suspend fun getBooks(
        query: String,
        orderBy: String = "relevance",
        filterModern: Boolean = false,
        page: Int = 1,
        limit: Int = 20
    ): Result<List<Book>> {
        return try {
            val isOnline = networkMonitor?.isCurrentlyOnline() ?: true

            val cachedBooks = bookDao?.searchBooks(query, limit) ?: emptyList()
            if (!isOnline && cachedBooks.isNotEmpty()) {
                return Result.success(cachedBooks.map { it.toDomain() })
            }

            val currentYear = Calendar.getInstance().get(Calendar.YEAR)

            // ====================================================================
            // FASE 1: BUSCAMOS EN NUESTRA PROPIA BASE DE DATOS (FIREBASE)
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
                bookDao?.insertBooks(localBooks.map { it.toEntity() })
                return Result.success(localBooks.take(limit))
            }


            // ====================================================================
            // FASE 2: PLAN DE EMERGENCIA (GOOGLE BOOKS)
            // Si la comunidad aún no ha guardado suficientes libros de esto, vamos a Google
            // ====================================================================
            val langCode = Locale.getDefault().language
            val startIndex = (page - 1) * limit
            var apiQuery = query

            if (filterModern) {
                apiQuery = "$query $currentYear OR ${currentYear - 1} OR ${currentYear - 2}"
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

                // ORDENAMOS POR FAMA (Cantidad de reseñas)
                books = books.sortedByDescending { it.ratingsCount }

                // FILTRO ANTI-SAGAS REPETIDAS (Variedad)
                if (filterModern || query.contains("subject:") || query.contains("Bestseller")) {
                    books = applyVarietyFilter(books)
                }

                // FALLBACK: Si tras limpiar to-do quedan muy pocos, rellenamos con criterios más suaves
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

                bookDao?.insertBooks(books.take(limit).map { it.toEntity() })
                Result.success(books.take(limit))
            } else {
                Result.failure(Exception("Google API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Busca y filtra libros guardados localmente en la colección de Firestore.
     */
    private suspend fun fetchFromFirebase(query: String): List<Book> {
        return try {
            val snapshot = db.collection("books").get().await()
            val allBooks = snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                val title = doc.getString("title") ?: ""
                val subtitle = doc.getString("subtitle") ?: ""
                val authors = (doc.get("authors") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

                // SANITIZACIÓN: Limpiamos por si se guardó sucio
                val rawDescription = doc.getString("description") ?: ""
                val description = com.example.topbooks.utils.HtmlCleaner.clean(rawDescription)

                val imageUrl = doc.getString("imageUrl") ?: ""
                val lanzamiento = doc.getString("lanzamiento") ?: ""
                val averageRating = doc.getDouble("averageRating") ?: 0.0
                val ratingsCount = doc.getLong("ratingsCount")?.toInt() ?: 0
                val pageCount = doc.getLong("pageCount")?.toInt() ?: 0
                val isMature = doc.getBoolean("isMature") ?: false
                val categories = (doc.get("categories") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

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

    /**
     * El "Cerebro" del Filtro de Variedad.
     */
    private fun applyVarietyFilter(books: List<Book>): List<Book> {
        val filteredList = mutableListOf<Book>()
        val authorCounts = mutableMapOf<String, Int>()

        for (book in books) {
            val author = book.authors.firstOrNull() ?: "Unknown"

            // Limpiamos el título para extraer un "prefijo" base (útil para detectar sagas ocultas)
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

            // Máximo 2 libros por autor y evitamos repetir la misma saga
            if (authorCount < 2 && !hasSameSaga) {
                filteredList.add(book)
                authorCounts[author] = authorCount + 1
            }
        }

        // Si hemos sido demasiado estrictos y nos quedamos sin resultados, devolvemos lo original sin repetidos exactos
        if (filteredList.size < 4 && books.size >= 4) {
            return books.distinctBy { it.id }
        }

        return filteredList
    }

    /**
     * Búsqueda híbrida y paralela.
     */
    suspend fun searchHybrid(query: String, maxResults: Int = 20): Result<List<Book>> = coroutineScope {
        try {

            // 1. BUSCAR PRIMERO EN FIREBASE
            val localBooks = fetchFromFirebase(query)

            if (localBooks.size >= 8) {
                return@coroutineScope Result.success(localBooks.take(maxResults))
            }

            val lang = Locale.getDefault().language

            // 2. BUSCAR EN GOOGLE Y OPENLIBRARY EN PARALELO (CON ESCUDOS)
            val googleJob = async {
                try {
                    apiService.searchBooksGoogle(
                        query = query,
                        apiKey = API_KEY,
                        startIndex = 0,
                        maxResults = 30,
                        orderBy = "relevance",
                        printType = "books"
                    )
                } catch (e: Exception) {
                    null // Si falla internet o Google, devolvemos null en vez de romper la app
                }
            }

            val olJob = async {
                try {
                    apiService.searchBooksOpenLibrary(query, limit = 20)
                } catch (e: Exception) {
                    null // Si Open Library da Timeout, devolvemos null y la app sigue viva
                }
            }

            val googleResp = googleJob.await()
            val olResp = olJob.await()

            val googleBooks = googleResp?.body()?.items?.map { it.toDomain() } ?: emptyList()
            val openLibraryBooks = olResp?.body()?.docs?.map { it.toDomain() } ?: emptyList()

            val sortedGoogle = googleBooks.sortedByDescending { it.ratingsCount }

            // Combinar, limpiar (+18 y sin imagen) y quitar duplicados exactos
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

    /**
     * Obtiene el detalle de un libro en concreto.
     * Si el ID es de Open Library y no tiene descripción, intenta buscar en Google por título.
     */
    suspend fun getBookDetail(id: String): Result<Book> {
        return try {
            val cachedBook = bookDao?.getBookById(id)
            val isOnline = networkMonitor?.isCurrentlyOnline() ?: true

            if (cachedBook != null && (!isOnline || System.currentTimeMillis() - cachedBook.cachedAt < CACHE_VALIDITY_MS)) {
                return Result.success(cachedBook.toDomain())
            }

            if (!isOnline && cachedBook != null) {
                return Result.success(cachedBook.toDomain())
            }

            // 1. Firebase (Prioridad 1)
            val snapshot = db.collection("books").document(id).get().await()
            if (snapshot.exists()) {
                val rawDescription = snapshot.getString("description") ?: ""

                // SANITIZACIÓN: Limpiamos la descripción por si se guardó con HTML en el pasado
                val cleanDescription = com.example.topbooks.utils.HtmlCleaner.clean(rawDescription)

                val isDescriptionValid = cleanDescription.isNotBlank() &&
                        cleanDescription != "Toca para ver detalles..." &&
                        cleanDescription != "Sin descripción."

                val book = Book(
                    id = id,
                    title = snapshot.getString("title") ?: "",
                    subtitle = snapshot.getString("subtitle") ?: "",
                    authors = (snapshot.get("authors") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    description = cleanDescription,
                    imageUrl = snapshot.getString("imageUrl") ?: "",
                    lanzamiento = snapshot.getString("lanzamiento") ?: "",
                    averageRating = snapshot.getDouble("averageRating") ?: 0.0,
                    ratingsCount = snapshot.getLong("ratingsCount")?.toInt() ?: 0,
                    pageCount = snapshot.getLong("pageCount")?.toInt() ?: 0,
                    isMature = snapshot.getBoolean("isMature") ?: false,
                    categories = (snapshot.get("categories") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    seriesName = snapshot.getString("seriesName") ?: "",
                    seriesIndex = snapshot.getLong("seriesIndex")?.toInt() ?: 0
                )
                if (isDescriptionValid) {
                    bookDao?.insertBook(book.toEntity())
                    return Result.success(book)
                }
            }

            // 2. Fetch de API (Google u OpenLibrary)
            var finalBook: Book? = null

            if (id.startsWith("OL")) {
                val response = apiService.getWorkDetailOpenLibrary(id)
                if (response.isSuccessful) {
                    val work = response.body()
                    val descriptionText = when (val desc = work?.description) {
                        is String -> desc
                        is Map<*, *> -> desc["value"] as? String ?: ""
                        else -> ""
                    }

                    // SANITIZACIÓN: Limpiamos el texto que viene de Open Library
                    val cleanDesc = com.example.topbooks.utils.HtmlCleaner.clean(descriptionText)
                    val cover = work?.covers?.firstOrNull()?.let { "https://covers.openlibrary.org/b/id/$it-L.jpg" } ?: ""

                    finalBook = Book(id = id, title = work?.title ?: "Sin título", authors = emptyList(), description = cleanDesc, imageUrl = cover)

                    // --- EL PUENTE (BRIDGE): Si OL no tiene descripción, saltamos a Google por título ---
                    if (cleanDesc.isBlank() || cleanDesc == "Sin descripción.") {
                        val googleFallback = apiService.searchBooksGoogle(
                            query = "intitle:${finalBook.title}",
                            apiKey = API_KEY,
                            startIndex = 0,
                            maxResults = 1,
                            orderBy = "relevance"
                        )
                        if (googleFallback.isSuccessful) {
                            val googleBook = googleFallback.body()?.items?.firstOrNull()?.toDomain()
                            if (googleBook != null) {
                                finalBook = finalBook.copy(
                                    description = googleBook.description, // El toDomain de GoogleBooksResponse ya lo limpia
                                    authors = if (finalBook.authors.isEmpty()) googleBook.authors else finalBook.authors,
                                    imageUrl = if (finalBook.imageUrl.isEmpty()) googleBook.imageUrl else finalBook.imageUrl
                                )
                            }
                        }
                    }
                }
            } else {
                val response = apiService.getBookDetailGoogle(id, API_KEY)
                if (response.isSuccessful) finalBook = response.body()?.toDomain()
            }

            if (finalBook != null) {
                bookDao?.insertBook(finalBook.toEntity())
                Result.success(finalBook)
            }
            else Result.failure(Exception("Libro no encontrado"))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Guarda o actualiza un libro en Firebase para que la comunidad pueda acceder a él sin consumir cuota de API. */
    fun saveBookToFirebase(book: Book) {
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

    /** Comprueba si el libro existe en Firestore; si no, lo guarda basándose en el modelo de dominio. */
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

        return snapshot.documents.mapNotNull { doc ->
            try {
                // SANITIZACIÓN: Limpiamos por si se guardó sucio
                val rawDescription = doc.getString("description") ?: ""
                val description = com.example.topbooks.utils.HtmlCleaner.clean(rawDescription)

                Book(
                    id = doc.getString("id") ?: doc.id,
                    title = doc.getString("title") ?: "",
                    subtitle = doc.getString("subtitle") ?: "",
                    authors = (doc.get("authors") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    description = description,
                    imageUrl = doc.getString("imageUrl") ?: "",
                    lanzamiento = doc.getString("publishedDate") ?: doc.getString("lanzamiento")
                    ?: "",
                    averageRating = doc.getDouble("averageRating") ?: 0.0,
                    ratingsCount = doc.getLong("ratingsCount")?.toInt() ?: 0,
                    pageCount = doc.getLong("pageCount")?.toInt() ?: 0,
                    isMature = doc.getBoolean("isMature") ?: false,
                    categories = (doc.get("categories") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    seriesName = doc.getString("seriesName") ?: "",
                    seriesIndex = doc.getLong("seriesIndex")?.toInt() ?: 0
                )
            } catch (e: Exception) {
                null
            }
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

        return normalized.ifEmpty { listOf("general") }
    }

    suspend fun testSagasRawJson(testQuery: String = "Harry Potter") {
        withContext(Dispatchers.IO) {
            try {
                // 1. Probamos Google Books
                val googleUrl = "https://www.googleapis.com/books/v1/volumes?q=${testQuery.replace(" ", "+")}&key=$API_KEY"
                val googleJson = URL(googleUrl).readText()

                Log.d("API_TEST_GOOGLE", "=== GOOGLE BOOKS RAW ===")
                Log.d("API_TEST_GOOGLE", googleJson.take(3000))

                // 2. Probamos Open Library
                val openLibUrl = "https://openlibrary.org/search.json?q=${testQuery.replace(" ", "+")}&limit=2"
                val openLibJson = URL(openLibUrl).readText()

                Log.d("API_TEST_OPENLIB", "=== OPEN LIBRARY RAW ===")
                Log.d("API_TEST_OPENLIB", openLibJson.take(3000))

            } catch (e: Exception) {
                Log.e("API_TEST_ERROR", "Error espiando APIs: ${e.message}")
            }
        }
    }

    // --- SISTEMA SOCIAL DE SAGAS ---

    suspend fun updateBookSeries(book: Book, newName: String, newIndex: Int, editorUid: String, editorName: String, editorAvatar: String): Result<Boolean> {
        return try {
            ensureBookExists(book)

            val updates = mapOf(
                "seriesName" to newName.trim(),
                "seriesIndex" to newIndex,
                "seriesEditorUid" to editorUid,
                "seriesEditorName" to editorName,
                "seriesEditorAvatar" to editorAvatar,
                "seriesEditDate" to System.currentTimeMillis(),
                "seriesUpvotes" to 0,
                "seriesDownvotes" to 0,
                "seriesVoters" to emptyList<String>()
            )

            db.collection("books").document(book.id).update(updates).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun voteSeriesEdit(bookId: String, uid: String, isUpvote: Boolean): Result<Boolean> {
        return try {
            val bookRef = db.collection("books").document(bookId)

            val voteField = if (isUpvote) "seriesUpvotes" else "seriesDownvotes"

            val updates = mapOf(
                voteField to FieldValue.increment(1),
                "seriesVoters" to FieldValue.arrayUnion(uid)
            )

            bookRef.update(updates).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Búsqueda estricta y directa por ISBN con patrón "Best-Effort" (Mejor Esfuerzo).
     */
    suspend fun getBookByIsbn(isbn: String): Result<Book?> = coroutineScope {
        try {
            var bookWithoutCover: Book? = null

            // 1. Búsqueda estricta en Google Books
            val googleResponse = apiService.searchBooksGoogle(
                query = "isbn:$isbn",
                apiKey = API_KEY,
                startIndex = 0,
                maxResults = 2,
                orderBy = "relevance"
            )

            if (googleResponse.isSuccessful) {
                val items = googleResponse.body()?.items?.map { it.toDomain() }
                val validBook = items?.find { it.title.isNotEmpty() && it.title != "Sin título" }

                if (validBook != null) {
                    if (validBook.imageUrl.isNotEmpty()) {
                        return@coroutineScope Result.success(validBook)
                    } else {
                        bookWithoutCover = validBook
                    }
                }
            }

            // 2. Búsqueda flexible en Google Books
            val fallbackResponse = apiService.searchBooksGoogle(
                query = isbn,
                apiKey = API_KEY,
                startIndex = 0,
                maxResults = 2,
                orderBy = "relevance"
            )

            if (fallbackResponse.isSuccessful) {
                val items = fallbackResponse.body()?.items?.map { it.toDomain() }
                val bestBook = items?.filter { it.title.isNotEmpty() && it.title != "Sin título" }?.maxByOrNull { it.imageUrl.length }

                if (bestBook != null) {
                    if (bestBook.imageUrl.isNotEmpty()) {
                        return@coroutineScope Result.success(bestBook)
                    } else if (bookWithoutCover == null) {
                        bookWithoutCover = bestBook
                    }
                }
            }

            // 3. Open Library
            val olResponse = apiService.searchBooksOpenLibrary(query = "isbn:$isbn", limit = 2)

            if (olResponse.isSuccessful) {
                val docs = olResponse.body()?.docs?.map { it.toDomain() }
                val olBook = docs?.firstOrNull { it.title.isNotEmpty() }

                if (olBook != null) {
                    if (olBook.imageUrl.isNotEmpty()) {
                        return@coroutineScope Result.success(olBook)
                    } else if (bookWithoutCover == null) {
                        bookWithoutCover = olBook
                    }
                }
            }

            // 4. PLAN B
            if (bookWithoutCover != null) {
                return@coroutineScope Result.success(bookWithoutCover)
            }

            Result.success(null)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}