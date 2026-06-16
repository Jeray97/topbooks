package com.example.topbooks.data.repository

import com.example.topbooks.data.model.Shelf
import com.example.topbooks.data.model.ShelfBookMeta
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface ShelfRepository {
    suspend fun getShelves(uid: String): Result<List<Shelf>>
    suspend fun getPublicShelves(uid: String): Result<List<Shelf>>
    suspend fun createShelf(name: String, color: Long, isPublic: Boolean = false): Result<Shelf>
    suspend fun updateShelf(shelf: Shelf): Result<Boolean>
    suspend fun deleteShelf(shelfId: String): Result<Boolean>
    suspend fun addBookToShelf(shelfId: String, bookId: String, meta: ShelfBookMeta): Result<Boolean>
    suspend fun removeBookFromShelf(shelfId: String, bookId: String): Result<Boolean>
    suspend fun moveBook(fromShelfId: String, toShelfId: String, bookId: String, toIndex: Int): Result<Boolean>
    suspend fun reorderBooks(shelfId: String, bookIds: List<String>): Result<Boolean>
    suspend fun toggleShelfVisibility(shelfId: String, isPublic: Boolean): Result<Boolean>
}

class ShelfRepositoryImpl : ShelfRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val myUid get() = auth.currentUser?.uid

    private fun shelvesRef(uid: String) = db.collection("users").document(uid).collection("shelves")

    private fun mapToShelf(doc: com.google.firebase.firestore.DocumentSnapshot): Shelf {
        val rawMeta = doc.get("bookMetadata") as? Map<String, Map<String, Any>> ?: emptyMap()
        val bookMetadata = rawMeta.mapValues { (_, v) ->
            ShelfBookMeta(
                title = v["title"] as? String ?: "",
                imageUrl = v["imageUrl"] as? String ?: "",
                pageCount = (v["pageCount"] as? Long)?.toInt() ?: 0,
                authors = v["authors"] as? List<String> ?: emptyList()
            )
        }
        return Shelf(
            id = doc.id,
            name = doc.getString("name") ?: "",
            color = doc.getLong("color") ?: 0xFF8D5B4C,
            bookIds = doc.get("bookIds") as? List<String> ?: emptyList(),
            bookMetadata = bookMetadata,
            order = doc.getLong("order")?.toInt() ?: 0,
            isPublic = doc.getBoolean("isPublic") ?: false,
            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
        )
    }

    private fun shelfToMap(shelf: Shelf): Map<String, Any> {
        val metaMap = shelf.bookMetadata.mapValues { (_, v) ->
            mapOf(
                "title" to v.title,
                "imageUrl" to v.imageUrl,
                "pageCount" to v.pageCount,
                "authors" to v.authors
            )
        }
        return mapOf(
            "name" to shelf.name,
            "color" to shelf.color,
            "bookIds" to shelf.bookIds,
            "bookMetadata" to metaMap,
            "order" to shelf.order,
            "isPublic" to shelf.isPublic,
            "createdAt" to shelf.createdAt
        )
    }

    override suspend fun getShelves(uid: String): Result<List<Shelf>> {
        return try {
            val snap = shelvesRef(uid).orderBy("order").get().await()
            val shelves = snap.documents.map { mapToShelf(it) }
            Result.success(shelves)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPublicShelves(uid: String): Result<List<Shelf>> {
        return try {
            val snap = shelvesRef(uid)
                .whereEqualTo("isPublic", true)
                .orderBy("order")
                .get()
                .await()
            val shelves = snap.documents.map { mapToShelf(it) }
            Result.success(shelves)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createShelf(name: String, color: Long, isPublic: Boolean): Result<Shelf> {
        val uid = myUid ?: return Result.failure(Exception("No auth"))
        return try {
            val currentShelves = shelvesRef(uid).get().await().size()
            val ref = shelvesRef(uid).document()
            val shelf = Shelf(
                id = ref.id,
                name = name,
                color = color,
                bookIds = emptyList(),
                bookMetadata = emptyMap(),
                order = currentShelves,
                isPublic = isPublic,
                createdAt = System.currentTimeMillis()
            )
            ref.set(shelfToMap(shelf)).await()
            Result.success(shelf)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateShelf(shelf: Shelf): Result<Boolean> {
        val uid = myUid ?: return Result.failure(Exception("No auth"))
        return try {
            shelvesRef(uid).document(shelf.id).set(shelfToMap(shelf)).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteShelf(shelfId: String): Result<Boolean> {
        val uid = myUid ?: return Result.failure(Exception("No auth"))
        return try {
            shelvesRef(uid).document(shelfId).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addBookToShelf(shelfId: String, bookId: String, meta: ShelfBookMeta): Result<Boolean> {
        val uid = myUid ?: return Result.failure(Exception("No auth"))
        return try {
            val ref = shelvesRef(uid).document(shelfId)
            val doc = ref.get().await()
            val currentIds = (doc.get("bookIds") as? List<String> ?: emptyList()).toMutableList()
            if (!currentIds.contains(bookId)) {
                currentIds.add(bookId)
                val metaMap = mapOf(
                    "title" to meta.title,
                    "imageUrl" to meta.imageUrl,
                    "pageCount" to meta.pageCount,
                    "authors" to meta.authors
                )
                ref.update(
                    mapOf(
                        "bookIds" to currentIds,
                        "bookMetadata.$bookId" to metaMap
                    )
                ).await()
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeBookFromShelf(shelfId: String, bookId: String): Result<Boolean> {
        val uid = myUid ?: return Result.failure(Exception("No auth"))
        return try {
            val ref = shelvesRef(uid).document(shelfId)
            val doc = ref.get().await()
            val currentIds = (doc.get("bookIds") as? List<String> ?: emptyList()).toMutableList()
            currentIds.remove(bookId)
            ref.update(
                mapOf(
                    "bookIds" to currentIds,
                    "bookMetadata.$bookId" to com.google.firebase.firestore.FieldValue.delete()
                )
            ).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun moveBook(fromShelfId: String, toShelfId: String, bookId: String, toIndex: Int): Result<Boolean> {
        val uid = myUid ?: return Result.failure(Exception("No auth"))
        return try {
            if (fromShelfId.isNotEmpty()) {
                removeBookFromShelf(fromShelfId, bookId)
            }
            val ref = shelvesRef(uid).document(toShelfId)
            val doc = ref.get().await()
            val currentIds = (doc.get("bookIds") as? List<String> ?: emptyList()).toMutableList()
            currentIds.remove(bookId)
            val insertIndex = toIndex.coerceIn(0, currentIds.size)
            currentIds.add(insertIndex, bookId)
            ref.update("bookIds", currentIds).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reorderBooks(shelfId: String, bookIds: List<String>): Result<Boolean> {
        val uid = myUid ?: return Result.failure(Exception("No auth"))
        return try {
            shelvesRef(uid).document(shelfId).update("bookIds", bookIds).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleShelfVisibility(shelfId: String, isPublic: Boolean): Result<Boolean> {
        val uid = myUid ?: return Result.failure(Exception("No auth"))
        return try {
            shelvesRef(uid).document(shelfId).update("isPublic", isPublic).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
