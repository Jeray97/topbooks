package com.example.topbooks.data.recommendation

import android.util.Log
import com.example.topbooks.data.local.AppDatabase
import com.example.topbooks.data.local.RecommendationEntity
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.repository.BooksRepository
import com.example.topbooks.data.repository.CommunityRepository
import com.example.topbooks.data.repository.UserRepository
import com.example.topbooks.data.repository.ProgressRepository
import com.example.topbooks.data.repository.ProgressRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

/**
 * Motor de recomendaciones optimizado.
 * Proporción: 75% API/DB - 25% análisis local
 */
class RecommendationEngine(
    private val booksRepository: BooksRepository,
    private val communityRepository: CommunityRepository,
    private val userRepository: UserRepository,
    private val database: AppDatabase,
    private val progressRepository: ProgressRepository = ProgressRepositoryImpl()
) {
    private val TAG = "RecommendationEngine"

    /**
     * Genera recomendaciones personalizadas para el usuario actual.
     * Usa caché de 7 días para minimizar llamadas a API.
     */
    suspend fun getRecommendations(fallbackQuery: String): List<Book> {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return emptyList()
        
        // 1. Verificar caché
        val cached = database.recommendationDao().getValidRecommendations(uid)
        if (cached != null) {
            Log.d(TAG, "Usando recomendaciones en caché")
            return loadBooksFromCache(cached.bookIds)
        }

        Log.d(TAG, "Generando nuevas recomendaciones")
        
        // 2. Generar nuevas recomendaciones
        val recommendations = generateNewRecommendations(uid, fallbackQuery)
        
        // 3. Guardar en caché
        if (recommendations.isNotEmpty()) {
            database.recommendationDao().insertRecommendations(
                RecommendationEntity(
                    userId = uid,
                    bookIds = recommendations.map { it.id }
                )
            )
        }
        
        return recommendations
    }

    /**
     * Algoritmo principal: 75% API/DB - 25% análisis local
     */
    private suspend fun generateNewRecommendations(uid: String, fallbackQuery: String): List<Book> {
        val db = FirebaseFirestore.getInstance()
        val allBooks = mutableListOf<Book>()

        try {
            // Obtener datos del usuario
            val userDoc = db.collection("users").document(uid).get().await()
            val favoriteGenres = (userDoc.get("favoriteGenres") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val favoriteBookIds = (userDoc.get("favoriteBooks") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val readBookIds = (userDoc.get("readBooks") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

            // ═══════════════════════════════════════════════════════════
            // 25% ANÁLISIS LOCAL (sin llamadas a API)
            // ═══════════════════════════════════════════════════════════
            
            val localBooks = analyzeLocalLibrary(favoriteBookIds + readBookIds, uid)
            allBooks.addAll(localBooks)
            Log.d(TAG, "Análisis local: ${localBooks.size} libros")

            // ═══════════════════════════════════════════════════════════
            // 75% API/DB (llamadas optimizadas)
            // ═══════════════════════════════════════════════════════════

            // A) Recomendaciones sociales (Firestore - gratis)
            val socialBooks = getSocialRecommendations(uid)
            allBooks.addAll(socialBooks)
            Log.d(TAG, "Recomendaciones sociales: ${socialBooks.size} libros")

            // B) Búsqueda optimizada por géneros (API - 1 llamada por género)
            if (favoriteGenres.isNotEmpty()) {
                val genreBooks = getBooksByGenres(favoriteGenres.take(3)) // Solo top 3 géneros
                allBooks.addAll(genreBooks)
                Log.d(TAG, "Búsqueda por géneros: ${genreBooks.size} libros")
            }

            // C) Libros populares (Firestore - 1 query)
            if (allBooks.size < 15) {
                val popularBooks = getPopularBooks(10)
                allBooks.addAll(popularBooks)
                Log.d(TAG, "Libros populares: ${popularBooks.size} libros")
            }

            // D) Fallback (API - solo si es necesario)
            if (allBooks.isEmpty()) {
                val fallback = booksRepository.searchHybrid(fallbackQuery).getOrNull() ?: emptyList()
                allBooks.addAll(fallback)
                Log.d(TAG, "Fallback: ${fallback.size} libros")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error generando recomendaciones: ${e.message}")
        }

        // Limpiar duplicados y aplicar scoring
        val uniqueBooks = allBooks.distinctBy { it.id }
        val scoredBooks = applyScoring(uniqueBooks, uid)
        
        return scoredBooks.take(20)
    }

    /**
     * 25% ANÁLISIS LOCAL: Detecta patrones en la biblioteca del usuario
     */
    private suspend fun analyzeLocalLibrary(userBookIds: List<String>, uid: String): List<Book> {
        val userBooks = userBookIds.mapNotNull { bookId ->
            booksRepository.getBookDetail(bookId).getOrNull()
        }

        if (userBooks.isEmpty()) return emptyList()

        val recommendations = mutableListOf<Book>()

        // 1. Detectar autores más leídos
        val authorCounts = userBooks
            .flatMap { it.authors }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(3)

        // 2. Detectar sagas incompletas
        val sagaGroups = userBooks
            .filter { it.seriesName.isNotBlank() }
            .groupBy { it.seriesName }
            .filter { it.value.size >= 2 }

        // 3. Analizar géneros más leídos
        val genreCounts = userBooks
            .flatMap { it.categories }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(3)

        // 4. Buscar libros de autores favoritos (1 búsqueda por autor)
        authorCounts.take(2).forEach { (author, _) ->
            val authorBooks = booksRepository.searchHybrid("inauthor:$author").getOrNull() ?: emptyList()
            val newBooks = authorBooks.filter { it.id !in userBookIds }
            recommendations.addAll(newBooks.take(3))
        }

        // 5. Completar sagas (buscar libros faltantes)
        sagaGroups.entries.take(2).forEach { (sagaName, books) ->
            val sagaBooks = booksRepository.searchHybrid(sagaName).getOrNull() ?: emptyList()
            val missingBooks = sagaBooks.filter { it.seriesName == sagaName && it.id !in userBookIds }
            recommendations.addAll(missingBooks.take(2))
        }

        // 6. Buscar libros de géneros más leídos (1 búsqueda por género)
        genreCounts.take(2).forEach { (genre, _) ->
            val genreBooks = booksRepository.searchHybrid("subject:$genre").getOrNull() ?: emptyList()
            val newBooks = genreBooks.filter { it.id !in userBookIds }
            recommendations.addAll(newBooks.take(2))
        }

        return recommendations.take(8) // Limitar a 8 libros del análisis local
    }

    /**
     * 75% API/DB - A: Recomendaciones sociales (Firestore)
     */
    private suspend fun getSocialRecommendations(uid: String): List<Book> {
        return try {
            val friendIds = communityRepository.getMyFriendsIds().getOrDefault(emptySet())
            if (friendIds.isEmpty()) return emptyList()

            val friendBooks = coroutineScope {
                friendIds.take(10).map { friendId ->
                    async {
                        val favIds = userRepository.getFavoriteIds(friendId).getOrDefault(emptyList())
                        favIds.take(2)
                    }
                }
            }.awaitAll().flatten()

            val books = friendBooks.take(10).mapNotNull { bookId ->
                booksRepository.getBookDetail(bookId).getOrNull()
            }

            books
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo recomendaciones sociales: ${e.message}")
            emptyList()
        }
    }

    /**
     * 75% API/DB - B: Búsqueda optimizada por géneros (1 llamada por género)
     */
    private suspend fun getBooksByGenres(genres: List<String>): List<Book> {
        return coroutineScope {
            genres.map { genre ->
                async {
                    booksRepository.searchHybrid("subject:$genre", maxResults = 20).getOrNull() ?: emptyList()
                }
            }
        }.awaitAll().flatten().take(10)
    }

    /**
     * 75% API/DB - C: Libros populares (Firestore - 1 query)
     */
    private suspend fun getPopularBooks(limit: Int): List<Book> {
        return try {
            val db = FirebaseFirestore.getInstance()
            val snapshot = db.collection("books")
                .orderBy("popularity", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Book::class.java)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo libros populares: ${e.message}")
            emptyList()
        }
    }

    /**
     * Carga libros desde caché de IDs
     */
    private suspend fun loadBooksFromCache(bookIds: List<String>): List<Book> {
        return bookIds.mapNotNull { bookId ->
            booksRepository.getBookDetail(bookId).getOrNull()
        }
    }

    /**
     * Sistema de scoring ponderado.
     * Asigna puntuaciones a cada libro basado en múltiples señales.
     */
    private suspend fun applyScoring(books: List<Book>, uid: String): List<Book> {
        val db = FirebaseFirestore.getInstance()
        
        // Obtener datos del usuario para scoring
        val userDoc = db.collection("users").document(uid).get().await()
        val favoriteGenres = (userDoc.get("favoriteGenres") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val favoriteBookIds = (userDoc.get("favoriteBooks") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        
        // Obtener autores favoritos del usuario
        val favoriteBooks = favoriteBookIds.mapNotNull { bookId ->
            booksRepository.getBookDetail(bookId).getOrNull()
        }
        val favoriteAuthors = favoriteBooks.flatMap { it.authors }.toSet()
        
        // Obtener amigos para scoring social
        val friendIds = communityRepository.getMyFriendsIds().getOrDefault(emptySet())

        return books.map { book ->
            var score = 0.0
            
            // 1. Género favorito (peso: 3.0)
            if (book.categories.any { it in favoriteGenres }) {
                score += 3.0
            }
            
            // 2. Autor favorito (peso: 2.5)
            if (book.authors.any { it in favoriteAuthors }) {
                score += 2.5
            }
            
            // 3. Rating alto (peso: 1.5)
            if (book.averageRating >= 4.0) {
                score += 1.5
            } else if (book.averageRating >= 3.5) {
                score += 0.75
            }
            
            // 4. Popularidad (peso: 1.0)
            if (book.ratingsCount > 100) {
                score += 1.0
            } else if (book.ratingsCount > 50) {
                score += 0.5
            }
            
            // 5. Recomendación social (peso: 2.0)
            // Verificar si amigos tienen este libro en favoritos
            val friendsWithThisBook = friendIds.count { friendId ->
                val favIds = userRepository.getFavoriteIds(friendId).getOrDefault(emptyList())
                book.id in favIds
            }
            if (friendsWithThisBook > 0) {
                score += 2.0 * friendsWithThisBook.coerceAtMost(3)
            }
            
            // 6. Saga (peso: 1.5)
            if (book.seriesName.isNotBlank()) {
                score += 1.5
            }
            
            // 7. Libro reciente (peso: 0.5)
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            val bookYear = book.lanzamiento.take(4).toIntOrNull() ?: 0
            if (bookYear >= currentYear - 2) {
                score += 0.5
            }
            
            book to score
        }
        .sortedByDescending { it.second }
        .map { it.first }
    }

    /**
     * Invalida el caché de recomendaciones (llamar cuando el usuario añada/quite libros)
     */
    suspend fun invalidateCache(uid: String) {
        database.recommendationDao().deleteRecommendations(uid)
        Log.d(TAG, "Caché de recomendaciones invalidado")
    }
}
