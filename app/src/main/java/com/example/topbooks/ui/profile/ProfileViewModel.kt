package com.example.topbooks.ui.profile

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

// Modelo de datos para la UI del perfil
data class UserProfile(
    val displayName: String = "Cargando...",
    val email: String = "",
    val bio: String = "¡Hola! Soy nuevo en TopBooks.",
    val friendsCount: Int = 0,
    val booksCompleted: Int = 0,
    val photoUrl: String? = null,
    val favoriteCovers: List<String> = emptyList(),
    val favoriteIds: List<String> = emptyList()
)

class ProfileViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile

    init {
        loadUserProfileListener()
    }

    private fun loadUserProfileListener() {
        val uid = auth.currentUser?.uid ?: return

        // 1. Escuchar datos básicos del usuario
        db.collection("users").document(uid)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let { doc ->
                    _userProfile.update {
                        it.copy(displayName = doc.getString("displayName") ?: "Usuario")
                    }
                }
            }

        // 2. Escuchar FAVORITOS en tiempo real
        db.collection("users")
            .document(uid)
            .collection("favorites")
            .whereEqualTo("list", "Favoritos")
            .limit(3)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                snapshot?.let { querySnapshot ->
                    val covers = querySnapshot.documents.mapNotNull { it.getString("imageUrl") }
                    val ids = querySnapshot.documents.map { it.id }

                    _userProfile.update {
                        it.copy(
                            favoriteCovers = covers,
                            favoriteIds = ids
                        )
                    }
                }
            }

        // 3. Escuchar CONTEOS (Amigos y Leídos)
        // Para los contadores, lo ideal es lo mismo:
        db.collection("users").document(uid).collection("favorites")
            .whereEqualTo("list", "Leídos")
            .addSnapshotListener { snapshot, _ ->
                _userProfile.update { it.copy(booksCompleted = snapshot?.size() ?: 0) }
            }
    }

    fun signOut() {
        auth.signOut()
    }
}