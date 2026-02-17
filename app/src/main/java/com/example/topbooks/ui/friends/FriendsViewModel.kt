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
import java.util.Locale

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
    val friendsIds: Set<String> = emptySet(),
    val myFriends: List<SocialUser> = emptyList(),
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
        loadFriendsList()
        loadSocialData()
    }

    private fun loadFriendsList() {
        val currentUser = auth.currentUser ?: return

        db.collection("users").document(currentUser.uid).collection("friends")
            .addSnapshotListener { snapshot, _ ->
                val ids = snapshot?.documents?.map { it.id }?.toSet() ?: emptySet()
                _uiState.update { it.copy(friendsIds = ids) }
                fetchFriendsProfiles(ids.toList())

                if (_uiState.value.searchQuery.isNotEmpty()) {
                    updateSearchResultsWithFriends(ids)
                }
            }
    }

    private fun fetchFriendsProfiles(ids: List<String>) {
        if (ids.isEmpty()) {
            _uiState.update { it.copy(myFriends = emptyList()) }
            return
        }

        // Dividimos en bloques de 10 porque 'whereIn' tiene límite
        db.collection("users")
            .whereIn("__name__", ids.take(10))
            .get()
            .addOnSuccessListener { snapshot ->
                val profiles = snapshot.documents.mapNotNull { doc ->
                    SocialUser(
                        uid = doc.id,
                        displayName = doc.getString("displayName") ?: "",
                        // CORRECCIÓN: Usamos "photoURL" (tal como se guarda en ProfileViewModel)
                        photoUrl = doc.getString("photoURL") ?: "",
                        isFriend = true
                    )
                }
                _uiState.update { it.copy(myFriends = profiles) }
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
        val queryLower = query.lowercase(Locale.getDefault())

        db.collection("users")
            .whereGreaterThanOrEqualTo("displayNameLowercase", queryLower)
            .whereLessThanOrEqualTo("displayNameLowercase", queryLower + "\uf8ff")
            .limit(10)
            .get()
            .addOnSuccessListener { snapshot ->
                val friends = _uiState.value.friendsIds
                val results = snapshot.documents.mapNotNull { doc ->
                    if (doc.id == currentUser) return@mapNotNull null

                    SocialUser(
                        uid = doc.id,
                        displayName = doc.getString("displayName") ?: "",
                        // CORRECCIÓN: "photoURL"
                        photoUrl = doc.getString("photoURL") ?: "",
                        isFriend = friends.contains(doc.id)
                    )
                }
                _uiState.update { it.copy(searchResults = results, isSearching = false) }
            }
            .addOnFailureListener {
                _uiState.update { it.copy(isSearching = false) }
            }
    }

    fun toggleFriend(user: SocialUser) {
        val currentUser = auth.currentUser ?: return
        val friendRef = db.collection("users").document(currentUser.uid)
            .collection("friends").document(user.uid)

        if (user.isFriend) {
            friendRef.delete()
        } else {
            val friendData = mapOf(
                "displayName" to user.displayName,
                // CORRECCIÓN: Guardamos también como "photoURL" por consistencia
                "photoURL" to user.photoUrl,
                "timestamp" to System.currentTimeMillis()
            )
            friendRef.set(friendData)
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
                            // CORRECCIÓN: "photoURL"
                            photoUrl = doc.getString("photoURL") ?: ""
                        )
                    } else null
                } ?: emptyList()

                _uiState.update { it.copy(sameTastes = users, isLoading = false) }
            }
    }
}