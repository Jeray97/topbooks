package com.example.topbooks.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.R
import com.example.topbooks.data.repository.AuthRepository
import com.example.topbooks.data.repository.AuthRepositoryImpl
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 1. DEFINIMOS EL ESTADO DE LA UI
 * * Esta clase encapsula to-do lo que la pantalla de Login/Registro necesita saber para pintarse.
 *
 * @property currentUser El usuario actualmente logueado en Firebase (null si no hay sesión).
 * @property isTutorialCompleted Define si el usuario ya eligió sus géneros favoritos. Determina si navega a Home o al Onboarding.
 * @property isAuthenticating Si es true, muestra un indicador de carga durante el login/registro.
 * @property isLoadingProfile Si es true, muestra carga mientras cruzamos datos con Firestore.
 * @property errorMessage ID del recurso de texto (R.string) que contiene el error a mostrar al usuario.
 */
data class AuthUiState(
    val currentUser: FirebaseUser? = null,
    val isTutorialCompleted: Boolean = true,
    val isAuthenticating: Boolean = false,
    val isLoadingProfile: Boolean = false,
    val errorMessage: Int? = null
)

/**
 * ViewModel encargado de la lógica de autenticación.
 * * Actúa como puente entre la Interfaz de Usuario (Compose) y los datos ([AuthRepository]).
 */
