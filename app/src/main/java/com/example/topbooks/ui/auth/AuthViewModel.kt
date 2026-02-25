package com.example.topbooks.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.repository.AuthRepository
import com.example.topbooks.data.repository.AuthRepositoryImpl
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import com.google.firebase.firestore.SetOptions

class AuthViewModel(
    private val repository: AuthRepository = AuthRepositoryImpl()
) : ViewModel() {

    var currentUser by mutableStateOf<FirebaseUser?>(repository.currentUser)
        private set

    var isTutorialCompleted by mutableStateOf(true)
        private set

    var isAuthenticating by mutableStateOf(false)
        private set

    var isLoadingProfile by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        checkUserProfile()
    }

    fun checkUserProfile(onComplete: (Boolean) -> Unit = {}) {
        val user = currentUser
        if (user == null) {
            Log.w("AUTH_DEBUG", "checkUserProfile: No hay usuario actual (null)")
            isLoadingProfile = false
            return
        }

        Log.d("AUTH_DEBUG", "3. checkUserProfile: Cargando perfil para UID: ${user.uid}")
        isLoadingProfile = true

        // Lanzamos la actualización del token
        updateFcmToken(user.uid)

        FirebaseFirestore.getInstance()
            .collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                Log.d("AUTH_DEBUG", "4. checkUserProfile: Documento Firestore obtenido con éxito")
                val completed = doc.getBoolean("isTutorialCompleted") ?: false
                isTutorialCompleted = completed
                isLoadingProfile = false
                onComplete(completed)
            }
            .addOnFailureListener { e ->
                Log.e("AUTH_DEBUG", "ERROR checkUserProfile: No se pudo leer Firestore: ${e.message}")
                isTutorialCompleted = true
                isLoadingProfile = false
                onComplete(true)
            }
    }

    fun login(email: String, pass: String, onSuccess: () -> Unit) {
        Log.d("AUTH_DEBUG", "Iniciando login tradicional...")
        viewModelScope.launch {
            isAuthenticating = true
            repository.login(email, pass).onSuccess {
                currentUser = repository.currentUser
                checkUserProfile {
                    isAuthenticating = false
                    onSuccess()
                }
            }.onFailure {
                Log.e("AUTH_DEBUG", "ERROR login: ${it.message}")
                errorMessage = it.localizedMessage
                isAuthenticating = false
            }
        }
    }

    fun register(name: String, email: String, pass: String, onSuccess: () -> Unit) {
        Log.d("AUTH_DEBUG", "Iniciando registro de nuevo usuario...")
        viewModelScope.launch {
            isAuthenticating = true
            repository.register(name, email, pass).onSuccess {
                currentUser = repository.currentUser
                Log.d("AUTH_DEBUG", "Registro exitoso. UID: ${currentUser?.uid}")

                currentUser?.uid?.let { uid ->
                    updateFcmToken(uid)
                }

                isTutorialCompleted = false
                isAuthenticating = false
                onSuccess()
            }.onFailure {
                Log.e("AUTH_DEBUG", "ERROR registro: ${it.message}")
                errorMessage = it.localizedMessage
                isAuthenticating = false
            }
        }
    }

    fun loginWithGoogle(token: String, onSuccess: () -> Unit) {
        Log.d("AUTH_DEBUG", "1. loginWithGoogle: Iniciando con token de Google...")
        viewModelScope.launch {
            isAuthenticating = true
            repository.loginWithGoogle(token).onSuccess {
                currentUser = repository.currentUser
                Log.d("AUTH_DEBUG", "2. loginWithGoogle: Auth en Firebase OK. UID: ${currentUser?.uid}")

                checkUserProfile { completed ->
                    Log.d("AUTH_DEBUG", "5. loginWithGoogle: Todo el proceso terminado. Tutorial: $completed")
                    isAuthenticating = false
                    onSuccess()
                }
            }.onFailure {
                Log.e("AUTH_DEBUG", "ERROR loginWithGoogle: ${it.message}")
                errorMessage = it.localizedMessage
                isAuthenticating = false
            }
        }
    }

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
                .set(data, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("FCM_DEBUG", "¡Token sincronizado en Firestore correctamente!")
                }
                .addOnFailureListener { e ->
                    Log.e("FCM_DEBUG", "Error al guardar el token en Firestore: ${e.message}")
                }
        }
    }

    fun signOut() {
        Log.d("AUTH_DEBUG", "Cerrando sesión del usuario...")
        repository.logout()
        currentUser = null
        isTutorialCompleted = true
    }

    fun clearError() { errorMessage = null }
}