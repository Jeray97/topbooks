package com.example.topbooks.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.model.Review
import com.example.topbooks.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    val uiState: StateFlow<ProfileUiState> = _uiState

    // Carga el perfil (si userId es null, carga el mío)
    fun loadProfile(targetUserId: String? = null) {
        val myUid = auth.currentUser?.uid ?: return
        val finalUserId = targetUserId ?: myUid
        val isMe = finalUserId == myUid

        _uiState.update { it.copy(isLoading = true, isMe = isMe) }

        // 1. Datos básicos y estadísticas (en tiempo real si es el mío)
        val docRef = db.collection("users").document(finalUserId)

        if (isMe) {
            docRef.addSnapshotListener { snapshot, _ ->
                snapshot?.let { doc ->
                    val user = doc.toObject(User::class.java) ?: User()
                    _uiState.update { it.copy(user = user, isLoading = false) }
                }
            }
            // También escuchamos cambios en favoritos/leídos/amigos para el contador
            listenToCounts(finalUserId)
        } else {
            // Para otros, una sola descarga basta (o puedes poner otro listener si quieres)
            viewModelScope.launch {
                try {
                    val doc = docRef.get().await()
                    val user = doc.toObject(User::class.java) ?: User()
                    _uiState.update { it.copy(user = user, isLoading = false) }
                    loadExtraData(finalUserId)
                } catch (e: Exception) {
                    Log.e("ProfileVM", "Error loading profile: ${e.message}")
                }
            }
        }
    }

    private fun listenToCounts(uid: String) {
        // Contador de Reseñas
        db.collection("reviews").whereEqualTo("userId", uid)
            .addSnapshotListener { snp, _ -> _uiState.update { it.copy(user = it.user.copy(reviewsCount = snp?.size() ?: 0)) } }

        // Contador de Amigos
        db.collection("users").document(uid).collection("friends")
            .addSnapshotListener { snp, _ -> _uiState.update { it.copy(user = it.user.copy(friendsCount = snp?.size() ?: 0)) } }

        // Contador de Leídos
        db.collection("users").document(uid).collection("favorites").whereEqualTo("list", "Leídos")
            .addSnapshotListener { snp, _ -> _uiState.update { it.copy(user = it.user.copy(booksCompleted = snp?.size() ?: 0)) } }

        // Portadas de favoritos
        db.collection("users").document(uid).collection("favorites").whereEqualTo("list", "Favoritos").limit(3)
            .addSnapshotListener { snp, _ ->
                val covers = snp?.documents?.mapNotNull { it.getString("imageUrl") } ?: emptyList()
                val ids = snp?.documents?.map { it.id } ?: emptyList()
                _uiState.update { it.copy(favoriteCovers = covers, favoriteIds = ids) }
            }
    }

    private suspend fun loadExtraData(uid: String) {
        // Carga estática para perfiles ajenos
        val favs = db.collection("users").document(uid).collection("favorites").whereEqualTo("list", "Favoritos").limit(3).get().await()
        val covers = favs.documents.mapNotNull { it.getString("imageUrl") }
        val ids = favs.documents.map { it.id }
        _uiState.update { it.copy(favoriteCovers = covers, favoriteIds = ids) }
    }

    // Funciones de edición (solo funcionan si isMe es true)
    fun updateAvatar(avatarName: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update("photoURL", avatarName)
        propagateUpdate(uid, mapOf("photoUrl" to avatarName))
    }

    fun updateProfileData(name: String, bio: String) {
        val uid = auth.currentUser?.uid ?: return
        val updates = mapOf("displayName" to name, "displayNameLowercase" to name.lowercase(), "bio" to bio)
        db.collection("users").document(uid).update(updates)
        propagateUpdate(uid, mapOf("displayName" to name))
    }

    private fun propagateUpdate(uid: String, changes: Map<String, Any>) {
        viewModelScope.launch {
            try {
                val snp = db.collectionGroup("friends").whereEqualTo(FieldPath.documentId(), uid).get().await()
                val batch = db.batch()
                for (doc in snp.documents) batch.update(doc.reference, changes)
                batch.commit()
            } catch (e: Exception) { Log.e("ProfileVM", "Propagate error") }
        }
    }
}