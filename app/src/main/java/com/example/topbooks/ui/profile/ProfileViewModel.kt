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

/**
 * Representa el estado completo de la interfaz de perfil.
 * * @property user Datos básicos del perfil (nombre, foto, bio).
 * @property favoriteCovers Lista de URLs de las portadas de los libros favoritos.
 * @property favoriteIds Lista de IDs de los libros favoritos (para lógica de UI).
 * @property isLoading Controla la visibilidad del spinner de carga.
 * @property isMe Indica si el perfil visualizado es el del propio usuario autenticado.
 * @property isFriend Indica si existe una relación de amistad con el usuario visualizado.
 * @property isEmailVerified Estado de seguridad de la cuenta.
 */
data class ProfileUiState(
    val user: User = User(),
    val favoriteCovers: List<String> = emptyList(),
    val favoriteIds: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isMe: Boolean = false,
    val isFriend: Boolean = false,
    val isEmailVerified: Boolean = true
)

/**
 * ViewModel que gestiona la lógica de la pantalla de Perfil.
 * * Se encarga de la obtención de datos del perfil (propio o ajeno) y de la actualización
 * de la información personal utilizando técnicas de persistencia en Firebase.
 */
class ProfileViewModel(
    // Inyección de dependencia del repositorio de usuarios
    private val repository: UserRepository = UserRepositoryImpl()
) : ViewModel() {

    // Estado reactivo interno y su exposición pública inmutable
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    /**
     * Carga la información completa del perfil solicitado.
     * * TÉCNICA AVANZADA: Utiliza el patrón de "Fetch paralelo". Al usar bloques [async],
     * se inician todas las peticiones a Firestore simultáneamente en lugar de secuencialmente,
     * optimizando el tiempo de respuesta.
     *
     * @param targetUserId ID del usuario a consultar. Si es nulo o vacío, carga el perfil propio.
     */
    fun loadProfile(targetUserId: String? = null) {
        val myUid = repository.getCurrentUserId() ?: return
        val finalUserId = if (targetUserId.isNullOrEmpty()) myUid else targetUserId
        val isMe = (finalUserId == myUid)

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Lanzamiento de consultas en paralelo
                val userDeferred = async { repository.getUserProfile(finalUserId) }
                val favCoversDeferred = async { repository.getFavoriteCovers(finalUserId, 5) }
                val favIdsDeferred = async { repository.getFavoriteIds(finalUserId) }
                val isFriendDeferred = async {
                    // Solo comprobamos amistad si no es nuestro propio perfil
                    if (!isMe) repository.isFriend(myUid, finalUserId).getOrDefault(false) else false
                }

                // Esperamos a que todas las peticiones finalicen
                val user = userDeferred.await().getOrNull() ?: User()
                val covers = favCoversDeferred.await().getOrDefault(emptyList())
                val ids = favIdsDeferred.await().getOrDefault(emptyList())
                val isFriend = isFriendDeferred.await()

                // Actualización masiva del estado de la UI
                _uiState.update {
                    it.copy(
                        user = user,
                        favoriteCovers = covers,
                        favoriteIds = ids,
                        isFriend = isFriend,
                        isMe = isMe,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("ProfileVM", "Error cargando perfil: ${e.message}")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Gestiona la relación de amistad (Seguir/Dejar de seguir).
     * * TÉCNICA: UI Optimista. Cambia el estado visual inmediatamente para dar sensación
     * de fluidez. Si la operación en el servidor falla, revierte el estado.
     */
    fun toggleFriend(targetUserId: String, targetUserName: String, targetPhotoUrl: String) {
        val myUid = repository.getCurrentUserId() ?: return
        val currentState = _uiState.value.isFriend
        val newState = !currentState

        // Actualización instantánea de la UI
        _uiState.update { it.copy(isFriend = newState) }

        viewModelScope.launch {
            repository.toggleFriendship(myUid, targetUserId, targetUserName, targetPhotoUrl, newState)
                .onFailure { error ->
                    Log.e("ProfileVM", "Error al cambiar amistad: ${error.message}")
                    // Rollback: Revertimos al estado anterior en caso de fallo
                    _uiState.update { it.copy(isFriend = currentState) }
                }
        }
    }

    /**
     * Actualiza el avatar del usuario actual.
     * Implementa lógica optimista con reversión en caso de error.
     */
    fun updateAvatar(newAvatar: String) {
        val uid = repository.getCurrentUserId() ?: return
        val currentUser = _uiState.value.user

        // Actualización visual inmediata
        _uiState.update { it.copy(user = currentUser.copy(photoURL = newAvatar)) }

        viewModelScope.launch {
            repository.updateAvatar(uid, newAvatar).onFailure { error ->
                Log.e("ProfileVM", "Error al actualizar avatar: ${error.message}")
                // Rollback si falla la persistencia en Firebase
                _uiState.update { it.copy(user = currentUser) }
            }
        }
    }

    /**
     * Actualiza los datos informativos del perfil (Nombre y Biografía).
     */
    fun updateProfileData(newName: String, newBio: String) {
        val uid = repository.getCurrentUserId() ?: return
        val currentUser = _uiState.value.user

        // Actualización visual inmediata para mayor fluidez
        _uiState.update { it.copy(user = currentUser.copy(displayName = newName, bio = newBio)) }

        viewModelScope.launch {
            repository.updateProfileData(uid, newName, newBio).onFailure { error ->
                Log.e("ProfileVM", "Error al actualizar perfil: ${error.message}")
                // Reversión de datos ante fallo de red
                _uiState.update { it.copy(user = currentUser) }
            }
        }
    }
}