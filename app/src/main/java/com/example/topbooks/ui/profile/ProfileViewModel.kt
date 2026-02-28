package com.example.topbooks.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.User
import com.example.topbooks.data.repository.UserRepository
import com.example.topbooks.data.repository.UserRepositoryImpl
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User = User(),
    val favoriteCovers: List<String> = emptyList(),
    val favoriteIds: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isMe: Boolean = false,
    val isFriend: Boolean = false,
    val isEmailVerified: Boolean = true
)

class ProfileViewModel(
    // Inyectamos el Repositorio de Usuario
    private val repository: UserRepository = UserRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile(targetUserId: String? = null) {
        val myUid = repository.getCurrentUserId() ?: return
        val finalUserId = if (targetUserId.isNullOrEmpty()) myUid else targetUserId
        val isMe = (finalUserId == myUid)

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) } // Iniciamos estado de carga
            try {
                // 🚀 Consultas en paralelo usando nuestro repositorio limpio
                val userDeferred = async { repository.getUserProfile(finalUserId) }
                val favCoversDeferred = async { repository.getFavoriteCovers(finalUserId, 5) }
                val favIdsDeferred = async { repository.getFavoriteIds(finalUserId) }
                val isFriendDeferred = async {
                    if (!isMe) repository.isFriend(myUid, finalUserId).getOrDefault(false) else false
                }

                val user = userDeferred.await().getOrNull() ?: User()
                val covers = favCoversDeferred.await().getOrDefault(emptyList())
                val ids = favIdsDeferred.await().getOrDefault(emptyList())
                val isFriend = isFriendDeferred.await()

                _uiState.update {
                    it.copy(
                        user = user,
                        favoriteCovers = covers,
                        favoriteIds = ids,
                        isFriend = isFriend,
                        isMe = isMe, // <--- EL ERROR ESTABA AQUÍ: Faltaba pasar la variable de si es el propio perfil
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("ProfileVM", "Error cargando perfil: ${e.message}")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun toggleFriend(targetUserId: String, targetUserName: String, targetPhotoUrl: String) {
        val myUid = repository.getCurrentUserId() ?: return
        val currentState = _uiState.value.isFriend
        val newState = !currentState

        // UI Optimista: Cambiamos el estado al instante para que la app parezca muy rápida
        _uiState.update { it.copy(isFriend = newState) }

        viewModelScope.launch {
            repository.toggleFriendship(myUid, targetUserId, targetUserName, targetPhotoUrl, newState)
                .onFailure { error ->
                    Log.e("ProfileVM", "Error al cambiar amistad: ${error.message}")
                    // Si falla la red, revertimos el botón a su estado original
                    _uiState.update { it.copy(isFriend = currentState) }
                }
        }
    }

    fun updateAvatar(newAvatar: String) {
        val uid = repository.getCurrentUserId() ?: return
        val currentUser = _uiState.value.user

        // UI Optimista: Actualizamos la vista inmediatamente
        _uiState.update { it.copy(user = currentUser.copy(photoURL = newAvatar)) }

        viewModelScope.launch {
            repository.updateAvatar(uid, newAvatar).onFailure { error ->
                Log.e("ProfileVM", "Error al actualizar avatar: ${error.message}")
                // Si Firebase falla, revertimos al avatar anterior
                _uiState.update { it.copy(user = currentUser) }
            }
        }
    }

    fun updateProfileData(newName: String, newBio: String) {
        val uid = repository.getCurrentUserId() ?: return
        val currentUser = _uiState.value.user

        // UI Optimista: Actualizamos la vista inmediatamente
        _uiState.update { it.copy(user = currentUser.copy(displayName = newName, bio = newBio)) }

        viewModelScope.launch {
            repository.updateProfileData(uid, newName, newBio).onFailure { error ->
                Log.e("ProfileVM", "Error al actualizar perfil: ${error.message}")
                // Si Firebase falla, revertimos a los datos anteriores
                _uiState.update { it.copy(user = currentUser) }
            }
        }
    }
}