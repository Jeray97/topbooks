package com.example.topbooks.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.repository.BooksRepository
import com.example.topbooks.data.repository.CommunityRepository
import com.example.topbooks.data.repository.CommunityRepositoryImpl
import com.example.topbooks.data.repository.SocialFeedRepository
import com.example.topbooks.data.repository.SocialFeedRepositoryImpl
import com.example.topbooks.data.repository.UserRepository
import com.example.topbooks.data.repository.UserRepositoryImpl
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Modelo de datos visual (UI Model) que representa a un usuario dentro del contexto social.
 * * Simplifica los datos crudos de Firebase, conservando solo lo necesario para mostrar
 * la lista de amigos, sugerencias o resultados de búsqueda.
 */
data class SocialUser(
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val isFriend: Boolean = false, // Determina si la UI debe pintar un '+' o un 'tick' verde
    val tastes: List<String> = emptyList()
)

/**
 * Agrupa todo el estado de la pantalla [FriendsScreen] en una sola clase de datos reactiva.
 */
data class FriendsState(
    val searchQuery: String = "",
    val searchResults: List<SocialUser> = emptyList(),
    val friendsIds: Set<String> = emptySet(),
    val suggestedUsers: List<SocialUser> = emptyList(),
    val myFriends: List<SocialUser> = emptyList(),
    val recentInteractions: List<Interaction> = emptyList(),
    val isLoading: Boolean = false,
    val isSearching: Boolean = false
)

/**
 * ViewModel que gestiona la lógica de la pestaña "Amigos".
 * * Conecta la UI con los repositorios para realizar búsquedas, cargar sugerencias y gestionar
 * las amistades utilizando técnicas avanzadas de optimización de red (Debounce) y UI Optimista.
 */
