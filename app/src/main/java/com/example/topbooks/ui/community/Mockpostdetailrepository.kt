package com.example.topbooks.ui.community

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID

/* =============================================================================
 *  EXTENSIÓN DEL MOCK CON DATOS DE DETALLE DE POST
 * =============================================================================
 *  Mantenemos los datos de respuestas y reacciones en mapas mutables in-memory
 *  para que las acciones del usuario (responder, reaccionar, dar like) se vean
 *  reflejadas durante toda la sesión. Al reiniciar la app vuelve a su estado
 *  inicial — eso ya lo solucionará el repo real conectado a Firestore.
 * ============================================================================= */

object MockPostDetailRepository {

    // ─────────────────────────────────────────────────────────────────────
    // ESTADO IN-MEMORY (se modifica con las acciones del usuario)
    // ─────────────────────────────────────────────────────────────────────

    /** ReactionMap: postId → mapa de emoji → contador. */
    private val reactionsByPost: MutableMap<String, MutableMap<String, Int>> = mutableMapOf()

    /** UsuarioActual reacciones: postId → set de emojis a los que dio el usuario. */
    private val myReactionsByPost: MutableMap<String, MutableSet<String>> = mutableMapOf()

    /** RepliesByPost: postId → lista mutable de respuestas. */
    private val repliesByPost: MutableMap<String, MutableList<PostReply>> = mutableMapOf()

    /** SavedCount: cuánta gente guardó cada post (independiente de "yo"). */
    private val savedCountByPost: MutableMap<String, Int> = mutableMapOf()

    init {
        seedInitialData()
    }

    // ─────────────────────────────────────────────────────────────────────
    // API PÚBLICA
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Devuelve el detalle completo de un post: post + reacciones agregadas + replies.
     */
    fun getPostDetail(postId: String): Flow<PostDetailSnapshot?> = flow {
        delay(200)
        val post = MockCommunityRepository_findPost(postId)
        if (post == null) {
            emit(null)
            return@flow
        }

        emit(buildSnapshot(post))
    }

    /**
     * Toggle de reacción del usuario actual al post. Si ya había reaccionado
     * con ese emoji, lo quita; si no, lo añade.
     */
    fun toggleReaction(postId: String, emoji: String): PostDetailSnapshot? {
        val post = MockCommunityRepository_findPost(postId) ?: return null
        val myReactions = myReactionsByPost.getOrPut(postId) { mutableSetOf() }
        val emojiCounts = reactionsByPost.getOrPut(postId) { mutableMapOf() }

        if (myReactions.contains(emoji)) {
            // Quitar la reacción
            myReactions.remove(emoji)
            emojiCounts[emoji] = (emojiCounts[emoji] ?: 1) - 1
            if ((emojiCounts[emoji] ?: 0) <= 0) {
                emojiCounts.remove(emoji)
            }
        } else {
            // Añadir la reacción
            myReactions.add(emoji)
            emojiCounts[emoji] = (emojiCounts[emoji] ?: 0) + 1
        }

        return buildSnapshot(post)
    }

    /**
     * Añade una nueva respuesta al hilo. Devuelve el snapshot actualizado.
     *
     * @param postId Post al que se responde.
     * @param body Texto de la respuesta.
     * @param currentUserAuthor Datos del usuario actual (autor de la respuesta).
     * @param postOriginalAuthorId ID del autor original del post — si coincide
     *                              con currentUserAuthor.id, marcamos la respuesta
     *                              con isFromOriginalAuthor=true (badge "autora").
     */
    fun addReply(
        postId: String,
        body: String,
        currentUserAuthor: PostAuthor,
        postOriginalAuthorId: String
    ): PostDetailSnapshot? {
        val post = MockCommunityRepository_findPost(postId) ?: return null
        val replies = repliesByPost.getOrPut(postId) { mutableListOf() }

        val reply = PostReply(
            id = "r_${UUID.randomUUID()}",
            author = currentUserAuthor,
            body = body,
            createdAtMillis = System.currentTimeMillis(),
            likeCount = 0,
            isLikedByMe = false,
            isFromOriginalAuthor = currentUserAuthor.id == postOriginalAuthorId
        )
        replies.add(reply)

        return buildSnapshot(post)
    }

    /**
     * Toggle de like sobre una respuesta concreta.
     */
    fun toggleReplyLike(postId: String, replyId: String): PostDetailSnapshot? {
        val post = MockCommunityRepository_findPost(postId) ?: return null
        val replies = repliesByPost[postId] ?: return buildSnapshot(post)

        val idx = replies.indexOfFirst { it.id == replyId }
        if (idx >= 0) {
            val current = replies[idx]
            replies[idx] = current.copy(
                isLikedByMe = !current.isLikedByMe,
                likeCount = current.likeCount + (if (current.isLikedByMe) -1 else 1)
            )
        }
        return buildSnapshot(post)
    }

