package com.example.topbooks.ui.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.topbooks.R
import com.example.topbooks.data.preferences.SettingsManager
import com.example.topbooks.data.repository.AuthRepository
import com.example.topbooks.data.repository.AuthRepositoryImpl
import com.example.topbooks.data.repository.UserRepository
import com.example.topbooks.data.repository.UserRepositoryImpl
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel encargado de gestionar la lógica de la pantalla de Configuración.
 */
class ConfigViewModel(
    private val settingsManager: SettingsManager,
    private val authRepository: AuthRepository = AuthRepositoryImpl(),
    private val userRepository: UserRepository = UserRepositoryImpl() // Añadido para gestionar los géneros
) : ViewModel() {

    // --- ESTADOS LOCALES ---

    private val _isEmailVerified = MutableStateFlow(true)
    val isEmailVerified: StateFlow<Boolean> = _isEmailVerified.asStateFlow()

    private val _isDeletingAccount = MutableStateFlow(false)
    val isDeletingAccount: StateFlow<Boolean> = _isDeletingAccount.asStateFlow()

    // Estado para los géneros favoritos actuales del usuario
    private val _favoriteGenres = MutableStateFlow<List<String>>(emptyList())
    val favoriteGenres: StateFlow<List<String>> = _favoriteGenres.asStateFlow()

    private val _isUpdatingGenres = MutableStateFlow(false)
    val isUpdatingGenres: StateFlow<Boolean> = _isUpdatingGenres.asStateFlow()

    // --- ESTADOS DE PREFERENCIAS (DATASTORE) ---

    val darkModeEnabled: StateFlow<Boolean> = settingsManager.darkModeFlow
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = false)

    val notificationsEnabled: StateFlow<Boolean> = settingsManager.notificationsFlow
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = true)

    val publicJournalDefaultEnabled: StateFlow<Boolean> = settingsManager.publicJournalDefaultFlow
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = false)

    // Nuevo flujo para el idioma (Asume que añadirás 'languageFlow' a tu SettingsManager)
    val currentLanguage: StateFlow<String> = settingsManager.languageFlow
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = "es")

    init {
        refreshVerificationStatus()
        loadFavoriteGenres()
    }

    fun refreshVerificationStatus() {
        viewModelScope.launch {
            authRepository.reloadUser()
            _isEmailVerified.value = authRepository.isEmailVerified()
        }
    }

    /**
     * Carga los géneros favoritos actuales del perfil del usuario en Firebase.
     */
    private fun loadFavoriteGenres() {
        val uid = userRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            val user = userRepository.getUserProfile(uid).getOrNull()
            if (user != null) {
                _favoriteGenres.value = user.favoriteGenres
            }
        }
    }

    // --- FUNCIONES DE ACTUALIZACIÓN ---

    fun toggleDarkMode(enabled: Boolean) = viewModelScope.launch { settingsManager.saveDarkMode(enabled) }
    fun toggleNotifications(enabled: Boolean) = viewModelScope.launch { settingsManager.saveNotifications(enabled) }
    fun togglePublicJournalDefault(enabled: Boolean) = viewModelScope.launch { settingsManager.savePublicJournalDefault(enabled) }

    /** Guarda el idioma seleccionado en las preferencias locales. */
    fun updateLanguage(langCode: String) = viewModelScope.launch { settingsManager.saveLanguage(langCode) }

    /**
     * Actualiza la lista de géneros favoritos en Firebase.
     * Requiere que añadas la función 'updateFavoriteGenres(uid, genres)' en tu UserRepository.
     */
    fun saveFavoriteGenres(newGenres: List<String>, onResult: (String) -> Unit) {
        val uid = userRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            _isUpdatingGenres.value = true

            // Asume que UserRepository tiene esta función implementada con Firestore
            userRepository.updateFavoriteGenres(uid, newGenres).onSuccess {
                _favoriteGenres.value = newGenres
                onResult("Categorías actualizadas correctamente.")
            }.onFailure {
                onResult("Error al actualizar las categorías.")
            }
            _isUpdatingGenres.value = false
        }
    }

    // --- RESTO DE FUNCIONES EXISTENTES ---

    fun resendVerificationEmail(onResult: (String) -> Unit) {
        authRepository.resendVerificationEmail { result ->
            if (result.isSuccess) onResult("Correo de verificación reenviado.")
            else onResult("Error al enviar el correo. Inténtalo más tarde.")
        }
    }

    fun sendPasswordReset(onResult: (String) -> Unit) {
        val email = FirebaseAuth.getInstance().currentUser?.email
        if (email.isNullOrEmpty()) {
            onResult("No se pudo obtener el correo del usuario.")
            return
        }
        viewModelScope.launch {
            authRepository.sendPasswordResetEmail(email).onSuccess {
                onResult("Se ha enviado un correo para restablecer tu contraseña.")
            }.onFailure {
                onResult("Error al solicitar el cambio de contraseña.")
            }
        }
    }

    fun signOut() {
        authRepository.logout()
    }

    fun isGoogleUser(): Boolean {
        val user = FirebaseAuth.getInstance().currentUser
        return user?.providerData?.any { it.providerId == "google.com" } == true
    }

    fun reauthenticateAndDelete(password: String, onResult: (Boolean, Int) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            onResult(false, R.string.conf_delete_error)
            return
        }

        _isDeletingAccount.value = true

        if (isGoogleUser()) {
            viewModelScope.launch {
                authRepository.deleteAccount().onSuccess {
                    _isDeletingAccount.value = false
                    onResult(true, R.string.conf_delete_success)
                }.onFailure { error ->
                    _isDeletingAccount.value = false
                    val isRecentLoginRequired = error is FirebaseAuthRecentLoginRequiredException || error.message?.contains("recent", ignoreCase = true) == true
                    if (isRecentLoginRequired) onResult(false, R.string.conf_delete_recent_login_required)
                    else onResult(false, R.string.conf_delete_error)
                }
            }
        } else {
            val email = user.email
            if (email.isNullOrEmpty()) {
                _isDeletingAccount.value = false
                onResult(false, R.string.conf_delete_error)
                return
            }

            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    viewModelScope.launch {
                        authRepository.deleteAccount().onSuccess {
                            _isDeletingAccount.value = false
                            onResult(true, R.string.conf_delete_success)
                        }.onFailure {
                            _isDeletingAccount.value = false
                            onResult(false, R.string.conf_delete_error)
                        }
                    }
                } else {
                    _isDeletingAccount.value = false
                    onResult(false, R.string.conf_delete_wrong_password)
                }
            }
        }
    }

    /**
     * Factory actualizada para incluir el UserRepository.
     */
    class Factory(private val settingsManager: SettingsManager) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ConfigViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ConfigViewModel(settingsManager, AuthRepositoryImpl(), UserRepositoryImpl()) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}