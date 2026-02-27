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
data class SocialUser(
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val isFriend: Boolean = false,
    val tastes: List<String> = emptyList()
)

data class FriendsState(
    val searchQuery: String = "",
    val searchResults: List<SocialUser> = emptyList(),
    val friendsIds: Set<String> = emptySet(),
    val suggestedUsers: List<SocialUser> = emptyList(),
    val isLoading: Boolean = false,
    val isSearching: Boolean = false
)

class FriendsViewModel(
    // Inyectamos nuestros dos repositorios limpios
    private val communityRepository: CommunityRepository = CommunityRepositoryImpl(),
    private val userRepository: UserRepository = UserRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsState())
    val uiState: StateFlow<FriendsState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 1. Obtenemos mis amigos
            val friendsIds = communityRepository.getMyFriendsIds().getOrDefault(emptySet())
            _uiState.update { it.copy(friendsIds = friendsIds) }

            // 2. Obtenemos sugerencias
            val currentUserId = userRepository.getCurrentUserId()
            val suggested = communityRepository.getSuggestedUsers(15).getOrDefault(emptyList())

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

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel() // Cancelamos la búsqueda anterior si teclea rápido

        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(500) // Debounce: Esperamos medio segundo antes de ir a Firebase
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
                // Si falla el internet, deshacemos el cambio visual
                updateUserFriendStatus(user.uid, isCurrentlyFriend)
            }
        }
    }

    // Función auxiliar para actualizar listas sin repetir código
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