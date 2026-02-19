package com.example.topbooks.ui.friends

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.repository.BooksRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.UUID

data class SocialUser(
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val isFriend: Boolean = false,
    val tastes: List<String> = emptyList()
)

data class Interaction(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val userName: String = "",
    val userPhoto: String = "",
    val actionText: String = "",
    val bookTitle: String = "",
    val timestamp: Long = 0
)

data class FriendsState(
    val searchQuery: String = "",
    val searchResults: List<SocialUser> = emptyList(),
    val friendsIds: Set<String> = emptySet(),
    val myFriends: List<SocialUser> = emptyList(),
    val sameTastes: List<SocialUser> = emptyList(),
    val recentInteractions: List<Interaction> = emptyList(),
    val isSearching: Boolean = false,
    val isLoading: Boolean = false
)

class FriendsViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val booksRepository = BooksRepository()
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
                _uiState.update { currentState ->
                    val filteredSuggestions = currentState.sameTastes.filter { !ids.contains(it.uid) }
                    currentState.copy(friendsIds = ids, sameTastes = filteredSuggestions)
                }
                fetchFriendsProfiles(ids.toList())
                loadRecentActivity(ids.toList())
                if (_uiState.value.searchQuery.isNotEmpty()) updateSearchResultsWithFriends(ids)
            }
    }

    private fun loadRecentActivity(friendsIds: List<String>) {
        if (friendsIds.isEmpty()) {
            _uiState.update { it.copy(recentInteractions = emptyList()) }
            return
        }

        viewModelScope.launch {
            val activeFriends = friendsIds.take(10)
            val tasks = activeFriends.map { friendId ->
                async {
                    val friendInteractions = mutableListOf<Interaction>()
                    try {
                        val friendDoc = db.collection("users").document(friendId).get().await()
                        val fName = friendDoc.getString("displayName") ?: "Amigo"
                        val fPhoto = friendDoc.getString("photoURL") ?: ""

                        // 1. Reseñas
                        val reviews = db.collection("reviews").whereEqualTo("userId", friendId)
                            .orderBy("createAt", Query.Direction.DESCENDING).limit(1).get().await()
                        if (!reviews.isEmpty) {
                            val doc = reviews.documents.first()
                            val title = getBookTitle(doc.getString("bookId") ?: "")
                            friendInteractions.add(Interaction(userId = friendId, userName = fName, userPhoto = fPhoto,
                                actionText = "le dio ${doc.getLong("rating")} estrellas a", bookTitle = title, timestamp = doc.getDate("createAt")?.time ?: 0L))
                        }

                        // 2. Comentarios y Respuestas
                        val comments = db.collection("comments").orderBy("createAt", Query.Direction.DESCENDING).limit(10).get().await()
                        comments.documents.forEach { doc ->
                            val bookTitle = getBookTitle(doc.getString("bookId") ?: "")

                            // Si es autor
                            if (doc.getString("userId") == friendId) {
                                friendInteractions.add(Interaction(userId = friendId, userName = fName, userPhoto = fPhoto,
                                    actionText = "comentó en", bookTitle = bookTitle, timestamp = doc.getDate("createAt")?.time ?: 0L))
                            }

                            // Si es respuesta
                            val replies = doc.get("replies") as? List<Map<String, Any>>
                            replies?.forEach { reply ->
                                if (reply["userId"] == friendId) {
                                    val originalUser = doc.getString("userName") ?: "Usuario"
                                    friendInteractions.add(Interaction(userId = friendId, userName = fName, userPhoto = fPhoto,
                                        actionText = "respondió a $originalUser en", bookTitle = bookTitle, timestamp = reply["timestamp"] as? Long ?: 0L))
                                }
                            }
                        }
                    } catch (e: Exception) { Log.e("FriendsVM", "Error: ${e.message}") }
                    friendInteractions
                }
            }
            val results = tasks.awaitAll().flatten().sortedByDescending { it.timestamp }.take(10)
            _uiState.update { it.copy(recentInteractions = results) }
        }
    }

    private suspend fun getBookTitle(bookId: String): String {
        return try {
            val doc = db.collection("books").document(bookId).get().await()
            doc.getString("title") ?: booksRepository.getBookDetail(bookId).getOrNull()?.title ?: "Libro"
        } catch (e: Exception) { "Libro" }
    }

    private fun fetchFriendsProfiles(ids: List<String>) {
        if (ids.isEmpty()) return
        db.collection("users").whereIn("__name__", ids.take(10)).get().addOnSuccessListener { snapshot ->
            val profiles = snapshot.documents.mapNotNull { doc ->
                SocialUser(uid = doc.id, displayName = doc.getString("displayName") ?: "", photoUrl = doc.getString("photoURL") ?: "", isFriend = true)
            }
            _uiState.update { it.copy(myFriends = profiles) }
        }
    }

    private fun updateSearchResultsWithFriends(friendsIds: Set<String>) {
        val updatedResults = _uiState.value.searchResults.map { user -> user.copy(isFriend = friendsIds.contains(user.uid)) }
        _uiState.update { it.copy(searchResults = updatedResults) }
    }

    fun onSearchQueryChange(newQuery: String) {
        _uiState.update { it.copy(searchQuery = newQuery) }
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch { delay(500); performSearch(newQuery) }
    }

    private fun performSearch(query: String) {
        _uiState.update { it.copy(isSearching = true) }
        val currentUser = auth.currentUser?.uid
        val queryLower = query.lowercase(Locale.getDefault())
        db.collection("users").whereGreaterThanOrEqualTo("displayNameLowercase", queryLower)
            .whereLessThanOrEqualTo("displayNameLowercase", queryLower + "\uf8ff").limit(10).get()
            .addOnSuccessListener { snapshot ->
                val friends = _uiState.value.friendsIds
                val results = snapshot.documents.mapNotNull { doc ->
                    if (doc.id == currentUser) return@mapNotNull null
                    SocialUser(uid = doc.id, displayName = doc.getString("displayName") ?: "", photoUrl = doc.getString("photoURL") ?: "", isFriend = friends.contains(doc.id))
                }
                _uiState.update { it.copy(searchResults = results, isSearching = false) }
            }
            .addOnFailureListener { _uiState.update { it.copy(isSearching = false) } }
    }

    fun toggleFriend(user: SocialUser) {
        val currentUser = auth.currentUser ?: return
        val friendRef = db.collection("users").document(currentUser.uid).collection("friends").document(user.uid)
        if (user.isFriend) friendRef.delete()
        else friendRef.set(mapOf("displayName" to user.displayName, "photoURL" to user.photoUrl, "timestamp" to System.currentTimeMillis()))
    }

    private fun loadSocialData() {
        val currentUser = auth.currentUser ?: return
        _uiState.update { it.copy(isLoading = true) }
        db.collection("users").limit(10).addSnapshotListener { snapshot, _ ->
            val friends = _uiState.value.friendsIds
            val users = snapshot?.documents?.mapNotNull { doc ->
                if (doc.id != currentUser.uid && !friends.contains(doc.id)) SocialUser(uid = doc.id, displayName = doc.getString("displayName") ?: "Usuario", photoUrl = doc.getString("photoURL") ?: "")
                else null
            } ?: emptyList()
            _uiState.update { it.copy(sameTastes = users, isLoading = false) }
        }
    }
}