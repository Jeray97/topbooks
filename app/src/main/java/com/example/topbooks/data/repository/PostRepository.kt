package com.example.topbooks.data.repository

import com.example.topbooks.data.model.Post
import com.example.topbooks.data.model.PostReply
import com.example.topbooks.data.model.PostType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

interface PostRepository {
    suspend fun createPost(post: Post): Result<String>
    suspend fun deletePost(postId: String): Result<Boolean>
    suspend fun getPostById(postId: String): Result<Post>
    suspend fun getCommunityFeed(limit: Long = 20, lastPostId: String? = null): Result<List<Post>>
    suspend fun getFriendsFeed(friendIds: List<String>, limit: Long = 20): Result<List<Post>>
    suspend fun getTopFeed(limit: Long = 20): Result<List<Post>>
    suspend fun getAlgorithmicFeed(userId: String, friendIds: List<String>, favoriteGenres: List<String>, limit: Long = 30): Result<List<Post>>
    suspend fun getUserPosts(userId: String): Result<List<Post>>
    suspend fun toggleLike(postId: String, userId: String): Result<Boolean>
    suspend fun toggleSave(postId: String, userId: String): Result<Boolean>
    suspend fun toggleReaction(postId: String, emoji: String, userId: String): Result<Boolean>
    suspend fun addReply(postId: String, reply: PostReply): Result<Boolean>
    suspend fun deleteReply(postId: String, replyId: String): Result<Boolean>
    suspend fun toggleReplyLike(postId: String, replyId: String, userId: String): Result<Boolean>
}

