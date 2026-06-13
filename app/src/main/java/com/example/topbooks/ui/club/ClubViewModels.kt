package com.example.topbooks.ui.club

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Club
import com.example.topbooks.data.model.ClubFrequency
import com.example.topbooks.data.model.Discussion
import com.example.topbooks.data.model.DiscussionMessage
import com.example.topbooks.data.repository.ClubRepository
import com.example.topbooks.data.repository.ClubRepositoryImpl
import com.example.topbooks.data.repository.UserRepository
import com.example.topbooks.data.repository.UserRepositoryImpl
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ClubListTab { MY_CLUBS, EXPLORE }

data class ClubListState(
    val isLoading: Boolean = true,
    val activeTab: ClubListTab = ClubListTab.MY_CLUBS,
    val myClubs: List<Club> = emptyList(),
    val publicClubs: List<Club> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<Club> = emptyList(),
    val errorMessage: String? = null
)

data class ClubDetailState(
    val isLoading: Boolean = true,
    val club: Club? = null,
    val discussions: List<Discussion> = emptyList(),
    val isMember: Boolean = false,
    val isCreator: Boolean = false,
    val isJoining: Boolean = false,
    val errorMessage: String? = null
)

data class DiscussionState(
    val isLoading: Boolean = true,
    val discussion: Discussion? = null,
    val isSending: Boolean = false,
    val errorMessage: String? = null
)

class ClubListViewModel(
    private val clubRepository: ClubRepository = ClubRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClubListState())
    val uiState: StateFlow<ClubListState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val myClubsResult = clubRepository.getMyClubs()
                val publicClubsResult = clubRepository.getPublicClubs(30)

                val myClubs = myClubsResult.getOrNull()
                val publicClubs = publicClubsResult.getOrNull()

                val error = myClubsResult.exceptionOrNull()?.message
                    ?: publicClubsResult.exceptionOrNull()?.message

                if (myClubs == null || publicClubs == null) {
                    Log.e("ClubListVM", "Error cargando clubs: $error")
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error ?: "Error desconocido")
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        myClubs = myClubs,
                        publicClubs = publicClubs
                    )
                }
            } catch (e: Exception) {
                Log.e("ClubListVM", "Error: ${e.message}")
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun selectTab(tab: ClubListTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun searchClubs(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.length < 2) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            val results = clubRepository.searchClubs(query).getOrDefault(emptyList())
            _uiState.update { it.copy(searchResults = results) }
        }
    }
}

