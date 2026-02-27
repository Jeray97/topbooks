package com.example.topbooks.data.repository

import com.example.topbooks.data.model.Comment
import com.example.topbooks.data.model.Reply
import com.example.topbooks.data.model.Review
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

interface SocialFeedRepository {
    suspend fun getUserReviews(userId: String): Result<List<Review>>
    suspend fun getUserComments(userId: String): Result<List<Comment>>
    suspend fun getUserFavorites(userId: String, limit: Long = 5): Result<List<Map<String, Any>>>
    suspend fun getCommunityComments(limit: Long = 20): Result<List<Comment>>
    suspend fun addReply(commentId: String, reply: Reply, targetFCMToken: String?, bookId: String): Result<Boolean>
}

class SocialFeedRepositoryImpl : SocialFeedRepository {
    private val db = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance()

    override suspend fun getUserReviews(userId: String): Result<List<Review>> {
        return try {
            val snap = db.collection("reviews").whereEqualTo("userId", userId).get().await()
            Result.success(snap.toObjects(Review::class.java))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun getUserComments(userId: String): Result<List<Comment>> {
        return try {
            val snap = db.collection("comments").whereEqualTo("userId", userId).get().await()
            Result.success(snap.toObjects(Comment::class.java))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun getUserFavorites(userId: String, limit: Long): Result<List<Map<String, Any>>> {
        return try {
            val snap = db.collection("users").document(userId).collection("favorites")
                .orderBy("addedAt", Query.Direction.DESCENDING).limit(limit).get().await()
            val favs = snap.documents.mapNotNull {
                mapOf(
                    "bookId" to (it.getString("bookId") ?: ""),
                    "addedAt" to (it.getLong("addedAt") ?: 0L)
                )
            }
            Result.success(favs)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun getCommunityComments(limit: Long): Result<List<Comment>> {
        return try {
            val snap = db.collection("comments")
                .orderBy("createAt", Query.Direction.DESCENDING)
                .limit(limit).get().await()
            Result.success(snap.toObjects(Comment::class.java))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun addReply(commentId: String, reply: Reply, targetFCMToken: String?, bookId: String): Result<Boolean> {
        return try {
            val ref = db.collection("comments").document(commentId)
            ref.update("replies", FieldValue.arrayUnion(reply)).await()

            // Disparamos la Cloud Function desde el Repositorio, dejando el ViewModel limpio
            if (!targetFCMToken.isNullOrEmpty() && targetFCMToken != "NO_TOKEN") {
                val data = hashMapOf(
                    "token" to targetFCMToken,
                    "title" to "Nueva respuesta en tu comentario",
                    "body" to "${reply.userName} ha respondido a tu comentario.",
                    "type" to "NEW_REPLY",
                    "bookId" to bookId,
                    "commentId" to commentId
                )
                functions.getHttpsCallable("enviarNotificacionRespuesta").call(data)
            }
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }
}