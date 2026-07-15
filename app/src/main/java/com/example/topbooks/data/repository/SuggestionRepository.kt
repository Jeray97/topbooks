package com.example.topbooks.data.repository

import com.example.topbooks.data.model.Suggestion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para gestionar el buzón de sugerencias de los usuarios.
 * Las sugerencias se almacenan en la colección "suggestions" de Firestore.
 */
class SuggestionRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Envía una nueva sugerencia al buzón.
     * 
     * @param category Categoría de la sugerencia.
     * @param title Título breve.
     * @param message Descripción detallada.
     * @return Result con el ID de la sugerencia creada o un error.
     */
    suspend fun submitSuggestion(
        category: String,
        title: String,
        message: String
    ): Result<String> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))

            // Obtener información del usuario
            val userDoc = db.collection("users").document(currentUser.uid).get().await()
            val userName = userDoc.getString("displayName") ?: "Usuario anónimo"
            val userEmail = currentUser.email ?: "sin-email"

            val suggestion = Suggestion(
                userId = currentUser.uid,
                userName = userName,
                userEmail = userEmail,
                category = category,
                title = title,
                message = message
            )

            val docRef = db.collection("suggestions").document()
            val suggestionWithId = suggestion.copy(id = docRef.id)
            
            docRef.set(suggestionWithId).await()
            
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene todas las sugerencias del usuario actual.
     * 
     * @return Lista de sugerencias del usuario ordenadas por fecha (más recientes primero).
     */
    suspend fun getMySuggestions(): Result<List<Suggestion>> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val snapshot = db.collection("suggestions")
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val suggestions = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Suggestion::class.java)
            }

            Result.success(suggestions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