class PostRepositoryImpl : PostRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override suspend fun createPost(post: Post): Result<String> {
        return try {
            val docRef = db.collection("posts").document()
            val newPost = post.copy(id = docRef.id, userId = auth.currentUser?.uid ?: "")
            docRef.set(newPost).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePost(postId: String): Result<Boolean> {
        return try {
            val myUid = auth.currentUser?.uid ?: return Result.failure(Exception("No autenticado"))
            val post = db.collection("posts").document(postId).get().await()
                .toObject(Post::class.java)
            if (post?.userId != myUid) {
                return Result.failure(Exception("No tienes permiso para eliminar este post"))
            }
            db.collection("posts").document(postId).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPostById(postId: String): Result<Post> {
        return try {
            val snap = db.collection("posts").document(postId).get().await()
            val post = snap.toObject(Post::class.java)
            if (post != null) Result.success(post)
            else Result.failure(Exception("Post no encontrado"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCommunityFeed(limit: Long, lastPostId: String?): Result<List<Post>> {
        return try {
            var query = db.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)

            if (lastPostId != null) {
                val lastDoc = db.collection("posts").document(lastPostId).get().await()
                query = query.startAfter(lastDoc)
            }

            val snap = query.get().await()
            Result.success(snap.toObjects(Post::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFriendsFeed(friendIds: List<String>, limit: Long): Result<List<Post>> {
        return try {
            if (friendIds.isEmpty()) return Result.success(emptyList())

            val snap = db.collection("posts")
                .whereIn("userId", friendIds.take(10))
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get().await()
            Result.success(snap.toObjects(Post::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTopFeed(limit: Long): Result<List<Post>> {
        return try {
            val snap = db.collection("posts")
                .orderBy("likes", Query.Direction.DESCENDING)
                .limit(limit)
                .get().await()
            Result.success(snap.toObjects(Post::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAlgorithmicFeed(
        userId: String,
        friendIds: List<String>,
        favoriteGenres: List<String>,
        limit: Long
    ): Result<List<Post>> {
        return try {
            val allPosts = mutableListOf<Post>()
            val seenIds = mutableSetOf<String>()

            if (friendIds.isNotEmpty()) {
                val friendsSnap = db.collection("posts")
                    .whereIn("userId", friendIds.take(10))
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(15)
                    .get().await()
                friendsSnap.toObjects(Post::class.java).forEach { post ->
                    if (seenIds.add(post.id)) allPosts.add(post)
                }
            }

            val recentSnap = db.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(40)
                .get().await()
            recentSnap.toObjects(Post::class.java).forEach { post ->
                if (seenIds.add(post.id)) allPosts.add(post)
            }

            val now = System.currentTimeMillis()
            val scoredPosts = allPosts.map { post ->
                var score = 0.0

                if (post.userId in friendIds) score += 50.0

                val postAge = now - (post.createdAt?.time ?: now)
                val hoursAgo = postAge / (1000.0 * 60 * 60)
                score += maxOf(0.0, 30.0 - hoursAgo)

                val engagement = post.likes + (post.replyCount * 2) + (post.reactions.values.sumOf { it.size } * 1.5)
                score += engagement * 2.0

                if (post.savedBy.contains(userId)) score += 20.0

                post to score
            }

            val sortedPosts = scoredPosts
                .sortedByDescending { it.second }
                .take(limit.toInt())
                .map { it.first }

            Result.success(sortedPosts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserPosts(userId: String): Result<List<Post>> {
        return try {
            val snap = db.collection("posts")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            Result.success(snap.toObjects(Post::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleLike(postId: String, userId: String): Result<Boolean> {
        return try {
            val ref = db.collection("posts").document(postId)
            val snap = ref.get().await()
            val post = snap.toObject(Post::class.java) ?: return Result.failure(Exception("Post no encontrado"))

            val isLiked = userId in post.likedBy
            if (isLiked) {
                ref.update(
                    "likedBy", FieldValue.arrayRemove(userId),
                    "likes", FieldValue.increment(-1)
                ).await()
            } else {
                ref.update(
                    "likedBy", FieldValue.arrayUnion(userId),
                    "likes", FieldValue.increment(1)
                ).await()
            }
            Result.success(!isLiked)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleSave(postId: String, userId: String): Result<Boolean> {
        return try {
            val ref = db.collection("posts").document(postId)
            val snap = ref.get().await()
            val post = snap.toObject(Post::class.java) ?: return Result.failure(Exception("Post no encontrado"))

            val isSaved = userId in post.savedBy
            if (isSaved) {
                ref.update("savedBy", FieldValue.arrayRemove(userId)).await()
            } else {
                ref.update("savedBy", FieldValue.arrayUnion(userId)).await()
            }
            Result.success(!isSaved)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleReaction(postId: String, emoji: String, userId: String): Result<Boolean> {
        return try {
            val ref = db.collection("posts").document(postId)
            val snap = ref.get().await()
            val post = snap.toObject(Post::class.java) ?: return Result.failure(Exception("Post no encontrado"))

            val currentReactions = post.reactions.toMutableMap()
            val usersForEmoji = currentReactions[emoji]?.toMutableList() ?: mutableListOf()

            val hasReacted = userId in usersForEmoji
            if (hasReacted) {
                usersForEmoji.remove(userId)
                if (usersForEmoji.isEmpty()) {
                    currentReactions.remove(emoji)
                } else {
                    currentReactions[emoji] = usersForEmoji
                }
            } else {
                usersForEmoji.add(userId)
                currentReactions[emoji] = usersForEmoji
            }

            ref.update("reactions", currentReactions).await()
            Result.success(!hasReacted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addReply(postId: String, reply: PostReply): Result<Boolean> {
        return try {
            val docRef = db.collection("posts").document(postId)
            val newReply = reply.copy(id = docRef.collection("replies").document().id)
            docRef.update(
                "replies", FieldValue.arrayUnion(newReply),
                "replyCount", FieldValue.increment(1)
            ).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteReply(postId: String, replyId: String): Result<Boolean> {
        return try {
            val myUid = auth.currentUser?.uid ?: return Result.failure(Exception("No autenticado"))
            val ref = db.collection("posts").document(postId)
            val snap = ref.get().await()
            val post = snap.toObject(Post::class.java) ?: return Result.failure(Exception("Post no encontrado"))

            val reply = post.replies.find { it.id == replyId }
            if (reply == null) return Result.failure(Exception("Respuesta no encontrada"))
            if (reply.userId != myUid) return Result.failure(Exception("No tienes permiso"))

            ref.update(
                "replies", FieldValue.arrayRemove(reply),
                "replyCount", FieldValue.increment(-1)
            ).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleReplyLike(postId: String, replyId: String, userId: String): Result<Boolean> {
        return try {
            val ref = db.collection("posts").document(postId)
            val snap = ref.get().await()
            val post = snap.toObject(Post::class.java) ?: return Result.failure(Exception("Post no encontrado"))

            val reply = post.replies.find { it.id == replyId }
                ?: return Result.failure(Exception("Respuesta no encontrada"))

            val isLiked = userId in reply.likedBy
            val updatedReplies = post.replies.map {
                if (it.id == replyId) {
                    val newLikedBy = if (isLiked) {
                        it.likedBy - userId
                    } else {
                        it.likedBy + userId
                    }
                    it.copy(
                        likes = if (isLiked) it.likes - 1 else it.likes + 1,
                        likedBy = newLikedBy
                    )
                } else it
            }
            ref.update("replies", updatedReplies).await()
            Result.success(!isLiked)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
