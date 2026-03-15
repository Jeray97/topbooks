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

/**
 * 1. DEFINICIÓN DE LA INTERFAZ
 * Contrato que define la obtención y gestión de datos sociales dinámicos de la comunidad.
 * * Maneja feeds de actividad, hilos de comentarios, respuestas y escuchas en tiempo real.
 */
interface SocialFeedRepository {
    /** Obtiene todas las reseñas escritas por un usuario específico. */
    suspend fun getUserReviews(userId: String): Result<List<Review>>

    /** Obtiene todos los comentarios (hilos) en los que un usuario ha participado o creado. */
    suspend fun getUserComments(userId: String): Result<List<Comment>>

    /** Obtiene los libros favoritos más recientes de un usuario (útil para el feed de amigos). */
    suspend fun getUserFavorites(userId: String, limit: Long = 5): Result<List<Map<String, Any>>>

    /** Obtiene un feed general con los últimos comentarios de toda la comunidad. */
    suspend fun getCommunityComments(limit: Long = 20): Result<List<Comment>>

    /** Añade una respuesta a un hilo de comentarios existente y dispara una notificación Push. */
    suspend fun addReply(commentId: String, reply: Reply, targetFCMToken: String?, bookId: String): Result<Boolean>

    /** Obtiene un comentario específico por su ID (lectura única). */
    suspend fun getCommentById(commentId: String): Result<Comment>

    /** Obtiene y escucha un comentario específico en tiempo real. */
    fun observeCommentById(commentId: String): Flow<Result<Comment>>

    /** Obtiene las reseñas que tiene el libro asociado. */
    suspend fun getReviewsByBook(bookId: String): Result<List<Review>>
}

/**
 * 2. IMPLEMENTACIÓN DE LA INTERFAZ
 * * Conecta con Firestore para las bases de datos y con [FirebaseFunctions] para la
 * ejecución de código en el servidor (ej. enviar notificaciones).
 */
class SocialFeedRepositoryImpl : SocialFeedRepository {
    private val db = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance()

    override suspend fun getUserReviews(userId: String): Result<List<Review>> {
        return try {
            val snap = db.collection("reviews").whereEqualTo("userId", userId).get().await()
            Result.success(snap.toObjects(Review::class.java))
        } catch (e: Exception) { Result.failure(e) }
    }

    /**
     * Obtiene todos los comentarios relacionados con un usuario.
     * * DISEÑO NOSQL AVANZADO: Realiza dos consultas separadas (creador y participante)
     * y luego las fusiona en memoria. Esto permite crear una experiencia donde el usuario
     * puede seguir un hilo de conversación aunque él no haya sido el creador original.
     */
    override suspend fun getUserComments(userId: String): Result<List<Comment>> {
        return try {
            // 1. Buscamos los comentarios donde eres el CREADOR original
            val snapOwner = db.collection("comments").whereEqualTo("userId", userId).get().await()
            val ownerComments = snapOwner.toObjects(Comment::class.java)

            // 2. Buscamos los comentarios donde solo eres PARTICIPANTE (has respondido)
            val snapParticipant = db.collection("comments").whereArrayContains("participantIds", userId).get().await()
            val participantComments = snapParticipant.toObjects(Comment::class.java)

            // 3. Los juntamos, quitamos duplicados (por si eres creador y participante a la vez)
            // y los ordenamos por fecha de creación de más nuevo a más antiguo.
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

    /**
     * Añade una respuesta a un comentario y notifica al autor.
     * * Utiliza [FieldValue.arrayUnion] para asegurar operaciones atómicas (evita pérdida de
     * datos si dos personas responden en el mismo milisegundo).
     */
    override suspend fun addReply(commentId: String, reply: Reply, targetFCMToken: String?, bookId: String): Result<Boolean> {
        return try {
            val ref = db.collection("comments").document(commentId)

            // Actualizamos el array de respuestas Y añadimos al usuario a la lista de participantes activos
            ref.update(
                "replies", FieldValue.arrayUnion(reply),
                "participantIds", FieldValue.arrayUnion(reply.userId)
            ).await()

            // Disparamos la Cloud Function desde el Repositorio (Capa de datos limpia)
            if (!targetFCMToken.isNullOrEmpty() && targetFCMToken != "NO_TOKEN") {
                val data = hashMapOf(
                    "token" to targetFCMToken,
                    "title" to "Nueva respuesta en tu comentario",
                    "body" to "${reply.userName} ha respondido a tu comentario.",
                    "type" to "NEW_REPLY",
                    "bookId" to bookId,
                    "commentId" to commentId
                )
                // Llama al servidor de Node.js en Firebase para enviar la notificación
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

    /**
     * Escucha un documento en Firebase Firestore de forma continua y reactiva.
     * * Convierte un [addSnapshotListener] tradicional en un Flujo ([Flow]) de Kotlin usando [callbackFlow].
     * Esto permite a la UI reaccionar automáticamente cada vez que alguien añade una respuesta al hilo
     * sin tener que recargar la pantalla.
     */
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

        // Bloque esencial: Se ejecuta automáticamente cuando el Flow deja de recolectar
        // (por ejemplo, cuando el usuario navega hacia atrás y destruye el ViewModel).
        awaitClose { listener.remove() }
    }

    override suspend fun getReviewsByBook(bookId: String): Result<List<Review>> {
        return try {
            val snap = db.collection("reviews")
                .whereEqualTo("bookId", bookId)
                .get().await()
            Result.success(snap.toObjects(Review::class.java))
        } catch (e: Exception) { Result.failure(e) }
    }
}