class FriendsViewModel(
    // Inyectamos nuestros dos repositorios limpios
    private val communityRepository: CommunityRepository = CommunityRepositoryImpl(),
    private val userRepository: UserRepository = UserRepositoryImpl(),
    private val feedRepository: SocialFeedRepository = SocialFeedRepositoryImpl(),
    private val booksRepository: BooksRepository = BooksRepository()
) : ViewModel() {

    // --- ESTADO REACTIVO ---
    private val _uiState = MutableStateFlow(FriendsState())
    val uiState: StateFlow<FriendsState> = _uiState.asStateFlow()

    // Variable para controlar el trabajo de búsqueda y poder cancelarlo si el usuario escribe muy rápido
    private var searchJob: Job? = null

    init {
        loadInitialData()
    }

    /**
     * Carga silenciosa de datos iniciales al abrir la pantalla.
     * Descarga la lista de amigos actuales y calcula usuarios sugeridos.
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val currentUserId = userRepository.getCurrentUserId() ?: return@launch

            // 1. Obtenemos mis amigos completos usando la función optimizada de la lección anterior
            val friendsList = userRepository.getFriendsList(currentUserId).getOrDefault(emptyList())

            // Mapeamos los datos para la UI y extraemos los IDs
            val socialFriends = friendsList.map {
                SocialUser(uid = it.uid, displayName = it.name, photoUrl = it.photo, isFriend = true)
            }
            val friendsIds = socialFriends.map { it.uid }.toSet()

            // 2. Obtenemos sugerencias
            val suggested = communityRepository.getSuggestedUsers(15).getOrDefault(emptyList())

            val socialSuggested = suggested.filter {
                it.uid != currentUserId && !friendsIds.contains(it.uid)
            }.map { user ->
                SocialUser(uid = user.uid, displayName = user.displayName, photoUrl = user.photoURL, isFriend = false)
            }

            // Actualizamos to-do el estado de golpe
            _uiState.update {
                it.copy(
                    myFriends = socialFriends, // Guardamos la lista completa
                    friendsIds = friendsIds,   // Guardamos los IDs para cruzar datos
                    suggestedUsers = socialSuggested,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Se llama cada vez que el usuario teclea una nueva letra en el buscador.
     * * TÉCNICA DE OPTIMIZACIÓN (DEBOUNCE): Utiliza [delay] de 500ms para esperar a que el usuario
     * termine de escribir antes de enviar la petición a Firebase. Esto evita hacer cientos de
     * peticiones basura a la base de datos, ahorrando costes y batería.
     *
     * @param query Texto actual escrito en el buscador.
     */
    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }

        // Cancelamos la búsqueda anterior si el usuario teclea una nueva letra rápido
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(500) // Debounce: Esperamos medio segundo antes de ir a Firebase

            _uiState.update { it.copy(isSearching = true) }

            val users = communityRepository.searchUsers(query).getOrDefault(emptyList())
            val friendsIds = _uiState.value.friendsIds

            // Mapeamos cruzando con la lista local de amigos para saber a quién pintarle el tick verde
            val results = users.map { user ->
                SocialUser(
                    uid = user.uid,
                    displayName = user.displayName,
                    photoUrl = user.photoURL,
                    isFriend = friendsIds.contains(user.uid)
                )
            }

            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    /**
     * Añade o elimina a un usuario de nuestra lista de amigos.
     * * TÉCNICA VISUAL (OPTIMISTIC UI): Primero cambia el botón en pantalla instantáneamente,
     * y luego envía la petición. Si Firebase falla por falta de internet, revierte el botón.
     */
    fun toggleFriend(user: SocialUser) {
        val myUid = userRepository.getCurrentUserId() ?: return
        val isCurrentlyFriend = user.isFriend
        val newFriendStatus = !isCurrentlyFriend

        // UI Optimista: Cambiamos el estado visualmente al instante
        updateUserFriendStatus(user.uid, newFriendStatus)

        viewModelScope.launch {
            // Reutilizamos la función del UserRepository que hicimos antes.
            userRepository.toggleFriendship(
                myUid = myUid,
                targetUid = user.uid,
                targetName = user.displayName,
                targetPhoto = user.photoUrl,
                isAdding = newFriendStatus
            ).onFailure {
                // Rollback: Si falla el internet o el servidor, deshacemos el cambio visual
                updateUserFriendStatus(user.uid, isCurrentlyFriend)
            }
        }
    }

    /**
     * Función auxiliar (Helper) para actualizar el estado de "isFriend" en TODAS las listas de la UI
     * de golpe (Búsqueda, Sugerencias y Conjunto de IDs) sin tener que repetir el código de mapeo.
     */
    private fun updateUserFriendStatus(uid: String, isFriend: Boolean) {
        _uiState.update { state ->
            val newFriendsIds = if (isFriend) state.friendsIds + uid else state.friendsIds - uid

            val newSearch = state.searchResults.map {
                if (it.uid == uid) it.copy(isFriend = isFriend) else it
            }

            val newSuggested = state.suggestedUsers.map {
                if (it.uid == uid) it.copy(isFriend = isFriend) else it
            }

            state.copy(
                friendsIds = newFriendsIds,
                searchResults = newSearch,
                suggestedUsers = newSuggested
            )
        }
    }

    /**
     * Obtiene un resumen rápido de las últimas interacciones de los amigos
     * para mostrarlas en el Dashboard principal. (VERSIÓN OPTIMIZADA)
     */
    fun refreshRecentActivity() {
        viewModelScope.launch {
            try {
                val friendsIds = _uiState.value.friendsIds.toList()
                if (friendsIds.isEmpty()) return@launch

                // 1. EXTRAER: Buscamos la actividad de TODOS los amigos en paralelo
                val activitiesDeferred = coroutineScope {
                    friendsIds.map { friendId ->
                        async {
                            val user = userRepository.getUserProfile(friendId).getOrNull() ?: return@async emptyList()
                            val friendName = user.displayName.ifEmpty { "Usuario" }
                            val friendPhoto = user.photoURL.ifEmpty { "capibara_1" }

                            // Descargamos reseñas y comentarios
                            val reviews = feedRepository.getUserReviews(friendId).getOrDefault(emptyList())
                            val comments = feedRepository.getUserComments(friendId).getOrDefault(emptyList())

                            // Usamos un Pair para guardar temporalmente el Timestamp y poder ordenar después
                            val userInteractions = mutableListOf<Pair<Long, Interaction>>()

                            reviews.forEach { r ->
                                val time = r.createAt?.time ?: 0L
                                //Guardamos el bookId en el campo bookTitle temporalmente
                                userInteractions.add(Pair(time, Interaction(friendPhoto, friendName, "ha valorado", r.bookId)))
                            }

                            comments.forEach { c ->
                                val time = c.createAt?.time ?: 0L
                                userInteractions.add(Pair(time, Interaction(friendPhoto, friendName, "ha comentado en", c.bookId)))
                            }

                            userInteractions
                        }
                    }
                }

                // 2. ORDENAR: Esperamos los datos, aplanamos la lista, ordenamos por fecha y cogemos las 3 últimas
                val top3Activities = activitiesDeferred.awaitAll()
                    .flatten()
                    .sortedByDescending { it.first } // it.first es el Timestamp
                    .take(3)

                // 3. HIDRATAR: Ahora sí, hacemos SOLO 3 peticiones a la API para conseguir los nombres reales de los libros
                val finalInteractions = top3Activities.map { (_, interaction) ->
                    // interaction.bookTitle tiene el ID del libro guardado del paso anterior
                    val book = booksRepository.getBookDetail(interaction.bookTitle).getOrNull()

                    // Creamos una copia final con el título de verdad
                    interaction.copy(bookTitle = book?.title ?: "un libro")
                }

                _uiState.update { it.copy(recentInteractions = finalInteractions) }

            } catch (e: Exception) {
                // Fallo silencioso: si no carga el resumen, la sección mostrará su mensaje vacío
            }
        }
    }
}