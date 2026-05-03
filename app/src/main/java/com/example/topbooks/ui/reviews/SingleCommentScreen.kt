package com.example.topbooks.ui.reviews

import androidx.compose.runtime.Composable
import com.example.topbooks.ui.community.PostDetailScreen

/**
 * PANTALLA DE DETALLE DE COMENTARIO/POST.
 *
 * Hasta v8 esta pantalla mostraba un Comment original con sus Reply en formato
 * de hilo. A partir de v9 (rediseño del feed comunidad), delega al nuevo
 * PostDetailScreen (Mockup 2) que muestra el post con:
 *   - Reacciones múltiples agregadas (❤️📚🥲 + selector de más emojis)
 *   - Hilo plano de respuestas con badge "autora" dorado cuando aplica
 *   - Stats bar: "X reacciones · Y respuestas · Z guardados"
 *   - Compose bar fija abajo para responder
 *
 * El parámetro [commentId] se interpreta como `postId` del nuevo feed.
 *
 * @param commentId Identificador del post a mostrar (anteriormente era un
 *                  Comment ID; ahora es un Post ID del nuevo feed).
 * @param onBackClick Acción para regresar a la pantalla anterior.
 */
@Composable
fun SingleCommentScreen(
    commentId: String,
    onBackClick: () -> Unit
) {
    PostDetailScreen(
        postId = commentId,
        onBackClick = onBackClick
    )
}