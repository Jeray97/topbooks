package com.example.topbooks.data.repository

import com.example.topbooks.data.model.Book
import com.example.topbooks.data.model.Journal
import com.example.topbooks.ui.profile.BookmarkUI
import com.example.topbooks.ui.profile.SimpleBook
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * 1. DEFINICIÓN DE LA INTERFAZ
 * Contrato que define todas las acciones de progreso e interacción de un usuario con los libros.
 * * Incluye reseñas, comentarios, marcadores, favoritos y estados de lectura.
 */
interface ProgressRepository {
    // --- ACCIONES DE ESCRITURA ---
    suspend fun saveReview(book: Book, rating: Int, text: String): Result<Boolean>
    suspend fun saveComment(book: Book, text: String, chapter: String): Result<Boolean>
    suspend fun toggleFavorite(book: Book, isSaving: Boolean): Result<Boolean>
    suspend fun saveBookmark(book: Book, quote: String, chapter: String, page: String, isPublic: Boolean): Result<Boolean>
    suspend fun markAsRead(book: Book): Result<Boolean>

    // --- ACCIONES DE LECTURA ---
    suspend fun getReadBooks(userId: String): Result<List<SimpleBook>>
    suspend fun getBookmarks(userId: String): Result<List<BookmarkUI>>
    suspend fun getUserJournals(userId: String): Result<List<Journal>>

    // --- ACCIONES GENÉRICAS DE GESTIÓN DE DATOS ---
    suspend fun deleteDocument(collection: String, documentId: String): Result<Boolean>
    suspend fun deleteUserSubdocument(collection: String, documentId: String): Result<Boolean>
    suspend fun updateUserSubdocument(collection: String, documentId: String, data: Map<String, Any>): Result<Boolean>
}

/**
 * 2. IMPLEMENTACIÓN DE LA INTERFAZ
 * * Conecta con Firebase Firestore y gestiona tanto colecciones globales ("reviews", "comments")
 * como subcolecciones privadas del usuario ("favorites", "bookmarks", "read_books").
 */
