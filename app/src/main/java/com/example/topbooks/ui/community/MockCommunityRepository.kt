package com.example.topbooks.ui.community

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/* =============================================================================
 *  MOCK REPOSITORY — datos de prueba en memoria
 * =============================================================================
 *  Devuelve un feed hardcodeado con posts variados (reseñas, citas, terminados)
 *  para validar la UI sin depender de Firestore.
 *
 *  Cuando esté listo para producción, sustituiremos esta clase por una
 *  CommunityRepository real que lea de Firestore. La UI no cambia: mismo
 *  contrato Flow<...>.
 * ============================================================================= */

object MockCommunityRepository {

    /**
     * Devuelve la story-bar de amigos lectores. Un primer item especial
     * representa "tu lectura" (lo añade la UI, aquí solo van amigos).
     */
    fun getStories(): Flow<List<StoryItem>> = flow {
        delay(150)  // Simulamos latencia de red
        emit(MOCK_STORIES)
    }

    /**
     * Devuelve los posts del feed según la tab activa.
     */
    fun getFeed(tab: FeedTab): Flow<List<Post>> = flow {
        delay(250)
        emit(when (tab) {
            FeedTab.FRIENDS -> MOCK_POSTS.filter { it.author.isFriend }
            FeedTab.COMMUNITY -> MOCK_POSTS  // Todos
            FeedTab.TOP -> MOCK_POSTS.sortedByDescending { it.likeCount }.take(10)
        })
    }

    /**
     * Devuelve el contador de posts nuevos de hoy (para el subtítulo).
     */
    fun getNewPostsCountToday(): Int = 3

    /**
     * Toggle local de like — devuelve el post actualizado para que el ViewModel
     * actualice su StateFlow. En producción esto haría una escritura a Firestore.
     */
    fun toggleLike(post: Post): Post = post.copy(
        isLikedByMe = !post.isLikedByMe,
        likeCount = post.likeCount + (if (post.isLikedByMe) -1 else 1)
    )

    fun toggleSave(post: Post): Post = post.copy(
        isSavedByMe = !post.isSavedByMe
    )

    fun findPostById(id: String): Post? = MOCK_POSTS.find { it.id == id }
}

// ─────────────────────────────────────────────────────────────────────────────
// DATOS HARDCODEADOS (cambia los nombres / portadas a tu gusto para probar)
// ─────────────────────────────────────────────────────────────────────────────

private val authorLucia = PostAuthor("u_lucia", "Lucía Martín", null, isFriend = true, isVerified = true)
private val authorMarcos = PostAuthor("u_marcos", "Marcos Ruiz", null, isFriend = true, isVerified = true)
private val authorAna = PostAuthor("u_ana", "Ana López", null, isFriend = true, isVerified = false)
private val authorPablo = PostAuthor("u_pablo", "Pablo Soto", null, isFriend = true, isVerified = false)
private val authorSara = PostAuthor("u_sara", "Sara Vidal", null, isFriend = false, isVerified = true)
private val authorDavid = PostAuthor("u_david", "David Quiroga", null, isFriend = false, isVerified = false)

private val bookTokio = PostBook("b_1", "Tokio Blues", "Haruki Murakami", null)
private val bookSapiens = PostBook("b_2", "Sapiens", "Yuval Noah Harari", null)
private val bookNiebla = PostBook("b_3", "Niebla", "Miguel de Unamuno", null)
private val bookSombras = PostBook("b_4", "Sombras de Grey", "E. L. James", null)
private val bookCienAnos = PostBook("b_5", "Cien años de soledad", "Gabriel García Márquez", null)
private val bookKafka = PostBook("b_6", "Kafka en la orilla", "Haruki Murakami", null)

private val now = System.currentTimeMillis()
private const val HOUR = 3_600_000L
private const val DAY = 86_400_000L

private val MOCK_STORIES = listOf(
    StoryItem(authorLucia, bookTokio),
    StoryItem(authorMarcos, bookSapiens),
    StoryItem(authorAna, bookCienAnos, hasFinished = true),
    StoryItem(authorPablo, bookSombras),
    StoryItem(authorSara, bookKafka)
)

private val MOCK_POSTS = listOf(
    Post(
        id = "p_1",
        type = PostType.REVIEW,
        author = authorLucia,
        book = bookTokio,
        createdAtMillis = now - 2 * HOUR,
        rating = 5,
        body = "Una novela melancólica que te marca para siempre. Murakami construye personajes tan reales que dueles con ellos. La escena del piano me hizo llorar...",
        likeCount = 12,
        commentCount = 3,
        isLikedByMe = true,
        isSavedByMe = false
    ),
    Post(
        id = "p_2",
        type = PostType.QUOTE,
        author = authorAna,
        book = bookNiebla,
        createdAtMillis = now - 6 * HOUR,
        body = "En algún lugar de un libro hay una frase esperándonos para darle un sentido a la existencia.",
        quoteSource = "Miguel de Unamuno · Niebla",
        likeCount = 8,
        commentCount = 1,
        isLikedByMe = false,
        isSavedByMe = true
    ),
    Post(
        id = "p_3",
        type = PostType.FINISHED,
        author = authorMarcos,
        book = bookSapiens,
        createdAtMillis = now - 1 * DAY,
        rating = 4,
        body = "Mi libro favorito de no-ficción este año. Cambia tu forma de ver la humanidad.",
        likeCount = 5,
        commentCount = 0,
        isLikedByMe = false,
        isSavedByMe = false
    ),
    Post(
        id = "p_4",
        type = PostType.REVIEW,
        author = authorPablo,
        book = bookSombras,
        createdAtMillis = now - 2 * DAY,
        rating = 3,
        body = "Más entretenido de lo que esperaba. No es alta literatura pero engancha y se lee rápido. Perfecto para playa.",
        likeCount = 4,
        commentCount = 2
    ),
    Post(
        id = "p_5",
        type = PostType.QUOTE,
        author = authorSara,
        book = bookCienAnos,
        createdAtMillis = now - 3 * DAY,
        body = "Muchos años después, frente al pelotón de fusilamiento, el coronel Aureliano Buendía había de recordar aquella tarde remota en que su padre lo llevó a conocer el hielo.",
        quoteSource = "Gabriel García Márquez · Cien años de soledad",
        likeCount = 24,
        commentCount = 5
    ),
    Post(
        id = "p_6",
        type = PostType.FINISHED,
        author = authorDavid,
        book = bookKafka,
        createdAtMillis = now - 5 * DAY,
        rating = 5,
        body = "Un viaje absoluto. Si te gustó Tokio Blues no puedes no leer este.",
        likeCount = 18,
        commentCount = 4
    )
)