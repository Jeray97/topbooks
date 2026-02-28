package com.example.topbooks.ui.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.preferences.SettingsManager
import com.example.topbooks.data.repository.AuthRepository
import com.example.topbooks.data.repository.AuthRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConfigViewModel(
    private val settingsManager: SettingsManager,
    private val authRepository: AuthRepository = AuthRepositoryImpl()
) : ViewModel() {

    // Estado para saber si el email está verificado
    private val _isEmailVerified = MutableStateFlow(true)
    val isEmailVerified: StateFlow<Boolean> = _isEmailVerified.asStateFlow()
    private val _isDeletingAccount = MutableStateFlow(false)
    val isDeletingAccount: StateFlow<Boolean> = _isDeletingAccount.asStateFlow()

    val darkModeEnabled: StateFlow<Boolean> = settingsManager.darkModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val notificationsEnabled: StateFlow<Boolean> = settingsManager.notificationsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    init {
        checkEmailVerification()
    }

    private fun checkEmailVerification() {
        _isEmailVerified.value = authRepository.isEmailVerified()
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch { settingsManager.saveDarkMode(enabled) }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch { settingsManager.saveNotifications(enabled) }
    }

    // Función para reenviar email de verificación
    fun resendVerificationEmail(onResult: (String) -> Unit) {
        authRepository.resendVerificationEmail { result ->
            if (result.isSuccess) {
                onResult("Correo de verificación reenviado.")
            } else {
                onResult("Error al enviar el correo. Inténtalo más tarde.")
            }
        }
    }

    // Función para cambiar contraseña
    fun sendPasswordReset(onResult: (String) -> Unit) {
        val email = authRepository.currentUser?.email
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

    // Estado para la preferencia de privacidad
    val publicJournalDefaultEnabled: StateFlow<Boolean> = settingsManager.publicJournalDefaultFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // Función para alternar la privacidad
    fun togglePublicJournalDefault(enabled: Boolean) {
        viewModelScope.launch { settingsManager.savePublicJournalDefault(enabled) }
    }

    fun signOut() {
        authRepository.logout()
    }

    fun deleteAccount(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isDeletingAccount.value = true

            authRepository.deleteAccount().onSuccess {
                _isDeletingAccount.value = false
                onResult(true, "Cuenta eliminada correctamente.")
            }.onFailure { error ->
                _isDeletingAccount.value = false
                onResult(false, error.message ?: "Error al eliminar la cuenta. Es posible que debas volver a iniciar sesión primero.")
            }
        }
    }

    class Factory(private val settingsManager: SettingsManager) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ConfigViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ConfigViewModel(settingsManager, AuthRepositoryImpl()) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}