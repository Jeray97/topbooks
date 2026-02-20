package com.example.topbooks.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

data class ProfileUiState(
    val user: User = User(),
    val favoriteCovers: List<String> = emptyList(),
    val favoriteIds: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isMe: Boolean = false
)

class ProfileViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile(targetUserId: String? = null) {
        val myUid = auth.currentUser?.uid ?: return
        val finalUserId = if (targetUserId.isNullOrEmpty()) myUid else targetUserId
        val isMe = finalUserId == myUid

        _uiState.update { it.copy(isLoading = true, isMe = isMe) }

        val docRef = db.collection("users").document(finalUserId)

        if (isMe) {
            // --- MODO: MI PERFIL (Listeners en tiempo real) ---
            docRef.addSnapshotListener { snapshot, _ ->
                snapshot?.let { doc ->
                    val user = doc.toObject(User::class.java) ?: User()
                    _uiState.update { it.copy(user = user, isLoading = false) }
                }
            }
            listenToRealtimeCounts(finalUserId)
        } else {
            // --- MODO: PERFIL DE AMIGO (Carga de datos y conteos manuales) ---
            viewModelScope.launch {
                try {
                    // 1. Cargamos los datos básicos del usuario
                    val doc = docRef.get().await()
                    var user = doc.toObject(User::class.java) ?: User()

                    // 2. Ejecutamos todos los conteos en paralelo para ir rápido
                    val reviewsTask = async { db.collection("reviews").whereEqualTo("userId", finalUserId).get().await().size() }
                    val friendsTask = async { db.collection("users").document(finalUserId).collection("friends").get().await().size() }
                    val readTask = async { db.collection("users").document(finalUserId).collection("favorites").whereEqualTo("list", "Leídos").get().await().size() }
                    val favsTask = async { db.collection("users").document(finalUserId).collection("favorites").whereEqualTo("list", "Favoritos").limit(3).get().await() }

                    // Esperamos los resultados
                    val reviewsCount = reviewsTask.await()
                    val friendsCount = friendsTask.await()
                    val readCount = readTask.await()
                    val favsSnapshot = favsTask.await()

                    // Actualizamos el objeto user con los números reales
                    user = user.copy(
                        reviewsCount = reviewsCount,
                        friendsCount = friendsCount,
                        booksCompleted = readCount
                    )

                    // 3. Actualizamos el estado final
                    _uiState.update { it.copy(
                        user = user,
                        isLoading = false,
                        favoriteCovers = favsSnapshot.documents.mapNotNull { d -> d.getString("imageUrl") },
                        favoriteIds = favsSnapshot.documents.map { d -> d.id }
                    )}

                } catch (e: Exception) {
                    Log.e("ProfileVM", "Error cargando perfil ajeno: ${e.message}")
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    private fun listenToRealtimeCounts(uid: String) {
        // Escuchar Reseñas de mi perfil
        db.collection("reviews").whereEqualTo("userId", uid)
            .addSnapshotListener { snp, _ ->
                _uiState.update { it.copy(user = it.user.copy(reviewsCount = snp?.size() ?: 0)) }
            }

        // Escuchar Amigos de mi perfil
        db.collection("users").document(uid).collection("friends")
            .addSnapshotListener { snp, _ ->
                _uiState.update { it.copy(user = it.user.copy(friendsCount = snp?.size() ?: 0)) }
            }

        // Escuchar Leídos y Favoritos de mi perfil
        db.collection("users").document(uid).collection("favorites")
            .addSnapshotListener { snp, _ ->
                val docs = snp?.documents ?: emptyList()
                val readCount = docs.count { it.getString("list") == "Leídos" }
                val favs = docs.filter { it.getString("list") == "Favoritos" }.take(3)

                _uiState.update { it.copy(
                    user = it.user.copy(booksCompleted = readCount),
                    favoriteCovers = favs.mapNotNull { d -> d.getString("imageUrl") },
                    favoriteIds = favs.map { d -> d.id }
                )}
            }
    }

    // --- Funciones de edición (solo para mi perfil) ---
    fun updateAvatar(avatarName: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update("photoURL", avatarName)
        propagateUpdateToFriends(uid, mapOf("photoUrl" to avatarName))
    }

    fun updateProfileData(name: String, bio: String) {
        val uid = auth.currentUser?.uid ?: return
        val updates = mapOf(
            "displayName" to name,
            "displayNameLowercase" to name.lowercase(),
            "bio" to bio
        )
        db.collection("users").document(uid).update(updates)
        propagateUpdateToFriends(uid, mapOf("displayName" to name))
    }

    private fun propagateUpdateToFriends(uid: String, changes: Map<String, Any>) {
        viewModelScope.launch {
            try {
                val snp = db.collectionGroup("friends").whereEqualTo(FieldPath.documentId(), uid).get().await()
                val batch = db.batch()
                for (doc in snp.documents) batch.update(doc.reference, changes)
                batch.commit()
            } catch (e: Exception) { Log.e("ProfileVM", "Error propagando datos") }
        }
    }
}