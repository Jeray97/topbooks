package com.example.topbooks.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM posts WHERE id = :id")
    suspend fun getPostById(id: String): PostEntity?

    @Query("SELECT * FROM posts WHERE id = :id")
    fun observePostById(id: String): Flow<PostEntity?>

    @Query("SELECT * FROM posts ORDER BY createdAtMillis DESC LIMIT :limit")
    suspend fun getRecentPosts(limit: Int = 30): List<PostEntity>

    @Query("SELECT * FROM posts ORDER BY createdAtMillis DESC LIMIT :limit")
    fun observeRecentPosts(limit: Int = 30): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE userId IN (:userIds) ORDER BY createdAtMillis DESC LIMIT :limit")
    suspend fun getPostsByUsers(userIds: List<String>, limit: Int = 30): List<PostEntity>

    @Query("SELECT * FROM posts WHERE userId = :userId ORDER BY createdAtMillis DESC")
    suspend fun getPostsByUser(userId: String): List<PostEntity>

    @Query("SELECT * FROM posts ORDER BY likes DESC LIMIT :limit")
    suspend fun getTopPosts(limit: Int = 20): List<PostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Query("DELETE FROM posts WHERE id = :id")
    suspend fun deletePost(id: String)

    @Query("DELETE FROM posts WHERE cachedAt < :olderThan")
    suspend fun deleteOldPosts(olderThan: Long)

    @Query("DELETE FROM posts")
    suspend fun clearAll()
}
