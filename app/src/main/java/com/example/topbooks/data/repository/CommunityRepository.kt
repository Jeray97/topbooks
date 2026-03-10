package com.example.topbooks.data.repository

import com.example.topbooks.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * 1. DEFINICIÓN DE LA INTERFAZ
 * Contrato para las operaciones relacionadas con la comunidad y el descubrimiento de usuarios.
 */
interface CommunityRepository {
    /** Busca usuarios por nombre. */
    suspend fun searchUsers(query: String): Result<List<User>>
    /** Obtiene una lista rápida de usuarios recomendados para seguir. */
    suspend fun getSuggestedUsers(limit: Long = 15): Result<List<User>>
    /** Obtiene únicamente los IDs de los usuarios a los que el usuario actual sigue (amigos). */
    suspend fun getMyFriendsIds(): Result<Set<String>>
}

/**
 * 2. IMPLEMENTACIÓN DE LA INTERFAZ
 * * Se conecta a Firebase Firestore para consultar la colección general de "users"
 * y las subcolecciones de amistades.
 */
class CommunityRepositoryImpl : CommunityRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Busca usuarios en la comunidad cuyo nombre empiece por el texto introducido.
     * * * TÉCNICA DE BÚSQUEDA AVANZADA EN FIRESTORE:
     * Al no existir un comando "LIKE %texto%" en Firebase, se utiliza el campo guardado
     * en minúsculas [displayNameLowercase] y se ordenan los resultados alfabéticamente.
     * El carácter "\uf8ff" es el último carácter conocido en Unicode, por lo que la consulta
     * trae todos los nombres que empiecen por "q" y terminen antes de que cambie la siguiente letra.
     *
     * @param query El texto a buscar (se limpiará y pasará a minúsculas automáticamente).
     */
    override suspend fun searchUsers(query: String): Result<List<User>> {
        return try {
            val q = query.lowercase().trim()
            val snapshot = db.collection("users")
                .orderBy("displayNameLowercase")
                .startAt(q)
                .endAt(q + "\uf8ff")
                .get()
                .await()
            val users = snapshot.toObjects(User::class.java)
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene una lista genérica de usuarios para sugerir en la pantalla de "Amigos".
     * * Ideal para rellenar la pantalla cuando el usuario aún no ha buscado a nadie.
     *
     * @param limit Cantidad máxima de perfiles a descargar (por defecto 15 para no gastar cuota de Firebase).
     */
    override suspend fun getSuggestedUsers(limit: Long): Result<List<User>> {
        return try {
            // Buscamos unos cuantos usuarios al límite establecido
            val snapshot = db.collection("users").limit(limit).get().await()
            val users = snapshot.toObjects(User::class.java)
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Devuelve un conjunto (Set) con los IDs (uid) de todos los amigos del usuario actual logueado.
     * * Se usa un Set en lugar de una List para realizar búsquedas hiper-rápidas en memoria
     * cuando queremos comprobar si alguien es nuestro amigo (ej. `friendsIds.contains(uid)`).
     */
    override suspend fun getMyFriendsIds(): Result<Set<String>> {
        // Comprobación de seguridad: Si no hay nadie logueado, devolvemos un set vacío.
        val uid = auth.currentUser?.uid ?: return Result.success(emptySet())
        return try {
            // Entramos en la subcolección "friends" dentro del documento del usuario actual
            val snapshot = db.collection("users").document(uid).collection("friends").get().await()

            // Solo nos interesan los IDs de los documentos (que corresponden al UID del amigo)
            val ids = snapshot.documents.map { it.id }.toSet()
            Result.success(ids)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}