    // ─────────────────────────────────────────────────────────────────────
    // LÓGICA INTERNA
    // ─────────────────────────────────────────────────────────────────────

    /** Construye el snapshot a partir del estado in-memory actual. */
    private fun buildSnapshot(post: Post): PostDetailSnapshot {
        val emojiCounts = reactionsByPost[post.id] ?: emptyMap()
        val myReactions = myReactionsByPost[post.id] ?: emptySet()

        // Pills a mostrar: top fijos siempre + cualquier otro emoji con count > 0.
        val emojisToShow = (TOP_FIXED_REACTIONS + emojiCounts.keys).distinct()
        val reactions = emojisToShow.mapNotNull { emoji ->
            val count = emojiCounts[emoji] ?: 0
            // Top fijos siempre se muestran (count puede ser 0); otros sólo si count > 0
            if (emoji in TOP_FIXED_REACTIONS || count > 0) {
                Reaction(
                    emoji = emoji,
                    count = count,
                    reactedByMe = myReactions.contains(emoji)
                )
            } else null
        }.sortedWith(
            // Top fijos primero, luego por count descendente
            compareByDescending<Reaction> { it.emoji in TOP_FIXED_REACTIONS }
                .thenByDescending { it.count }
        )

        val replies = repliesByPost[post.id]?.toList() ?: emptyList()
        val totalReactions = emojiCounts.values.sum()

        return PostDetailSnapshot(
            post = post,
            reactions = reactions,
            replies = replies.sortedBy { it.createdAtMillis },
            totalReactionCount = totalReactions,
            savedCount = savedCountByPost[post.id] ?: 0
        )
    }

    /**
     * Llena los mapas con datos iniciales coherentes con los posts mock.
     * Reacciones y respuestas que veíamos en el mockup 2 para Tokio Blues.
     */
    private fun seedInitialData() {
        // Post Tokio Blues (id "p_1"): reacciones múltiples y 3 respuestas
        reactionsByPost["p_1"] = mutableMapOf("❤️" to 8, "📚" to 3, "🥲" to 2)
        myReactionsByPost["p_1"] = mutableSetOf("❤️")
        savedCountByPost["p_1"] = 5
        repliesByPost["p_1"] = mutableListOf(
            PostReply(
                id = "r_1",
                author = PostAuthor("u_marcos", "Marcos Ruiz", null, isFriend = true, isVerified = true),
                body = "Lo empecé el mes pasado y no pude soltarlo. Coincido en que la atmósfera es lo mejor del libro. ¿Has leído Kafka en la orilla?",
                createdAtMillis = System.currentTimeMillis() - 60 * 60_000,
                likeCount = 4,
                isLikedByMe = true,
                isFromOriginalAuthor = false
            ),
            PostReply(
                id = "r_2",
                author = PostAuthor("u_lucia", "Lucía Martín", null, isFriend = true, isVerified = true),
                body = "¡Sí! Kafka es mi favorito. Si te gustó este, ese te va a encantar. Tiene un toque más onírico.",
                createdAtMillis = System.currentTimeMillis() - 45 * 60_000,
                likeCount = 2,
                isLikedByMe = true,
                isFromOriginalAuthor = true
            ),
            PostReply(
                id = "r_3",
                author = PostAuthor("u_ana", "Ana López", null, isFriend = true, isVerified = false),
                body = "Me lo apunto 📚 ¿alguna recomendación para empezar con Murakami?",
                createdAtMillis = System.currentTimeMillis() - 20 * 60_000,
                likeCount = 1,
                isLikedByMe = false,
                isFromOriginalAuthor = false
            )
        )

        // Post de cita de Niebla (p_2): pocas reacciones, sin respuestas
        reactionsByPost["p_2"] = mutableMapOf("❤️" to 8, "📚" to 1)
        savedCountByPost["p_2"] = 12

        // Post Sapiens (p_3): rating 4, 5 likes
        reactionsByPost["p_3"] = mutableMapOf("❤️" to 5)
        savedCountByPost["p_3"] = 8
    }

    /**
     * Hack para acceder al post desde MockCommunityRepository sin tocarlo.
     * En la versión real esto vendría del repo de Firestore por ID.
     */
    private fun MockCommunityRepository_findPost(id: String): Post? {
        return MockCommunityRepository.findPostById(id)
    }
}

/**
 * Snapshot inmutable que devuelve el repo. Se separa del UiState para que el
 * repo no dependa de tipos UI (ej. isSendingReply, emojiPickerOpen son
 * cosas de la pantalla, no del repo).
 */
data class PostDetailSnapshot(
    val post: Post,
    val reactions: List<Reaction>,
    val replies: List<PostReply>,
    val totalReactionCount: Int,
    val savedCount: Int
)