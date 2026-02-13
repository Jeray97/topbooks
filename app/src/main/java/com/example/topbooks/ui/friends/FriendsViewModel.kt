package com.example.topbooks.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SocialUser(
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val isFriend: Boolean = false,
    val tastes: List<String> = emptyList()
)

data class Interaction(
    val userId: String = "",
    val userPhoto: String = "",
    val type: String = "",
    val timestamp: Long = 0
)

data class FriendsState(
    val searchQuery: String = "",
    val searchResults: List<SocialUser> = emptyList(),
    val friendsIds: Set<String> = emptySet(), // IDs de amigos actuales
    val friendsOfFriends: List<SocialUser> = emptyList(),
    val sameTastes: List<SocialUser> = emptyList(),
    val recentInteractions: List<Interaction> = emptyList(),
    val isSearching: Boolean = false,
    val isLoading: Boolean = false
)

class FriendsViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var searchJob: Job? = null

    private val _uiState = MutableStateFlow(FriendsState())
    val uiState: StateFlow<FriendsState> = _uiState

    init {
        // Primero cargamos los amigos para saber a quién marcar con el "tick"
        loadFriendsList()
        loadSocialData()
    }

    private fun loadFriendsList() {
        val currentUser = auth.currentUser ?: return

        // Escuchamos la colección de amigos del usuario actual
        db.collection("users").document(currentUser.uid).collection("friends")
            .addSnapshotListener { snapshot, _ ->
                val ids = snapshot?.documents?.map { it.id }?.toSet() ?: emptySet()
                _uiState.update { it.copy(friendsIds = ids) }

                // Si hay una búsqueda activa, actualizamos los "ticks" en tiempo real
                if (_uiState.value.searchQuery.isNotEmpty()) {
                    updateSearchResultsWithFriends(ids)
                }
            }
    }

    private fun updateSearchResultsWithFriends(friendsIds: Set<String>) {
        val updatedResults = _uiState.value.searchResults.map { user ->
            user.copy(isFriend = friendsIds.contains(user.uid))
        }
        _uiState.update { it.copy(searchResults = updatedResults) }
    }

    fun onSearchQueryChange(newQuery: String) {
        _uiState.update { it.copy(searchQuery = newQuery) }
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(500)
            performSearch(newQuery)
        }
    }

    private fun performSearch(query: String) {
        _uiState.update { it.copy(isSearching = true) }
        val currentUser = auth.currentUser?.uid

        db.collection("users")
            .whereGreaterThanOrEqualTo("displayName", query)
            .whereLessThanOrEqualTo("displayName", query + "\uf8ff")
            .limit(10)
            .get()
            .addOnSuccessListener { snapshot ->
                val friends = _uiState.value.friendsIds
                val results = snapshot.documents.mapNotNull { doc ->
                    if (doc.id == currentUser) return@mapNotNull null // No buscarse a uno mismo

                    SocialUser(
                        uid = doc.id,
                        displayName = doc.getString("displayName") ?: "",
                        photoUrl = doc.getString("photoUrl") ?: "",
                        isFriend = friends.contains(doc.id)
                    )
                }
                _uiState.update { it.copy(searchResults = results, isSearching = false) }
            }
            .addOnFailureListener {
                _uiState.update { it.copy(isSearching = false) }
            }
    }

    fun addFriend(targetUser: SocialUser) {
        val currentUser = auth.currentUser ?: return

        // 1. Añadimos a nuestra lista de amigos
        val friendData = mapOf(
            "displayName" to targetUser.displayName,
            "photoUrl" to targetUser.photoUrl,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("users").document(currentUser.uid)
            .collection("friends").document(targetUser.uid)
            .set(friendData)
            .addOnSuccessListener {
            }
    }

    private fun loadSocialData() {
        val currentUser = auth.currentUser ?: return
        _uiState.update { it.copy(isLoading = true) }

        db.collection("users")
            .limit(10)
            .addSnapshotListener { snapshot, _ ->
                val users = snapshot?.documents?.mapNotNull { doc ->
                    if (doc.id != currentUser.uid) {
                        SocialUser(
                            uid = doc.id,
                            displayName = doc.getString("displayName") ?: "Usuario",
                            photoUrl = doc.getString("photoUrl") ?: ""
                        )
                    } else null
                } ?: emptyList()

                _uiState.update { it.copy(sameTastes = users, isLoading = false) }
            }
    }
}