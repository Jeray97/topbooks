package com.example.topbooks.ui.navigation

import com.example.topbooks.R

/**
 * Representa los elementos individuales de la barra de navegación inferior (Bottom Navigation Bar).
 *
 * Utiliza una [sealed class] para garantizar la seguridad de tipos, permitiendo que el sistema
 * de navegación de Compose reconozca únicamente los destinos definidos aquí.
 *
 * @property route Identificador único de la ruta para el NavHost.
 * @property title Identificador del recurso de cadena (String Resource) para la etiqueta visual.
 * @property icon Identificador del recurso gráfico (Drawable Resource) para el icono de la pestaña.
 */
sealed class BottomNavItem(val route: String, val title: Int, val icon: Int) {

    /** Destino de la pantalla de Inicio y descubrimiento. */
    object Home : BottomNavItem(
        route = "home_tab",
        title = R.string.home_bottom,
        icon = R.drawable.home_icon
    )

    /** Destino para el seguimiento del progreso de lectura y biblioteca. */
    object Progress : BottomNavItem(
        route = "progress_tab",
        title = R.string.progress_bottom,
        icon = R.drawable.progreso
    )

    /** Destino de la sección social y búsqueda de amigos. */
    object Friends : BottomNavItem(
        route = "friends_tab",
        title = R.string.social_bottom,
        icon = R.drawable.social
    )

    /** Destino para el feed de reseñas y actividad de la comunidad. */
    object Reviews : BottomNavItem(
        route = "reviews_tab",
        title = R.string.reviews_bottom,
        icon = R.drawable.resenas
    )

    /** Destino para el perfil del usuario, estadísticas y configuración. */
    object Profile : BottomNavItem(
        route = "profile_tab",
        title = R.string.profile_bottom,
        icon = R.drawable.perfil
    )
}