package com.example.topbooks.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class UserProfile(
    val displayName: String = "Cargando...",
    val email: String = "",
    val bio: String = "¡Hola! Soy nuevo en TopBooks.",
    val friendsCount: Int = 0,
    val booksCompleted: Int = 0,
    val photoUrl: String? = "capibara_1",
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

        db.collection("users").document(uid)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let { doc ->
                    _userProfile.update {
                        it.copy(
                            displayName = doc.getString("displayName") ?: "Usuario",
                            email = doc.getString("email") ?: "",
                            photoUrl = doc.getString("photoURL") ?: "capibara_1",
                            bio = doc.getString("bio") ?: "¡Hola! Soy nuevo en TopBooks."
                        )
                    }
                }
            }

        db.collection("users").document(uid).collection("favorites")
            .whereEqualTo("list", "Favoritos")
            .limit(3)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let { query ->
                    val covers = query.documents.mapNotNull { it.getString("imageUrl") }
                    val ids = query.documents.map { it.id }
                    _userProfile.update { it.copy(favoriteCovers = covers, favoriteIds = ids) }
                }
            }

        db.collection("users").document(uid).collection("favorites")
            .whereEqualTo("list", "Leídos")
            .addSnapshotListener { snapshot, _ ->
                _userProfile.update { it.copy(booksCompleted = snapshot?.size() ?: 0) }
            }

        db.collection("users").document(uid).collection("friends")
            .addSnapshotListener { snapshot, _ ->
                _userProfile.update { it.copy(friendsCount = snapshot?.size() ?: 0) }
            }
    }

    // --- CAMBIAR AVATAR Y PROPAGAR ---
    fun updateAvatar(avatarName: String) {
        val uid = auth.currentUser?.uid ?: return

        // 1. Actualizar mi perfil
        val update = mapOf("photoURL" to avatarName)
        db.collection("users").document(uid).set(update, SetOptions.merge())

        // 2. Propagar cambio a las listas de amigos de otros
        propagateUserUpdateToFriends(uid, mapOf("photoUrl" to avatarName))
    }

    // --- ACTUALIZAR DATOS DE TEXTO Y PROPAGAR ---
    fun updateProfileData(newName: String, newBio: String) {
        val uid = auth.currentUser?.uid ?: return

        // 1. Preparamos los datos incluyendo el lowercase para búsquedas
        val updates = mapOf(
            "displayName" to newName,
            "displayNameLowercase" to newName.lowercase(Locale.getDefault()),
            "bio" to newBio
        )

        // 2. Actualizamos mi perfil
        db.collection("users").document(uid).set(updates, SetOptions.merge())

        // 3. Propagar cambio de nombre a las listas de amigos de otros
        // Nota: "bio" y "lowercase" normalmente no se guardan en la lista de amigos, solo el nombre visible.
        propagateUserUpdateToFriends(uid, mapOf("displayName" to newName))
    }

    /**
     * Función avanzada: Busca en TODA la base de datos las subcolecciones "friends"
     * que contengan un documento con mi ID y actualiza los datos duplicados.
     */
    private fun propagateUserUpdateToFriends(myUid: String, changes: Map<String, Any>) {
        viewModelScope.launch {
            try {
                // collectionGroup busca en todas las colecciones llamadas "friends"
                db.collectionGroup("friends")
                    .whereEqualTo(FieldPath.documentId(), myUid) // Busca donde el ID del documento sea MI ID
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        val batch = db.batch() // Usamos batch para ser eficientes

                        for (document in querySnapshot.documents) {
                            // Actualizamos la referencia en la lista del amigo
                            batch.update(document.reference, changes)
                        }

                        // Ejecutamos todas las actualizaciones juntas
                        batch.commit().addOnFailureListener { e ->
                            Log.e("ProfileViewModel", "Error propagando actualización: ${e.message}")
                        }
                    }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error en consulta de grupo: ${e.message}")
            }
        }
    }
}