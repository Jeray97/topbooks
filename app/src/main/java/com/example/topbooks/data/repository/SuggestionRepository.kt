package com.example.topbooks.data.repository

import android.util.Log
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
    private val TAG = "SUGGESTION_DEBUG"

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
            Log.d(TAG, "=== INICIANDO ENVÍO DE SUGERENCIA ===")
            Log.d(TAG, "Categoría: $category")
            Log.d(TAG, "Título: $title")
            Log.d(TAG, "Mensaje: ${message.take(50)}...")

            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e(TAG, "ERROR: Usuario no autenticado")
                return Result.failure(Exception("Usuario no autenticado"))
            }

            Log.d(TAG, "Usuario autenticado: ${currentUser.uid}")
            Log.d(TAG, "Email: ${currentUser.email}")

            // Obtener información del usuario
            Log.d(TAG, "Obteniendo datos del usuario desde Firestore...")
            val userDoc = db.collection("users").document(currentUser.uid).get().await()
            
            if (!userDoc.exists()) {
                Log.e(TAG, "ERROR: Documento de usuario no existe en Firestore")
                return Result.failure(Exception("Documento de usuario no encontrado"))
            }

            val userName = userDoc.getString("displayName") ?: "Usuario anónimo"
            val userEmail = currentUser.email ?: "sin-email"
            
            Log.d(TAG, "Nombre de usuario: $userName")
            Log.d(TAG, "Email de usuario: $userEmail")

            val suggestion = Suggestion(
                userId = currentUser.uid,
                userName = userName,
                userEmail = userEmail,
                category = category,
                title = title,
                message = message
            )

            Log.d(TAG, "Creando documento en colección 'suggestions'...")
            val docRef = db.collection("suggestions").document()
            val suggestionWithId = suggestion.copy(id = docRef.id)
            
            Log.d(TAG, "Guardando sugerencia con ID: ${docRef.id}")
            docRef.set(suggestionWithId).await()
            
            Log.d(TAG, "✓ SUGERENCIA ENVIADA EXITOSAMENTE")
            Log.d(TAG, "=== FIN DEL ENVÍO ===")
            
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "✗ ERROR AL ENVIAR SUGERENCIA", e)
            Log.e(TAG, "Tipo de excepción: ${e.javaClass.simpleName}")
            Log.e(TAG, "Mensaje: ${e.message}")
            Log.e(TAG, "=== FIN DEL ENVÍO CON ERROR ===")
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
            Log.d(TAG, "=== OBTENIENDO MIS SUGERENCIAS ===")
            
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e(TAG, "ERROR: Usuario no autenticado")
                return Result.failure(Exception("Usuario no autenticado"))
            }

            Log.d(TAG, "Usuario: ${currentUser.uid}")
            Log.d(TAG, "Consultando colección 'suggestions'...")

            val snapshot = db.collection("suggestions")
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val suggestions = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Suggestion::class.java)
            }

            Log.d(TAG, "✓ Se encontraron ${suggestions.size} sugerencias")
            Log.d(TAG, "=== FIN DE CONSULTA ===")

            Result.success(suggestions)
        } catch (e: Exception) {
            Log.e(TAG, "✗ ERROR AL OBTENER SUGERENCIAS", e)
            Log.e(TAG, "Tipo de excepción: ${e.javaClass.simpleName}")
            Log.e(TAG, "Mensaje: ${e.message}")
            Log.e(TAG, "=== FIN DE CONSULTA CON ERROR ===")
            Result.failure(e)
        }
    }
}
