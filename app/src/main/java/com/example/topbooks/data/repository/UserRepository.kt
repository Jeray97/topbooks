package com.example.topbooks.data.repository

import android.content.Context
import com.example.topbooks.data.local.AppDatabase
import com.example.topbooks.data.local.NetworkMonitor
import com.example.topbooks.data.local.UserDao
import com.example.topbooks.data.local.toDomain
import com.example.topbooks.data.local.toEntity
import com.example.topbooks.data.model.User
import com.example.topbooks.ui.profile.SimpleUser
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

    /** Obtiene la lista de amigos del usuario. */
    suspend fun getFriendsList(userId: String): Result<List<SimpleUser>>

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

    /**
     * Borrado en cascada manual (Client-Side).
     * Busca y elimina todos los rastros del usuario en la base de datos antes de borrar su cuenta.
     */
    suspend fun deleteAllUserData(uid: String): Result<Boolean>
}

/**
 * 2. IMPLEMENTACIÓN DE LA INTERFAZ
 * Se conecta a la colección principal "users" de Firebase Firestore.
 */
class UserRepositoryImpl(context: Context? = null) : UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val userDao: UserDao? = context?.let { AppDatabase.getInstance(it).userDao() }
    private val networkMonitor: NetworkMonitor? = context?.let { NetworkMonitor(it) }
    
    companion object {
        private const val CACHE_VALIDITY_MS = 60 * 60 * 1000L // 1 hora
    }

    override fun getCurrentUserId(): String? = auth.currentUser?.uid

    /**
     * Obtiene el documento completo de un usuario y lo mapea al modelo [User].
     */
    override suspend fun getUserProfile(userId: String): Result<User?> {
        return try {
            val cachedUser = userDao?.getUserById(userId)
            val isOnline = networkMonitor?.isCurrentlyOnline() ?: true
            
            if (cachedUser != null && (!isOnline || System.currentTimeMillis() - cachedUser.cachedAt < CACHE_VALIDITY_MS)) {
                return Result.success(cachedUser.toDomain())
            }
            
            if (!isOnline && cachedUser != null) {
                return Result.success(cachedUser.toDomain())
            }
            
            val doc = db.collection("users").document(userId).get().await()
            val user = doc.toObject(User::class.java)
            if (user != null) {
                userDao?.insertUser(user.toEntity())
            }
            Result.success(user)
        } catch (e: Exception) {
            val cachedUser = userDao?.getUserById(userId)
            if (cachedUser != null) {
                Result.success(cachedUser.toDomain())
            } else {
                Result.failure(e)
            }
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
        return try {
            val cachedUser = userDao?.getUserById(uid)
            val isOnline = networkMonitor?.isCurrentlyOnline() ?: true
            
            if (cachedUser != null && (!isOnline || System.currentTimeMillis() - cachedUser.cachedAt < CACHE_VALIDITY_MS)) {
                return cachedUser.favoriteGenres
            }
            
            if (!isOnline && cachedUser != null) {
                return cachedUser.favoriteGenres
            }
            
            val snapshot = FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .await()

            val genres = (snapshot.get("favoriteGenres") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            
            if (cachedUser != null) {
                userDao?.insertUser(cachedUser.toDomain().copy(favoriteGenres = genres).toEntity())
            }
            
            genres
        } catch (e: Exception) {
            val cachedUser = userDao?.getUserById(uid)
            cachedUser?.favoriteGenres ?: emptyList()
        }
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

    override suspend fun getFriendsList(userId: String): Result<List<SimpleUser>> {
        return try {
            // Leemos directamente la subcolección donde ya guardamos foto y nombre
            val snapshot = db.collection("users").document(userId).collection("friends").get().await()

            val friends = snapshot.documents.map { doc ->
                SimpleUser(
                    uid = doc.id, // ¡El ID del documento es el UID de tu amigo!
                    name = doc.getString("displayName") ?: "Lector",
                    photo = doc.getString("photoUrl") ?: ""
                )
            }
            Result.success(friends)
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

    /**
     * Borrado en cascada manual (Client-Side).
     * Busca y elimina todos los rastros del usuario en la base de datos antes de borrar su cuenta.
     */
    override suspend fun deleteAllUserData(uid: String): Result<Boolean> = kotlinx.coroutines.coroutineScope {
        try {
            val db = FirebaseFirestore.getInstance()
            val batch = db.batch()

            // 1. LIMPIAR AMISTADES (Rastro en otros usuarios)
            val friendsSnapshot = db.collection("users").document(uid).collection("friends").get().await()
            for (doc in friendsSnapshot.documents) {
                val friendId = doc.id
                // Vamos al documento del amigo, y borramos a nuestro usuario de SU lista de amigos
                val reverseFriendRef = db.collection("users").document(friendId).collection("friends").document(uid)
                batch.delete(reverseFriendRef)

                // Borramos la referencia en nuestra propia lista
                batch.delete(doc.reference)
            }

            // 2. LIMPIAR RESEÑAS GLOBALES (Collection Group)
            val reviewsSnapshot = db.collectionGroup("reviews").whereEqualTo("userId", uid).get().await()
            for (doc in reviewsSnapshot.documents) {
                batch.delete(doc.reference)
            }

            // 3. LIMPIAR COMENTARIOS GLOBALES (Collection Group) <-- ¡AÑADIDO!
            // Busca todos los comentarios que haya hecho este usuario en cualquier libro
            val commentsSnapshot = db.collectionGroup("comments").whereEqualTo("userId", uid).get().await()
            for (doc in commentsSnapshot.documents) {
                batch.delete(doc.reference)
            }

            // 4. LIMPIAR SUBCOLECCIONES PROPIAS
            val collectionsToClean = listOf("favorites", "read_books", "bookmarks")
            for (collectionName in collectionsToClean) {
                val subColSnapshot = db.collection("users").document(uid).collection(collectionName).get().await()
                for (doc in subColSnapshot.documents) {
                    batch.delete(doc.reference)
                }
            }

            // 5. BORRAR EL DOCUMENTO PRINCIPAL DEL USUARIO
            val userMainRef = db.collection("users").document(uid)
            batch.delete(userMainRef)

            // 6. EJECUTAR TO-DO EL BLOQUE DE GOLPE
            batch.commit().await()

            Result.success(true)
        } catch (e: Exception) {
            android.util.Log.e("DeleteCascade", "Error borrando datos del usuario: ${e.message}")
            Result.failure(e)
        }
    }
}