class AuthViewModel(
    private val repository: AuthRepository = AuthRepositoryImpl()
) : ViewModel() {

    // 2. INICIALIZAMOS EL STATEFLOW (Recomendación oficial de arquitectura de Android)
    private val _uiState = MutableStateFlow(AuthUiState(currentUser = repository.currentUser))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Al arrancar la app, si ya hay un usuario guardado, verificamos si completó el tutorial
        checkUserProfile()
    }

    /**
     * Verifica en la base de datos de Firestore si el usuario actual ha completado el tutorial inicial.
     * También aprovecha este momento para actualizar el Token de notificaciones del dispositivo.
     */
    fun checkUserProfile(onComplete: (Boolean) -> Unit = {}) {
        val user = _uiState.value.currentUser
        if (user == null) {
            Log.w("AUTH_DEBUG", "checkUserProfile: No hay usuario actual (null)")
            _uiState.update { it.copy(isLoadingProfile = false) }
            return
        }

        Log.d("AUTH_DEBUG", "3. checkUserProfile: Cargando perfil para UID: ${user.uid}")
        _uiState.update { it.copy(isLoadingProfile = true) }

        // Lanzamos la actualización del token de Firebase Cloud Messaging para asegurar que reciba notificaciones
        updateFcmToken(user.uid)

        FirebaseFirestore.getInstance()
            .collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                Log.d("AUTH_DEBUG", "4. checkUserProfile: Documento Firestore obtenido con éxito")
                val completed = doc.getBoolean("isTutorialCompleted") ?: false
                _uiState.update {
                    it.copy(isTutorialCompleted = completed, isLoadingProfile = false)
                }
                onComplete(completed)
            }
            .addOnFailureListener { e ->
                Log.e("AUTH_DEBUG", "ERROR checkUserProfile: No se pudo leer Firestore: ${e.message}")
                _uiState.update {
                    it.copy(isTutorialCompleted = true, isLoadingProfile = false)
                }
                onComplete(true)
            }
    }

    /**
     * Inicia sesión con correo y contraseña tradicionales.
     * * Usa [viewModelScope.launch para no bloquear la interfaz mientras espera a internet.
     */
    fun login(email: String, pass: String, onSuccess: () -> Unit) {
        Log.d("AUTH_DEBUG", "Iniciando login tradicional...")
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthenticating = true) }
            repository.login(email, pass).onSuccess {
                _uiState.update { it.copy(currentUser = repository.currentUser) }
                checkUserProfile {
                    _uiState.update { it.copy(isAuthenticating = false) }
                    onSuccess()
                }
            }.onFailure {
                Log.e("AUTH_DEBUG", "ERROR login: ${it.message}")
                _uiState.update { state ->
                    state.copy(errorMessage = translateAuthError(it), isAuthenticating = false)
                }
            }
        }
    }

    /**
     * Registra una cuenta nueva.
     * * Tras crearla, envía automáticamente un correo de verificación.
     */
    fun register(name: String, email: String, pass: String, onSuccess: () -> Unit) {
        Log.d("AUTH_DEBUG", "Iniciando registro de nuevo usuario...")
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthenticating = true) }
            repository.register(name, email, pass).onSuccess {
                val user = repository.currentUser
                _uiState.update { it.copy(currentUser = user) }
                Log.d("AUTH_DEBUG", "Registro exitoso. UID: ${user?.uid}")

                repository.sendEmailVerification().onSuccess {
                    Log.d("AUTH_DEBUG", "¡Correo de verificación ENVIADO al servidor de Firebase!")
                }.onFailure {
                    Log.e("AUTH_DEBUG", "¡FALLO al enviar correo de verificación! Motivo: ${it.message}")
                }

                user?.uid?.let { uid ->
                    updateFcmToken(uid)
                }

                _uiState.update {
                    it.copy(isTutorialCompleted = false, isAuthenticating = false)
                }
                onSuccess()
            }.onFailure {
                Log.e("AUTH_DEBUG", "ERROR registro: ${it.message}")
                _uiState.update { state ->
                    state.copy(errorMessage = translateAuthError(it), isAuthenticating = false)
                }
            }
        }
    }

    /** Inicia sesión utilizando una cuenta de Google (Botón de Google). */
    fun loginWithGoogle(token: String, onSuccess: () -> Unit) {
        Log.d("AUTH_DEBUG", "1. loginWithGoogle: Iniciando con token de Google...")
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthenticating = true) }
            repository.loginWithGoogle(token).onSuccess {
                _uiState.update { it.copy(currentUser = repository.currentUser) }
                Log.d("AUTH_DEBUG", "2. loginWithGoogle: Auth en Firebase OK. UID: ${_uiState.value.currentUser?.uid}")

                checkUserProfile { completed ->
                    Log.d("AUTH_DEBUG", "5. loginWithGoogle: Todo el proceso terminado. Tutorial: $completed")
                    _uiState.update { it.copy(isAuthenticating = false) }
                    onSuccess()
                }
            }.onFailure {
                Log.e("AUTH_DEBUG", "ERROR loginWithGoogle: ${it.message}")
                _uiState.update { state ->
                    state.copy(errorMessage = translateAuthError(it), isAuthenticating = false)
                }
            }
        }
    }

    /** Envía un correo automático para que el usuario pueda recuperar su contraseña olvidada. */
    fun resetPassword(email: String, onResult: (Boolean, String) -> Unit) {
        if (email.isBlank()) {
            onResult(false, "Por favor, introduce tu correo electrónico.")
            return
        }
        viewModelScope.launch {
            repository.sendPasswordResetEmail(email).onSuccess {
                onResult(true, "Se ha enviado un correo para restablecer tu contraseña.")
            }.onFailure {
                onResult(false, "No pudimos enviar el correo. Comprueba que está bien escrito.")
            }
        }
    }

    /** Comprueba de forma silenciosa si el usuario ha verificado su correo mediante el link enviado. */
    fun isEmailVerified(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            repository.reloadUser().onSuccess {
                val verified = repository.currentUser?.isEmailVerified == true
                onResult(verified)
            }.onFailure {
                onResult(false)
            }
        }
    }

    /**
     * TÉCNICA CLAVE DE NOTIFICACIONES PUSH:
     * * Recupera el identificador único del móvil (FCM Token) y lo guarda en el documento del usuario.
     * * Esto permite a Firebase saber exactamente a qué teléfono enviar los avisos sociales.
     */
    private fun updateFcmToken(uid: String) {
        Log.d("FCM_DEBUG", "Solicitando token a Firebase Messaging...")
        Firebase.messaging.token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.e("FCM_DEBUG", "Fallo crítico al obtener token FCM", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d("FCM_DEBUG", "Token obtenido: ${token?.take(10)}...")

            val data = mapOf("fcmToken" to token)

            FirebaseFirestore.getInstance().collection("users").document(uid)
                .set(data, SetOptions.merge())// Usamos merge para no borrar el resto del perfil
                .addOnSuccessListener {
                    Log.d("FCM_DEBUG", "¡Token sincronizado en Firestore correctamente!")
                }
                .addOnFailureListener { e ->
                    Log.e("FCM_DEBUG", "Error al guardar el token en Firestore: ${e.message}")
                }
        }
    }

    /**
     * TRADUCTOR DE ERRORES DE FIREBASE
     * * Convierte las excepciones frías y en inglés de Firebase en códigos de recursos (Int)
     * que apuntan a cadenas (Strings) traducidas, amigables y claras para el usuario final.
     */
    private fun translateAuthError(e: Throwable): Int {
        if (e !is com.google.firebase.auth.FirebaseAuthException) {
            return R.string.error_network_generic
        }
        return when (e.errorCode) {
            "ERROR_INVALID_CREDENTIAL", "auth/invalid-credential" -> R.string.error_auth_invalid_credential
            "ERROR_USER_NOT_FOUND", "auth/user-not-found" -> R.string.error_auth_user_not_found
            "ERROR_WRONG_PASSWORD", "auth/wrong-password" -> R.string.error_auth_wrong_password
            "ERROR_EMAIL_ALREADY_IN_USE", "auth/email-already-in-use" -> R.string.error_auth_email_already_in_use
            "ERROR_WEAK_PASSWORD", "auth/weak-password" -> R.string.error_auth_weak_password
            "ERROR_INVALID_EMAIL", "auth/invalid-email" -> R.string.error_auth_invalid_email
            "ERROR_USER_DISABLED", "auth/user-disabled" -> R.string.error_auth_user_disabled
            "ERROR_TOO_MANY_REQUESTS", "auth/too-many-requests" -> R.string.error_auth_too_many_requests
            "ERROR_OPERATION_NOT_ALLOWED", "auth/operation-not-allowed" -> R.string.error_auth_operation_not_allowed
            else -> R.string.error_auth_generic
        }
    }

    /** Cierra la sesión activa en el teléfono y limpia la información de la UI. */
    fun signOut() {
        Log.d("AUTH_DEBUG", "Cerrando sesión del usuario...")
        repository.logout()
        _uiState.update {
            it.copy(currentUser = null, isTutorialCompleted = true)
        }
    }

    /** Limpia los mensajes de error de la pantalla. */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Marca el tutorial como completado localmente para actualizar el estado de navegación
     * inmediatamente sin esperar a Firestore.
     */
    fun markTutorialCompleted() {
        _uiState.update { it.copy(isTutorialCompleted = true) }
    }
}