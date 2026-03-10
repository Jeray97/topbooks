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

/**
 * ViewModel encargado de gestionar la lógica de la pantalla de Configuración ([ConfigScreen]).
 * * Conecta las interacciones del usuario con las preferencias locales ([SettingsManager])
 * y la gestión avanzada de la cuenta en la nube ([AuthRepository]).
 */
class ConfigViewModel(
    private val settingsManager: SettingsManager,
    private val authRepository: AuthRepository = AuthRepositoryImpl()
) : ViewModel() {

    // --- ESTADOS LOCALES ---

    private val _isEmailVerified = MutableStateFlow(true)
    val isEmailVerified: StateFlow<Boolean> = _isEmailVerified.asStateFlow()

    private val _isDeletingAccount = MutableStateFlow(false)
    val isDeletingAccount: StateFlow<Boolean> = _isDeletingAccount.asStateFlow()

    // --- ESTADOS DE PREFERENCIAS (DATASTORE) ---
    // TÉCNICA DE RENDIMIENTO: Al usar 'SharingStarted.WhileSubscribed(5000)', el ViewModel
    // dejará de observar la base de datos local 5 segundos después de que el usuario salga
    // de la pantalla de configuración, ahorrando batería y recursos del sistema.

    val darkModeEnabled: StateFlow<Boolean> = settingsManager.darkModeFlow
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = false)

    val notificationsEnabled: StateFlow<Boolean> = settingsManager.notificationsFlow
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = true)

    val publicJournalDefaultEnabled: StateFlow<Boolean> = settingsManager.publicJournalDefaultFlow
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = false)

    init {
        refreshVerificationStatus()
    }

    /** Obliga a Firebase a actualizar los datos del usuario actual para ver si ya verificó su correo. */
    fun refreshVerificationStatus() {
        viewModelScope.launch {
            authRepository.reloadUser()
            _isEmailVerified.value = authRepository.isEmailVerified()
        }
    }

    // --- FUNCIONES DE TOGGLE (INTERRUPTORES) ---
    fun toggleDarkMode(enabled: Boolean) = viewModelScope.launch { settingsManager.saveDarkMode(enabled) }
    fun toggleNotifications(enabled: Boolean) = viewModelScope.launch { settingsManager.saveNotifications(enabled) }
    fun togglePublicJournalDefault(enabled: Boolean) = viewModelScope.launch { settingsManager.savePublicJournalDefault(enabled) }

    /** Reenvía el correo de verificación de identidad a la bandeja de entrada del usuario. */
    fun resendVerificationEmail(onResult: (String) -> Unit) {
        authRepository.resendVerificationEmail { result ->
            if (result.isSuccess) onResult("Correo de verificación reenviado.")
            else onResult("Error al enviar el correo. Inténtalo más tarde.")
        }
    }

    /** Solicita a Firebase que envíe un correo electrónico con un enlace para cambiar la contraseña. */
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

    /** Cierra la sesión activa en este dispositivo. */
    fun signOut() {
        authRepository.logout()
    }

    /**
     * Comprueba los proveedores de inicio de sesión vinculados a la cuenta actual.
     * @return 'true' si el usuario se registró usando el botón de Google.
     */
    fun isGoogleUser(): Boolean {
        val user = FirebaseAuth.getInstance().currentUser
        return user?.providerData?.any { it.providerId == "google.com" } == true
    }

    /**
     * Maneja el proceso crítico y destructivo de eliminar una cuenta de forma definitiva.
     * * REGLA DE SEGURIDAD DE FIREBASE: Para evitar que alguien coja el móvil desbloqueado de otro y borre
     * su cuenta, Firebase exige re-autenticar al usuario inmediatamente antes de borrar.
     *
     * @param password La contraseña actual introducida por el usuario para confirmar su identidad.
     * @param onResult Callback que devuelve un booleano (éxito/fallo) y el ID de recurso (String) del mensaje resultante.
     */
    fun reauthenticateAndDelete(password: String, onResult: (Boolean, Int) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            onResult(false, R.string.conf_delete_error)
            return
        }

        _isDeletingAccount.value = true // Activa el spinner de carga en el botón rojo

        if (isGoogleUser()) {
            // Los usuarios de Google no tienen contraseña tradicional. Intentamos borrar directamente.
            viewModelScope.launch {
                authRepository.deleteAccount().onSuccess {
                    _isDeletingAccount.value = false
                    onResult(true, R.string.conf_delete_success)
                }.onFailure { error ->
                    _isDeletingAccount.value = false

                    // Si Firebase exige inicio de sesión reciente para la cuenta de Google, se lo indicamos
                    val isRecentLoginRequired = error is FirebaseAuthRecentLoginRequiredException || error.message?.contains("recent", ignoreCase = true) == true
                    if (isRecentLoginRequired) onResult(false, R.string.conf_delete_recent_login_required)
                    else onResult(false, R.string.conf_delete_error)
                }
            }
        } else {
            // Usuarios de Correo/Contraseña: Necesitamos crear una credencial con la contraseña dada y reautenticar.
            val email = user.email
            if (email.isNullOrEmpty()) {
                _isDeletingAccount.value = false
                onResult(false, R.string.conf_delete_error)
                return
            }

            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Si la contraseña era correcta, procedemos a borrar la cuenta definitivamente
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
                    // Contraseña incorrecta, abortamos operación
                    _isDeletingAccount.value = false
                    onResult(false, R.string.conf_delete_wrong_password)
                }
            }
        }
    }

    /**
     * Clase factoría requerida por el sistema de Android para poder inyectar parámetros
     * (como el [SettingsManager]) en el constructor del ViewModel durante su creación.
     */
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