class ProgressRepositoryImpl : ProgressRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    // Helper property para obtener el UID del usuario de forma rápida y segura
    private val myUid get() = auth.currentUser?.uid

    /**
     * Guarda una reseña global del libro en la colección "reviews".
     */
    override suspend fun saveReview(book: Book, rating: Int, text: String): Result<Boolean> {
        val uid = myUid ?: return Result.failure(Exception("No auth"))
        return try {
            ensureBookInGlobal(book)

            // Obtenemos el perfil del usuario para guardar su nombre
            val userDoc = db.collection("users").document(uid).get().await()
            val userName = userDoc.getString("displayName") ?: "Usuario"
            val userPhoto = userDoc.getString("photoURL") ?: ""

            val ref = db.collection("reviews").document()
            ref.set(hashMapOf(
                "id"           to ref.id,
                "bookId"       to book.id,
                "userId"       to uid,
                "userName"     to userName,
                "userPhotoUrl" to userPhoto,
                "rating"       to rating,
                "text"         to text,
                "createAt"     to com.google.firebase.Timestamp.now()
            )).await()
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    /**
     * Guarda un comentario sobre un capítulo en la colección "comments".
     * Inicializa el hilo de respuestas (replies) y los participantes.
     */
    override suspend fun saveComment(book: Book, text: String, chapter: String): Result<Boolean> {
        val uid = myUid ?: return Result.failure(Exception("No auth"))
        return try {
            ensureBookInGlobal(book)
            val ref = db.collection("comments").document()
            ref.set(hashMapOf(
                "commentId" to ref.id,
                "bookId" to book.id,
                "userId" to uid,
                "text" to text,
                "chapter" to chapter,
                "createAt" to com.google.firebase.Timestamp.now(),
                "replies" to emptyList<Any>(),
                "participantIds" to listOf(uid) // El creador es el primer participante
            )).await()
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    /** Añade o quita un libro de la subcolección privada "favorites" del usuario. */
    override suspend fun toggleFavorite(book: Book, isSaving: Boolean): Result<Boolean> {
        val uid = myUid ?: return Result.failure(Exception("No auth"))
        return try {
            ensureBookInGlobal(book)
            val ref = db.collection("users").document(uid).collection("favorites").document(book.id)
            if (isSaving) ref.set(hashMapOf("bookId" to book.id, "bookImageUrl" to book.imageUrl, "addedAt" to System.currentTimeMillis())).await()
            else ref.delete().await()
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    /** Guarda un marcador (cita, página, capítulo) en la subcolección "bookmarks". */
    override suspend fun saveBookmark(book: Book, quote: String, chapter: String, page: String, isPublic: Boolean): Result<Boolean> {
        val uid = myUid ?: return Result.failure(Exception("No auth"))
        return try {
            ensureBookInGlobal(book)
            val ref = db.collection("users").document(uid).collection("bookmarks").document(book.id)
            ref.set(hashMapOf("bookId" to book.id, "quote" to quote, "chapter" to chapter, "page" to page, "isPublic" to isPublic)).await()
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    /** Marca un libro como leído en la subcolección "read_books". */
    override suspend fun markAsRead(book: Book): Result<Boolean> {
        val uid = myUid ?: return Result.failure(Exception("No auth"))
        return try {
            ensureBookInGlobal(book)
            val ref = db.collection("users").document(uid).collection("read_books").document(book.id)
            ref.set(hashMapOf("bookId" to book.id, "title" to book.title, "imageUrl" to book.imageUrl, "addedAt" to System.currentTimeMillis())).await()
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    /** Obtiene la lista de libros leídos del usuario de forma ligera ([SimpleBook]). */
    override suspend fun getReadBooks(userId: String): Result<List<SimpleBook>> {
        return try {
            val snap = db.collection("users").document(userId).collection("read_books").get().await()
            val books = snap.documents.mapNotNull { SimpleBook(id = it.id, title = it.getString("title") ?: "Libro", imageUrl = it.getString("imageUrl") ?: "") }
            Result.success(books)
        } catch (e: Exception) { Result.failure(e) }
    }

    /** Obtiene los marcadores guardados por el usuario. */
    override suspend fun getBookmarks(userId: String): Result<List<BookmarkUI>> {
        return try {
            val snap = db.collection("users").document(userId).collection("bookmarks").get().await()
            val marks = snap.documents.mapNotNull {
                BookmarkUI(id = it.id, bookId = it.getString("bookId") ?: "", quote = it.getString("quote") ?: "", chapter = it.getString("chapter") ?: "", page = it.getString("page") ?: "", isPublic = it.getBoolean("isPublic") ?: true)
            }
            Result.success(marks)
        } catch (e: Exception) { Result.failure(e) }
    }

    /** Obtiene todos los diarios de lectura guardados en la subcolección "journals" del usuario. */
    override suspend fun getUserJournals(userId: String): Result<List<Journal>> {
        return try {
            val snap = db.collection("users").document(userId).collection("journals").get().await()
            Result.success(snap.toObjects(Journal::class.java))
        } catch (e: Exception) { Result.failure(e) }
    }

    /**
     * Función genérica para eliminar un documento de una colección principal (Ej: borrar una review).
     */
    override suspend fun deleteDocument(collection: String, documentId: String): Result<Boolean> {
        return try {
            db.collection(collection).document(documentId).delete().await()
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    /**
     * Función genérica para eliminar un documento dentro de las subcolecciones del usuario actual.
     */
    override suspend fun deleteUserSubdocument(collection: String, documentId: String): Result<Boolean> {
        val uid = myUid ?: return Result.failure(Exception("No auth"))
        return try {
            db.collection("users").document(uid).collection(collection).document(documentId).delete().await()
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    /**
     * TÉCNICA DE CACHÉ / RELACIÓN NOSQL:
     * * Antes de que un usuario interactúe con un libro (reseña, favorito, etc.),
     * nos aseguramos de que los datos básicos de ese libro (título, portada) existan en
     * la colección global "books".
     * * Esto evita cruces complejos de datos y errores al mostrar los feeds sociales.
     */
    private suspend fun ensureBookInGlobal(book: Book) {
        val ref = db.collection("books").document(book.id)
        if (!ref.get().await().exists()) {
            ref.set(hashMapOf("title" to book.title, "thumbnail" to book.imageUrl, "createdAt" to com.google.firebase.Timestamp.now()), SetOptions.merge()).await()// Usa merge por si el libro se creó una fracción de segundo antes
        }
    }


    override suspend fun updateUserSubdocument(collection: String, documentId: String, data: Map<String, Any>): Result<Boolean> {
        val uid = myUid ?: return Result.failure(Exception("No auth"))
        return try {
            db.collection("users")
                .document(uid)
                .collection(collection)
                .document(documentId)
                .update(data)
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}