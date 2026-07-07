package com.example.topbooks.ui.community

/* =============================================================================
 *  MODELOS UI DEL DETALLE DE POST (Mockup 2)
 * =============================================================================
 *  Reúne la información necesaria para renderizar una pantalla con:
 *    - El post principal (Post ya existente del feed).
 *    - Las reacciones agregadas con contador.
 *    - El hilo plano de respuestas (con badge "autora" si responde quien
 *      escribió la reseña original).
 *
 *  Como en CommunityModels.kt, son data classes en memoria. Cuando conectemos
 *  Firestore, mapearemos desde tus Comment/Reply hacia estos.
 * ============================================================================= */

/**
 * Reacción agregada a un post: emoji + cuántos usuarios reaccionaron + si lo
 * hizo el usuario actual.
 *
 * @param emoji El emoji concreto (e.g. "❤️", "📚", "🥲", "🔥", "😮").
 * @param count Número total de personas que han reaccionado con ese emoji.
 * @param reactedByMe true si el usuario actual lo eligió.
 */
data class Reaction(
    val emoji: String,
    val count: Int,
    val reactedByMe: Boolean
)

/**
 * Una respuesta dentro del hilo de un post. Hilo plano: no hay anidación
 * (tu decisión: no Reddit-style).
 *
 * @param id ID único de la respuesta.
 * @param author Autor de la respuesta.
 * @param body Texto de la respuesta.
 * @param createdAtMillis Fecha (se renderiza relativa: "hace 1h").
 * @param likeCount Número de likes que ha recibido la respuesta.
 * @param isLikedByMe Si el usuario actual le dio like.
 * @param isFromOriginalAuthor true si quien responde es la persona que
 *                              escribió el post original. Activa el badge
 *                              dorado "autora" y el fondo con tinte cálido.
 */
data class PostReply(
    val id: String,
    val author: PostAuthor,
    val body: String,
    val createdAtMillis: Long,
    val likeCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val isFromOriginalAuthor: Boolean = false
)

/**
 * Estado UI completo de la pantalla PostDetail.
 *
 * @param isLoading Cargando el post o sus respuestas (estado inicial).
 * @param post El post principal — null si todavía no llegó o si no existe.
 * @param reactions Lista de reacciones agregadas al post (orden por count desc).
 * @param replies Hilo plano de respuestas (orden cronológico ascendente).
 * @param totalReactionCount Suma de todas las reacciones (resumen "12 reacciones").
 * @param savedCount Cuántas personas guardaron el post (resumen "5 guardados").
 * @param isSendingReply true mientras se envía una nueva respuesta.
 * @param emojiPickerOpen true cuando el usuario abrió el selector de "+ reaccionar".
 * @param errorMessage Texto de error a mostrar (toast/snackbar) — null si no hay.
 */
data class PostDetailUiState(
    val isLoading: Boolean = true,
    val post: Post? = null,
    val reactions: List<Reaction> = emptyList(),
    val replies: List<PostReply> = emptyList(),
    val totalReactionCount: Int = 0,
    val savedCount: Int = 0,
    val isSendingReply: Boolean = false,
    val emojiPickerOpen: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Lista de emojis disponibles en el selector "+ reaccionar". Los 3 primeros
 * (❤️📚🥲) son los "top fijos" que aparecen siempre como pills clickables;
 * los demás aparecen al pulsar el botón "+".
 *
 * Pensados para una app de libros: emociones de lectura ("me llegó al alma",
 * "cambió mi forma de pensar"), no genéricos de RRSS.
 */
val POPULAR_REACTIONS = listOf(
    "❤️",   // Me encantó
    "📚",   // Lo añado a leer
    "🥲",   // Me hizo llorar
    "🔥",   // Brutal
    "😮",   // Sorpresa / no me lo esperaba
    "🤔",   // Me hizo pensar
    "🤣",   // Me reí mucho
    "💔",   // Me rompió
    "✨",   // Mágico
    "🌟"    // Imprescindible
)

/**
 * Los 3 emojis "top" que se muestran siempre como pills incluso si el contador
 * es 0. El resto aparece solo cuando alguien los ha usado.
 */
val TOP_FIXED_REACTIONS = listOf("❤️", "📚", "🥲")