class ClubDetailViewModel(
    private val clubRepository: ClubRepository = ClubRepositoryImpl(),
    private val userRepository: UserRepository = UserRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClubDetailState())
    val uiState: StateFlow<ClubDetailState> = _uiState.asStateFlow()

    private var myUid: String = ""

    init {
        myUid = userRepository.getCurrentUserId() ?: ""
    }

    fun loadClub(clubId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val club = clubRepository.getClubById(clubId).getOrNull()
                if (club == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Club no encontrado") }
                    return@launch
                }

                val creator = userRepository.getUserProfile(club.createdBy).getOrNull()
                val enrichedClub = club.copy(
                    creatorName = creator?.displayName ?: club.creatorName,
                    creatorPhotoUrl = creator?.photoURL ?: club.creatorPhotoUrl
                )

                val discussions = clubRepository.getDiscussions(clubId).getOrDefault(emptyList())
                val enrichedDiscussions = discussions.map { discussion ->
                    viewModelScope.async {
                        val user = userRepository.getUserProfile(discussion.createdBy).getOrNull()
                        discussion.copy(
                            creatorName = user?.displayName ?: discussion.creatorName,
                            creatorPhotoUrl = user?.photoURL ?: discussion.creatorPhotoUrl
                        )
                    }
                }.awaitAll()

                val isMember = myUid in club.memberIds
                val isCreator = club.createdBy == myUid

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        club = enrichedClub,
                        discussions = enrichedDiscussions,
                        isMember = isMember,
                        isCreator = isCreator
                    )
                }
            } catch (e: Exception) {
                Log.e("ClubDetailVM", "Error: ${e.message}")
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun toggleMembership() {
        val club = _uiState.value.club ?: return
        val wasMember = _uiState.value.isMember

        _uiState.update {
            it.copy(
                isJoining = true,
                isMember = !wasMember,
                club = it.club?.copy(
                    memberCount = if (wasMember) it.club.memberCount - 1 else it.club.memberCount + 1
                )
            )
        }

        viewModelScope.launch {
            val result = if (wasMember) {
                clubRepository.leaveClub(club.id)
            } else {
                clubRepository.joinClub(club.id)
            }

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isJoining = false) }
                },
                onFailure = { error ->
                    Log.e("ClubDetailVM", "Error toggle membership: ${error.message}")
                    _uiState.update {
                        it.copy(
                            isJoining = false,
                            isMember = wasMember,
                            club = it.club?.copy(
                                memberCount = if (wasMember) it.club.memberCount + 1 else it.club.memberCount - 1
                            )
                        )
                    }
                }
            )
        }
    }

    fun createDiscussion(title: String, chapter: String, isSpoiler: Boolean, onSuccess: () -> Unit) {
        val club = _uiState.value.club ?: return
        viewModelScope.launch {
            val discussion = Discussion(
                clubId = club.id,
                title = title,
                chapter = chapter,
                isSpoiler = isSpoiler
            )
            clubRepository.createDiscussion(discussion).fold(
                onSuccess = {
                    loadClub(club.id)
                    sendDiscussionNotification(club.id, club.name, title)
                    onSuccess()
                },
                onFailure = { error ->
                    Log.e("ClubDetailVM", "Error creating discussion: ${error.message}")
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
            )
        }
    }

    private fun sendDiscussionNotification(clubId: String, clubName: String, discussionTitle: String) {
        viewModelScope.launch {
            try {
                val user = userRepository.getUserProfile(myUid).getOrNull()
                val data = hashMapOf(
                    "clubId" to clubId,
                    "clubName" to clubName,
                    "discussionTitle" to discussionTitle,
                    "creatorId" to myUid,
                    "creatorName" to (user?.displayName ?: "Un miembro")
                )
                com.google.firebase.functions.FirebaseFunctions.getInstance()
                    .getHttpsCallable("notificarNuevaDiscusion")
                    .call(data)
                    .addOnSuccessListener {
                        Log.d("ClubDetailVM", "Notificación de discusión enviada")
                    }
                    .addOnFailureListener { e ->
                        Log.e("ClubDetailVM", "Error enviando notificación: ${e.message}")
                    }
            } catch (e: Exception) {
                Log.e("ClubDetailVM", "Error: ${e.message}")
            }
        }
    }

    fun updateProgress(progress: Int) {
        val club = _uiState.value.club ?: return
        viewModelScope.launch {
            clubRepository.updateMemberProgress(club.id, progress)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

class DiscussionViewModel(
    private val clubRepository: ClubRepository = ClubRepositoryImpl(),
    private val userRepository: UserRepository = UserRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscussionState())
    val uiState: StateFlow<DiscussionState> = _uiState.asStateFlow()

    private var myUid: String = ""
    private var currentClubId: String = ""
    private var currentDiscussionId: String = ""

    init {
        myUid = userRepository.getCurrentUserId() ?: ""
    }

    fun loadDiscussion(clubId: String, discussionId: String) {
        currentClubId = clubId
        currentDiscussionId = discussionId
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val discussion = clubRepository.getDiscussionById(clubId, discussionId).getOrNull()
                if (discussion == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Discusión no encontrada") }
                    return@launch
                }

                val enrichedMessages = discussion.messages.map { message ->
                    viewModelScope.async {
                        val user = userRepository.getUserProfile(message.userId).getOrNull()
                        message.copy(
                            userName = user?.displayName ?: message.userName,
                            userPhotoUrl = user?.photoURL ?: message.userPhotoUrl
                        )
                    }
                }.awaitAll()

                val creator = userRepository.getUserProfile(discussion.createdBy).getOrNull()
                val enrichedDiscussion = discussion.copy(
                    creatorName = creator?.displayName ?: discussion.creatorName,
                    creatorPhotoUrl = creator?.photoURL ?: discussion.creatorPhotoUrl,
                    messages = enrichedMessages
                )

                _uiState.update {
                    it.copy(isLoading = false, discussion = enrichedDiscussion)
                }
            } catch (e: Exception) {
                Log.e("DiscussionVM", "Error: ${e.message}")
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        _uiState.update { it.copy(isSending = true) }

        viewModelScope.launch {
            val me = userRepository.getUserProfile(myUid).getOrNull()
            val message = DiscussionMessage(
                userId = myUid,
                text = text.trim(),
                userName = me?.displayName ?: "Usuario",
                userPhotoUrl = me?.photoURL ?: "capibara_1"
            )

            clubRepository.addMessage(currentClubId, currentDiscussionId, message).fold(
                onSuccess = {
                    val newMessage = message.copy(
                        id = System.currentTimeMillis().toString(),
                        createdAt = java.util.Date()
                    )
                    _uiState.update { state ->
                        state.copy(
                            isSending = false,
                            discussion = state.discussion?.copy(
                                messages = state.discussion.messages + newMessage,
                                messageCount = state.discussion.messageCount + 1
                            )
                        )
                    }
                },
                onFailure = { error ->
                    Log.e("DiscussionVM", "Error sending message: ${error.message}")
                    _uiState.update { it.copy(isSending = false, errorMessage = error.message) }
                }
            )
        }
    }
}
