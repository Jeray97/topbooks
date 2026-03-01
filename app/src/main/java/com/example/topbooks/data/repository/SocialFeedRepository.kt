package com.example.topbooks.data.repository

import com.example.topbooks.data.model.Comment
import com.example.topbooks.data.model.Reply
import com.example.topbooks.data.model.Review
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface SocialFeedRepository {
    suspend fun getUserReviews(userId: String): Result<List<Review>>
    suspend fun getUserComments(userId: String): Result<List<Comment>>
    suspend fun getUserFavorites(userId: String, limit: Long = 5): Result<List<Map<String, Any>>>
    suspend fun getCommunityComments(limit: Long = 20): Result<List<Comment>>
    suspend fun addReply(commentId: String, reply: Reply, targetFCMToken: String?, bookId: String): Result<Boolean>
    suspend fun getCommentById(commentId: String): Result<Comment>
    // 🔥 NUEVO: Función para observar los cambios en tiempo real
    fun observeCommentById(commentId: String): Flow<Result<Comment>>
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
            // 1. Buscamos los comentarios donde eres el CREADOR
            val snapOwner = db.collection("comments").whereEqualTo("userId", userId).get().await()
            val ownerComments = snapOwner.toObjects(Comment::class.java)

            // 2. Buscamos los comentarios donde eres PARTICIPANTE (respuestas)
            val snapParticipant = db.collection("comments").whereArrayContains("participantIds", userId).get().await()
            val participantComments = snapParticipant.toObjects(Comment::class.java)

            // 3. Los juntamos, quitamos duplicados y los ordenamos por fecha
            val allComments = (ownerComments + participantComments)
                .distinctBy { it.commentId }
                .sortedByDescending { it.createAt }

            Result.success(allComments)
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

            // Actualizamos las respuestas Y añadimos al usuario a los participantes
            ref.update(
                "replies", FieldValue.arrayUnion(reply),
                "participantIds", FieldValue.arrayUnion(reply.userId)
            ).await()

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

    override suspend fun getCommentById(commentId: String): Result<Comment> {
        return try {
            val snap = db.collection("comments").document(commentId).get().await()
            val comment = snap.toObject(Comment::class.java)
            if (comment != null) Result.success(comment)
            else Result.failure(Exception("Comentario no encontrado"))
        } catch (e: Exception) { Result.failure(e) }
    }

    // Escucha el documento en tiempo real
    override fun observeCommentById(commentId: String): Flow<Result<Comment>> = callbackFlow {
        val listener = db.collection("comments").document(commentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val comment = snapshot.toObject(Comment::class.java)
                    if (comment != null) {
                        trySend(Result.success(comment))
                    }
                } else {
                    trySend(Result.failure(Exception("Comentario eliminado o no encontrado")))
                }
            }

        // Se ejecuta cuando el Flow deja de recolectar (por ejemplo, al salir de la pantalla)
        awaitClose { listener.remove() }
    }
}