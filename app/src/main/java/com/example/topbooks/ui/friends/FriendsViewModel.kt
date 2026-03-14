package com.example.topbooks.ui.friends

import android.util.Log
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
 * Agrupa to-do el estado de la pantalla [FriendsScreen] en una sola clase de datos reactiva.
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
    private val communityRepository: CommunityRepository = CommunityRepositoryImpl(),
    private val userRepository: UserRepository = UserRepositoryImpl(),
    private val feedRepository: SocialFeedRepository = SocialFeedRepositoryImpl(),
    private val booksRepository: BooksRepository = BooksRepository()
) : ViewModel() {

    // --- ESTADO REACTIVO ---
    private val _uiState = MutableStateFlow(FriendsState())
    val uiState: StateFlow<FriendsState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val currentUserId = userRepository.getCurrentUserId() ?: return@launch

            val friendsList = userRepository.getFriendsList(currentUserId).getOrDefault(emptyList())

            val socialFriends = friendsList.map {
                SocialUser(uid = it.uid, displayName = it.name, photoUrl = it.photo, isFriend = true)
            }
            val friendsIds = socialFriends.map { it.uid }.toSet()

            val suggested = communityRepository.getSuggestedUsers(15).getOrDefault(emptyList())

            val socialSuggested = suggested.filter {
                it.uid != currentUserId && !friendsIds.contains(it.uid)
            }.map { user ->
                SocialUser(uid = user.uid, displayName = user.displayName, photoUrl = user.photoURL, isFriend = false)
            }

            _uiState.update {
                it.copy(
                    myFriends = socialFriends,
                    friendsIds = friendsIds,
                    suggestedUsers = socialSuggested,
                    isLoading = false
                )
            }

            refreshRecentActivity()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }

        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(500)

            _uiState.update { it.copy(isSearching = true) }

            val users = communityRepository.searchUsers(query).getOrDefault(emptyList())
            val friendsIds = _uiState.value.friendsIds

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

    fun toggleFriend(user: SocialUser) {
        val myUid = userRepository.getCurrentUserId() ?: return
        val isCurrentlyFriend = user.isFriend
        val newFriendStatus = !isCurrentlyFriend

        updateUserFriendStatus(user.uid, newFriendStatus)

        viewModelScope.launch {
            userRepository.toggleFriendship(
                myUid = myUid,
                targetUid = user.uid,
                targetName = user.displayName,
                targetPhoto = user.photoUrl,
                isAdding = newFriendStatus
            ).onFailure {
                updateUserFriendStatus(user.uid, isCurrentlyFriend)
            }
        }
    }

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
     * Obtiene un resumen rápido de las últimas interacciones de los amigos.
     * Incluye trazas de depuración (Logs) para monitorizar el rendimiento y la integridad de los datos.
     */
    fun refreshRecentActivity() {
        viewModelScope.launch {
            try {
                val friendsIds = _uiState.value.friendsIds.toList()
                Log.d("FriendsVM_Debug", "1. Iniciando carga de actividad para ${friendsIds.size} amigos: $friendsIds")

                if (friendsIds.isEmpty()) return@launch

                // 1. EXTRAER: Buscamos TODA la actividad en paralelo
                val activitiesDeferred = coroutineScope {
                    friendsIds.map { friendId ->
                        async {
                            val user = userRepository.getUserProfile(friendId).getOrNull() ?: return@async emptyList()
                            val friendName = user.displayName.ifEmpty { "Usuario" }
                            val friendPhoto = user.photoURL.ifEmpty { "capibara_1" }

                            // Descargamos las tres fuentes de interacciones
                            val reviews = feedRepository.getUserReviews(friendId).getOrDefault(emptyList())
                            val comments = feedRepository.getUserComments(friendId).getOrDefault(emptyList())
                            val favorites = feedRepository.getUserFavorites(friendId).getOrDefault(emptyList())

                            Log.d("FriendsVM_Debug", "2. Amigo [$friendName] -> Reseñas: ${reviews.size} | Comentarios: ${comments.size} | Favoritos: ${favorites.size}")

                            val userInteractions = mutableListOf<Pair<Long, Interaction>>()

                            // Procesamos Reseñas
                            reviews.forEach { r ->
                                val time = r.createAt?.time ?: 0L
                                if (time == 0L) Log.w("FriendsVM_Debug", "ALERTA FECHA NULA: Reseña de $friendName en el libro ${r.bookId}")
                                userInteractions.add(Pair(time, Interaction(friendPhoto, friendName, "ha valorado", r.bookId)))
                            }

                            // Procesamos Comentarios
                            comments.forEach { c ->
                                val time = c.createAt?.time ?: 0L
                                if (time == 0L) Log.w("FriendsVM_Debug", "ALERTA FECHA NULA: Comentario de $friendName en el libro ${c.bookId}")
                                userInteractions.add(Pair(time, Interaction(friendPhoto, friendName, "ha comentado en", c.bookId)))
                            }

                            // Procesamos Favoritos
                            favorites.forEach { fav ->
                                val bookId = fav["bookId"] as? String ?: return@forEach
                                val time = fav["addedAt"] as? Long ?: 0L
                                if (time == 0L) Log.w("FriendsVM_Debug", "ALERTA FECHA NULA: Favorito de $friendName en el libro $bookId")
                                userInteractions.add(Pair(time, Interaction(friendPhoto, friendName, "ha añadido a favoritos", bookId)))
                            }

                            userInteractions
                        }
                    }
                }

                // 2. ORDENAR
                val allActivities = activitiesDeferred.awaitAll().flatten()
                Log.d("FriendsVM_Debug", "3. Total de interacciones en bruto recolectadas: ${allActivities.size}")

                val top3Activities = allActivities
                    .sortedByDescending { it.first } // Ordenamos de más nuevo a más antiguo
                    .take(3)

                Log.d("FriendsVM_Debug", "4. Top 3 seleccionadas: ${top3Activities.map { it.second.actionText }}")

                // 3. HIDRATAR (Buscamos los títulos reales solo para las 3 ganadoras)
                val finalInteractions = top3Activities.map { (_, interaction) ->
                    Log.d("FriendsVM_Debug", "5. Llamando a API (BooksRepository) para el libro ID: ${interaction.bookTitle}")
                    val book = booksRepository.getBookDetail(interaction.bookTitle).getOrNull()
                    interaction.copy(bookTitle = book?.title ?: "un libro")
                }

                _uiState.update { it.copy(recentInteractions = finalInteractions) }
                Log.d("FriendsVM_Debug", "6. ÉXITO: UI de Dashboard actualizada con las interacciones recientes.")

            } catch (e: Exception) {
                Log.e("FriendsVM_Debug", "ERROR FATAL calculando interacciones recientes: ${e.message}", e)
            }
        }
    }
}