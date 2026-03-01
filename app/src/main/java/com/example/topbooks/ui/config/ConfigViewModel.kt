package com.example.topbooks.ui.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.topbooks.R
import com.example.topbooks.data.preferences.SettingsManager
import com.example.topbooks.data.repository.AuthRepository
import com.example.topbooks.data.repository.AuthRepositoryImpl
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
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

    private val _isEmailVerified = MutableStateFlow(true)
    val isEmailVerified: StateFlow<Boolean> = _isEmailVerified.asStateFlow()
    private val _isDeletingAccount = MutableStateFlow(false)
    val isDeletingAccount: StateFlow<Boolean> = _isDeletingAccount.asStateFlow()

    val darkModeEnabled: StateFlow<Boolean> = settingsManager.darkModeFlow
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = false)

    val notificationsEnabled: StateFlow<Boolean> = settingsManager.notificationsFlow
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = true)

    val publicJournalDefaultEnabled: StateFlow<Boolean> = settingsManager.publicJournalDefaultFlow
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = false)

    init {
        refreshVerificationStatus()
    }

    fun refreshVerificationStatus() {
        viewModelScope.launch {
            authRepository.reloadUser()
            _isEmailVerified.value = authRepository.isEmailVerified()
        }
    }

    fun toggleDarkMode(enabled: Boolean) = viewModelScope.launch { settingsManager.saveDarkMode(enabled) }
    fun toggleNotifications(enabled: Boolean) = viewModelScope.launch { settingsManager.saveNotifications(enabled) }
    fun togglePublicJournalDefault(enabled: Boolean) = viewModelScope.launch { settingsManager.savePublicJournalDefault(enabled) }

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

    // 🔥 DETECTA SI EL USUARIO ES DE GOOGLE
    fun isGoogleUser(): Boolean {
        val user = FirebaseAuth.getInstance().currentUser
        return user?.providerData?.any { it.providerId == "google.com" } == true
    }

    // 🔥 REAUTENTICA CON CONTRASEÑA Y LUEGO BORRA LA CUENTA
    fun reauthenticateAndDelete(password: String, onResult: (Boolean, Int) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            onResult(false, R.string.conf_delete_error)
            return
        }

        _isDeletingAccount.value = true

        if (isGoogleUser()) {
            // Usuarios de Google no tienen contraseña, se intenta borrar directo
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
            // Usuarios normales: Reautenticar con contraseña primero
            val email = user.email
            if (email.isNullOrEmpty()) {
                _isDeletingAccount.value = false
                onResult(false, R.string.conf_delete_error)
                return
            }

            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Contraseña correcta, procedemos a borrar
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
                    // Contraseña incorrecta
                    _isDeletingAccount.value = false
                    onResult(false, R.string.conf_delete_wrong_password)
                }
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