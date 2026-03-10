package com.example.topbooks.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.repository.CommunityRepository
import com.example.topbooks.data.repository.CommunityRepositoryImpl
import com.example.topbooks.data.repository.UserRepository
import com.example.topbooks.data.repository.UserRepositoryImpl
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Conservamos las data classes originales para no romper tu UI

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
    private val userRepository: UserRepository = UserRepositoryImpl()
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

            // 1. Obtenemos mis amigos
            val friendsIds = communityRepository.getMyFriendsIds().getOrDefault(emptySet())
            _uiState.update { it.copy(friendsIds = friendsIds) }

            // 2. Obtenemos sugerencias (Limitado a 15 para no saturar)
            val currentUserId = userRepository.getCurrentUserId()
            val suggested = communityRepository.getSuggestedUsers(15).getOrDefault(emptyList())

            // Filtramos a los usuarios sugeridos: No me sugieras a mí mismo, ni a los que ya son mis amigos.
            val socialSuggested = suggested.filter {
                it.uid != currentUserId && !friendsIds.contains(it.uid)
            }.map { user ->
                SocialUser(
                    uid = user.uid,
                    displayName = user.displayName,
                    photoUrl = user.photoURL,
                    isFriend = false
                )
            }

            _uiState.update { it.copy(suggestedUsers = socialSuggested, isLoading = false) }
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
            // Reutilizamos la función del UserRepository que hicimos antes. ¡Magia!
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
}