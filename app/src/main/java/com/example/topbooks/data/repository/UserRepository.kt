package com.example.topbooks.data.repository

import com.example.topbooks.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface UserRepository {
    fun getCurrentUserId(): String?
    suspend fun getUserProfile(userId: String): Result<User?>
    suspend fun getFavoriteCovers(userId: String, limit: Long = 5): Result<List<String>>
    suspend fun getFavoriteIds(userId: String): Result<List<String>>
    suspend fun isFriend(myUid: String, targetUid: String): Result<Boolean>
    suspend fun toggleFriendship(myUid: String, targetUid: String, targetName: String, targetPhoto: String, isAdding: Boolean): Result<Boolean>
    suspend fun updateAvatar(userId: String, avatarUrl: String): Result<Boolean>
    suspend fun updateProfileData(userId: String, name: String, bio: String): Result<Boolean>
}

class UserRepositoryImpl : UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun getCurrentUserId(): String? = auth.currentUser?.uid

    override suspend fun getUserProfile(userId: String): Result<User?> {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            Result.success(doc.toObject(User::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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
            Result.success(doc.exists())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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
                friendRef.delete().await()
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateAvatar(userId: String, avatarUrl: String): Result<Boolean> {
        return try {
            db.collection("users").document(userId).update("photoURL", avatarUrl).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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
}