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

class AuthViewModel(
    // Aquí inyectamos la implementación en la interfaz
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
            isLoadingProfile = false
            return
        }

        isLoadingProfile = true

        // RUTA ESTÁNDAR: users/{uid}
        FirebaseFirestore.getInstance()
            .collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                val completed = doc.getBoolean("isTutorialCompleted") ?: false
                isTutorialCompleted = completed
                isLoadingProfile = false
                onComplete(completed)
            }
            .addOnFailureListener {
                // Si falla (ej: sin internet), asumimos completado para no bloquear
                isTutorialCompleted = true
                isLoadingProfile = false
                onComplete(true)
            }
    }

    fun login(email: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isAuthenticating = true
            repository.login(email, pass).onSuccess {
                currentUser = repository.currentUser
                checkUserProfile {
                    isAuthenticating = false
                    onSuccess()
                }
            }.onFailure {
                errorMessage = it.localizedMessage
                isAuthenticating = false
            }
        }
    }

    fun register(name: String, email: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isAuthenticating = true
            repository.register(name, email, pass).onSuccess {
                currentUser = repository.currentUser
                // Al registrarse, el tutorial NO está completado
                isTutorialCompleted = false
                isAuthenticating = false
                onSuccess()
            }.onFailure {
                errorMessage = it.localizedMessage
                isAuthenticating = false
            }
        }
    }

    fun loginWithGoogle(token: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isAuthenticating = true
            repository.loginWithGoogle(token).onSuccess {
                currentUser = repository.currentUser
                checkUserProfile {
                    isAuthenticating = false
                    onSuccess()
                }
            }.onFailure {
                errorMessage = it.localizedMessage
                isAuthenticating = false
            }
        }
    }

    fun signOut() {
        repository.logout()
        currentUser = null
        isTutorialCompleted = true
    }

    fun clearError() { errorMessage = null }
}