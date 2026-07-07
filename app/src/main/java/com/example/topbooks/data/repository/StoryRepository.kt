package com.example.topbooks.data.repository

import com.example.topbooks.data.model.Story
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.concurrent.TimeUnit

interface StoryRepository {
    suspend fun createStory(story: Story): Result<String>
    suspend fun deleteStory(storyId: String): Result<Boolean>
    suspend fun getMyStories(): Result<List<Story>>
    suspend fun getStoriesByUser(userId: String): Result<List<Story>>
    suspend fun getFriendsStories(friendIds: List<String>): Result<List<Story>>
    suspend fun getCommunityStories(limit: Long = 20): Result<List<Story>>
    suspend fun markAsViewed(storyId: String, viewerId: String): Result<Boolean>
    suspend fun cleanupExpiredStories(): Result<Int>
}

class StoryRepositoryImpl : StoryRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private val STORY_DURATION_MS = TimeUnit.HOURS.toMillis(24)
    }

    override suspend fun createStory(story: Story): Result<String> {
        return try {
            val docRef = db.collection("stories").document()
            val now = Date()
            val expiresAt = Date(now.time + STORY_DURATION_MS)
            val newStory = story.copy(
                id = docRef.id,
                userId = auth.currentUser?.uid ?: "",
                expiresAt = expiresAt
            )
            docRef.set(newStory).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteStory(storyId: String): Result<Boolean> {
        return try {
            val myUid = auth.currentUser?.uid ?: return Result.failure(Exception("No autenticado"))
            val story = db.collection("stories").document(storyId).get().await()
                .toObject(Story::class.java)
            if (story?.userId != myUid) {
                return Result.failure(Exception("No tienes permiso para eliminar esta historia"))
            }
            db.collection("stories").document(storyId).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMyStories(): Result<List<Story>> {
        return try {
            val myUid = auth.currentUser?.uid ?: return Result.failure(Exception("No autenticado"))
            val now = Date()
            val snap = db.collection("stories")
                .whereEqualTo("userId", myUid)
                .whereGreaterThan("expiresAt", now)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            Result.success(snap.toObjects(Story::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getStoriesByUser(userId: String): Result<List<Story>> {
        return try {
            val now = Date()
            val snap = db.collection("stories")
                .whereEqualTo("userId", userId)
                .whereGreaterThan("expiresAt", now)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            Result.success(snap.toObjects(Story::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFriendsStories(friendIds: List<String>): Result<List<Story>> {
        return try {
            if (friendIds.isEmpty()) return Result.success(emptyList())

            val now = Date()
            val snap = db.collection("stories")
                .whereIn("userId", friendIds.take(10))
                .whereGreaterThan("expiresAt", now)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            Result.success(snap.toObjects(Story::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCommunityStories(limit: Long): Result<List<Story>> {
        return try {
            val now = Date()
            val snap = db.collection("stories")
                .whereGreaterThan("expiresAt", now)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get().await()
            Result.success(snap.toObjects(Story::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAsViewed(storyId: String, viewerId: String): Result<Boolean> {
        return try {
            db.collection("stories").document(storyId)
                .update("viewers", FieldValue.arrayUnion(viewerId))
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cleanupExpiredStories(): Result<Int> {
        return try {
            val now = Date()
            val snap = db.collection("stories")
                .whereLessThan("expiresAt", now)
                .limit(100)
                .get().await()

            val batch = db.batch()
            snap.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
            Result.success(snap.size())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
