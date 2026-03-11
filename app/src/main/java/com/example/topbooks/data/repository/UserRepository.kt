package com.example.topbooks.data.repository

import com.example.topbooks.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await


/**
 * 1. DEFINICIÓN DE LA INTERFAZ
 * Contrato que centraliza todas las operaciones relacionadas con el perfil del usuario,
 * sus preferencias y el sistema de amistades (seguir/dejar de seguir).
 */
interface UserRepository {
    /** Devuelve el ID único (UID) del usuario que tiene la sesión iniciada actualmente. */
    fun getCurrentUserId(): String?

    /** Descarga todos los datos del perfil de un usuario (propio o de un amigo). */
    suspend fun getUserProfile(userId: String): Result<User?>

    /** Obtiene rápidamente solo las portadas (URLs) de los libros favoritos de un usuario para mostrarlas en su perfil. */
    suspend fun getFavoriteCovers(userId: String, limit: Long = 5): Result<List<String>>

    /** Obtiene una lista solo con los IDs de los libros que un usuario tiene en favoritos. */
    suspend fun getFavoriteIds(userId: String): Result<List<String>>

    /** Comprueba si el usuario actual sigue a otro usuario específico. */
    suspend fun isFriend(myUid: String, targetUid: String): Result<Boolean>

    /** Añade o elimina a un usuario de la lista de amigos. */
    suspend fun toggleFriendship(myUid: String, targetUid: String, targetName: String, targetPhoto: String, isAdding: Boolean): Result<Boolean>

    /** Actualiza la foto de perfil (avatar) del usuario en la base de datos. */
    suspend fun updateAvatar(userId: String, avatarUrl: String): Result<Boolean>

    /** Actualiza la información básica del perfil (nombre público y biografía). */
    suspend fun updateProfileData(userId: String, name: String, bio: String): Result<Boolean>

    /** Descarga los géneros literarios que el usuario marcó como favoritos durante el tutorial. */
    suspend fun getFavoriteGenres(uid: String): List<String>

    /** Actualiza la lista de géneros favoritos del usuario en la base de datos. */
    suspend fun updateFavoriteGenres(uid: String, genres: List<String>): Result<Boolean>

    /** Finaliza el tutorial guardando los géneros y libros iniciales en el perfil. */
    suspend fun completeTutorial(userId: String, genres: List<String>, books: List<String>): Result<Boolean>
}

/**
 * 2. IMPLEMENTACIÓN DE LA INTERFAZ
 * Se conecta a la colección principal "users" de Firebase Firestore.
 */
class UserRepositoryImpl : UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun getCurrentUserId(): String? = auth.currentUser?.uid

    /**
     * Obtiene el documento completo de un usuario y lo mapea al modelo [User].
     */
    override suspend fun getUserProfile(userId: String): Result<User?> {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            Result.success(doc.toObject(User::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * TÉCNICA DE OPTIMIZACIÓN:
     * Busca en la subcolección 'favorites' y extrae únicamente el campo 'bookImageUrl'.
     * Evita descargar objetos enteros cuando la UI solo necesita pintar las portadas.
     */
    override suspend fun getFavoriteCovers(userId: String, limit: Long): Result<List<String>> {
        return try {
            val favs = db.collection("users").document(userId).collection("favorites")
                .limit(limit).get().await()
            val covers = favs.documents.mapNotNull { it.getString("bookImageUrl") }
            Result.success(covers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFavoriteIds(userId: String): Result<List<String>> {
        return try {
            val favs = db.collection("users").document(userId).collection("favorites").get().await()
            val ids = favs.documents.mapNotNull { it.getString("bookId") }
            Result.success(ids)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isFriend(myUid: String, targetUid: String): Result<Boolean> {
        return try {
            val doc = db.collection("users").document(myUid)
                .collection("friends").document(targetUid).get().await()
            // Si el documento existe en la subcolección, significa que son amigos
            Result.success(doc.exists())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Añade o quita un amigo.
     * * TÉCNICA NOSQL (Desnormalización): Guardamos el nombre y la foto del amigo directamente
     * en el documento de la relación. Así, al cargar la lista de amigos, no necesitamos hacer
     * consultas extra para buscar cómo se llama cada uno.
     */
    override suspend fun toggleFriendship(myUid: String, targetUid: String, targetName: String, targetPhoto: String, isAdding: Boolean): Result<Boolean> {
        return try {
            val friendRef = db.collection("users").document(myUid).collection("friends").document(targetUid)
            if (isAdding) {
                friendRef.set(mapOf(
                    "displayName" to targetName,
                    "photoUrl" to targetPhoto,
                    "addedAt" to System.currentTimeMillis()
                )).await()
            } else {
                friendRef.delete().await() // Si dejamos de seguirlo, simplemente borramos el documento
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Utiliza [.update()] en lugar de [.set()] para modificar única y exclusivamente
     * el campo 'photoURL' sin alterar el resto de datos del perfil.
     */
    override suspend fun updateAvatar(userId: String, avatarUrl: String): Result<Boolean> {
        return try {
            db.collection("users").document(userId).update("photoURL", avatarUrl).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualiza varios campos específicos a la vez usando un mapa.
     */
    override suspend fun updateProfileData(userId: String, name: String, bio: String): Result<Boolean> {
        return try {
            db.collection("users").document(userId).update(
                mapOf(
                    "displayName" to name,
                    "bio" to bio
                )
            ).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun getFavoriteGenres(uid: String): List<String> {
        val snapshot = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .await()

        // Realizamos un casteo seguro a List<String>
        return snapshot.get("favoriteGenres") as? List<String> ?: emptyList()
    }

    /**
     * Sobreescribe el array de géneros favoritos en el documento del usuario.
     */
    override suspend fun updateFavoriteGenres(uid: String, genres: List<String>): Result<Boolean> {
        return try {
            db.collection("users").document(uid).update("favoriteGenres", genres).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun completeTutorial(userId: String, genres: List<String>, books: List<String>): Result<Boolean> {
        return try {
            val updates = mapOf(
                "isTutorialCompleted" to true,
                "favoriteGenres" to genres,
                "favoriteBooks" to books
            )
            db.collection("users").document(userId)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}