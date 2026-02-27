package com.example.topbooks.data.repository

import com.example.topbooks.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface CommunityRepository {
    suspend fun searchUsers(query: String): Result<List<User>>
    suspend fun getSuggestedUsers(limit: Long = 15): Result<List<User>>
    suspend fun getMyFriendsIds(): Result<Set<String>>
}

class CommunityRepositoryImpl : CommunityRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

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

    override suspend fun getSuggestedUsers(limit: Long): Result<List<User>> {
        return try {
            // Buscamos unos cuantos usuarios al azar para sugerir
            val snapshot = db.collection("users").limit(limit).get().await()
            val users = snapshot.toObjects(User::class.java)
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMyFriendsIds(): Result<Set<String>> {
        val uid = auth.currentUser?.uid ?: return Result.success(emptySet())
        return try {
            val snapshot = db.collection("users").document(uid).collection("friends").get().await()
            val ids = snapshot.documents.map { it.id }.toSet()
            Result.success(ids)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}