package com.example.topbooks.ui.community

/* =============================================================================
 *  MODELOS UI DEL FEED COMUNIDAD (sólo en memoria)
 * =============================================================================
 *  Estos data class son únicamente para la capa de UI. NO se persisten en
 *  Firestore — cuando llegue el momento de conectar datos reales, mapearemos
 *  desde tus Comment/Reply/Review existentes hacia estos modelos en una capa
 *  de mapper sin tocar la UI.
 *
 *  Diseñado para ser independiente de la lógica de persistencia: si mañana
 *  cambian los modelos de Firestore, los mappers se adaptan, y la UI sigue
 *  funcionando con estos data class.
 * ============================================================================= */

/**
 * Tab activa del feed: tres opciones según pidió el usuario.
 */
enum class FeedTab {
    COMMUNITY,  // Posts globales de la comunidad
    FRIENDS,    // Posts solo de amigos (default)
    TOP         // Posts más populares de la semana
}

/**
 * Tipo de post: define cómo se renderiza la card en el feed.
 *  - REVIEW: reseña con texto + estrellas + portada
 *  - QUOTE: cita literaria destacada (renderizado especial: serif italic, borde dorado)
 *  - FINISHED: anuncio "terminó este libro" con rating opcional
 *  - READING: anuncio "está leyendo este libro" (lo que ves en la story-bar)
 */
enum class PostType {
    REVIEW,
    QUOTE,
    FINISHED,
    READING
}

/**
 * Autor del post — info mínima necesaria para renderizar avatar + nombre.
 *
 * @param isFriend true si el usuario actual y el autor son amigos. Determina
 *                 el color del anillo del avatar (dorado vs gris).
 * @param isVerified true si la lectura está verificada (marcó el libro como leído).
 *                    Activa el badge ✓ junto al nombre.
 */
data class PostAuthor(
    val id: String,
    val displayName: String,
    val photoUrl: String?,
    val isFriend: Boolean,
    val isVerified: Boolean
)

/**
 * Libro al que el post hace referencia. Mínima info para renderizar el book-strip.
 */
data class PostBook(
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String?
)

/**
 * Post del feed Comunidad. Es el modelo "unificador" que agrupa los distintos
 * tipos de actividad social: reseñas, citas, "terminó libro", etc.
 *
 * @param id Identificador único del post.
 * @param type Tipo de actividad (afecta cómo se renderiza la card).
 * @param author Autor del post.
 * @param book Libro asociado (puede ser null en citas que no especifican origen).
 * @param createdAtMillis Fecha de creación; se convierte a "hace 2h" en la UI.
 * @param rating Estrellas (1-5) opcionales. Solo aplica a REVIEW y FINISHED.
 * @param body Texto del post (reseña, cita, etc.).
 * @param quoteSource Si es un QUOTE, autor/origen de la cita.
 * @param likeCount Número de likes recibidos.
 * @param commentCount Número de respuestas.
 * @param isLikedByMe ¿Le di like? (estado de la UI, no permanente).
 * @param isSavedByMe ¿Lo guardé? (idem).
 */
data class Post(
    val id: String,
    val type: PostType,
    val author: PostAuthor,
    val book: PostBook?,
    val createdAtMillis: Long,
    val rating: Int? = null,
    val body: String = "",
    val quoteSource: String? = null,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val isSavedByMe: Boolean = false
)

/**
 * Item de la story-bar superior: amigo + libro que está leyendo ahora.
 *
 * @param author Autor (la story-bar siempre muestra amigos).
 * @param currentBook Libro que está leyendo, o null si está en transición.
 * @param hasFinished true si terminó hoy (cambia el badge a ✓).
 */
data class StoryItem(
    val author: PostAuthor,
    val currentBook: PostBook?,
    val hasFinished: Boolean = false
)

/**
 * Estado UI completo de la pantalla CommunityFeed.
 */
data class CommunityFeedUiState(
    val isLoading: Boolean = true,
    val activeTab: FeedTab = FeedTab.FRIENDS,
    val stories: List<StoryItem> = emptyList(),
    val posts: List<Post> = emptyList(),
    val newPostsCountToday: Int = 0,
    val errorMessage: String? = null
)