package com.example.topbooks.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Modelo de datos para la UI del perfil
data class UserProfile(
    val displayName: String = "Cargando...",
    val email: String = "",
    val bio: String = "¡Hola! Soy nuevo en TopBooks.",
    val friendsCount: Int = 0,
    val booksCompleted: Int = 0,
    val photoUrl: String? = null
)

class ProfileViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        val uid = currentUser?.uid

        if (uid == null) {
            _userProfile.update { it.copy(displayName = "No conectado") }
            return
        }

        viewModelScope.launch {
            try {
                // 1. Cargar Datos Básicos del Usuario (users/{uid})
                val userDoc = db.collection("users").document(uid).get().await()

                val name = userDoc.getString("displayName") ?: "Usuario"
                val photo = userDoc.getString("photoURL")
                // TODO campo "bio" en la BD, lo leeríamos aquí:
                val bio = userDoc.getString("bio") ?: "Me apasiona leer, los libros de fantasía y misterio son mis favoritos!"

                // Actualizamos la UI con lo básico mientras cargamos los contadores
                _userProfile.update {
                    it.copy(
                        displayName = name,
                        email = currentUser.email ?: "",
                        photoUrl = photo,
                        bio = bio
                    )
                }

                // 2. Cargar Contador de Amigos (users/{uid}/friends)
                // Obtenemos todos los documentos de la subcolección y los contamos
                val friendsSnapshot = db.collection("users")
                    .document(uid)
                    .collection("friends")
                    .get()
                    .await()

                val realFriendsCount = friendsSnapshot.size()

                // 3. Cargar Contador de Libros Completados (users/{uid}/favorites)
                // Filtramos donde el campo 'list' sea 'Leídos'
                val completedBooksSnapshot = db.collection("users")
                    .document(uid)
                    .collection("favorites")
                    .whereEqualTo("list", "Leídos")
                    .get()
                    .await()

                val realBooksCompleted = completedBooksSnapshot.size()

                // Actualizamos el estado final con los números reales
                _userProfile.update {
                    it.copy(
                        friendsCount = realFriendsCount,
                        booksCompleted = realBooksCompleted
                    )
                }

            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error cargando perfil", e)
                _userProfile.update { it.copy(displayName = "Error de carga") }
            }
        }
    }

    fun signOut() {
        auth.signOut()
    }
}