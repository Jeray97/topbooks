package com.example.topbooks.data.repository

import com.example.topbooks.data.model.Book
import com.example.topbooks.ui.profile.BookmarkUI
import com.example.topbooks.ui.profile.SimpleBook
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

interface ProgressRepository {
    suspend fun saveReview(book: Book, rating: Int, text: String): Result<Boolean>
    suspend fun saveComment(book: Book, text: String, chapter: String): Result<Boolean>
    suspend fun toggleFavorite(book: Book, isSaving: Boolean): Result<Boolean>
    suspend fun saveBookmark(book: Book, quote: String, chapter: String, page: String, isPublic: Boolean): Result<Boolean>
    suspend fun markAsRead(book: Book): Result<Boolean>

    suspend fun getReadBooks(userId: String): Result<List<SimpleBook>>
    suspend fun getBookmarks(userId: String): Result<List<BookmarkUI>>

    suspend fun deleteDocument(collection: String, documentId: String): Result<Boolean>
    suspend fun deleteUserSubdocument(collection: String, documentId: String): Result<Boolean>
    suspend fun updateUserSubdocument(collection: String, documentId: String, data: Map<String, Any>): Result<Boolean>
}

class ProgressRepositoryImpl : ProgressRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val myUid get() = auth.currentUser?.uid

    override suspend fun saveReview(book: Book, rating: Int, text: String): Result<Boolean> {
        val uid = myUid ?: return Result.failure(Exception("No auth"))
        return try {
            ensureBookInGlobal(book)
            val ref = db.collection("reviews").document()
            ref.set(hashMapOf("id" to ref.id, "bookId" to book.id, "userId" to uid, "rating" to rating, "text" to text, "createAt" to com.google.firebase.Timestamp.now())).await()
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun saveComment(book: Book, text: String, chapter: String): Result<Boolean> {
        val uid = myUid ?: return Result.failure(Exception("No auth"))
        return try {
            ensureBookInGlobal(book)
            val ref = db.collection("comments").document()
            ref.set(hashMapOf("commentId" to ref.id, "bookId" to book.id, "userId" to uid, "text" to text, "chapter" to chapter, "createAt" to com.google.firebase.Timestamp.now(), "replies" to emptyList<Any>())).await()
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

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

    override suspend fun saveBookmark(book: Book, quote: String, chapter: String, page: String, isPublic: Boolean): Result<Boolean> {
        val uid = myUid ?: return Result.failure(Exception("No auth"))
        return try {
            ensureBookInGlobal(book)
            val ref = db.collection("users").document(uid).collection("bookmarks").document(book.id)
            ref.set(hashMapOf("bookId" to book.id, "quote" to quote, "chapter" to chapter, "page" to page, "isPublic" to isPublic)).await()
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun markAsRead(book: Book): Result<Boolean> {
        val uid = myUid ?: return Result.failure(Exception("No auth"))
        return try {
            ensureBookInGlobal(book)
            val ref = db.collection("users").document(uid).collection("read_books").document(book.id)
            ref.set(hashMapOf("bookId" to book.id, "title" to book.title, "imageUrl" to book.imageUrl, "addedAt" to System.currentTimeMillis())).await()
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun getReadBooks(userId: String): Result<List<SimpleBook>> {
        return try {
            val snap = db.collection("users").document(userId).collection("read_books").get().await()
            val books = snap.documents.mapNotNull { SimpleBook(id = it.id, title = it.getString("title") ?: "Libro", imageUrl = it.getString("imageUrl") ?: "") }
            Result.success(books)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun getBookmarks(userId: String): Result<List<BookmarkUI>> {
        return try {
            val snap = db.collection("users").document(userId).collection("bookmarks").get().await()
            val marks = snap.documents.mapNotNull {
                BookmarkUI(id = it.id, bookId = it.getString("bookId") ?: "", quote = it.getString("quote") ?: "", chapter = it.getString("chapter") ?: "", page = it.getString("page") ?: "", isPublic = it.getBoolean("isPublic") ?: true)
            }
            Result.success(marks)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun deleteDocument(collection: String, documentId: String): Result<Boolean> {
        return try {
            db.collection(collection).document(documentId).delete().await()
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun deleteUserSubdocument(collection: String, documentId: String): Result<Boolean> {
        val uid = myUid ?: return Result.failure(Exception("No auth"))
        return try {
            db.collection("users").document(uid).collection(collection).document(documentId).delete().await()
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    private suspend fun ensureBookInGlobal(book: Book) {
        val ref = db.collection("books").document(book.id)
        if (!ref.get().await().exists()) {
            ref.set(hashMapOf("title" to book.title, "thumbnail" to book.imageUrl, "createdAt" to com.google.firebase.Timestamp.now()), SetOptions.merge()).await()
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