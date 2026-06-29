package com.example.topbooks.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE uid = :uid")
    suspend fun getUserById(uid: String): UserEntity?

    @Query("SELECT * FROM users WHERE uid = :uid")
    fun observeUserById(uid: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE displayNameLowercase LIKE '%' || :query || '%' LIMIT :limit")
    suspend fun searchUsers(query: String, limit: Int = 20): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("DELETE FROM users WHERE uid = :uid")
    suspend fun deleteUser(uid: String)

    @Query("DELETE FROM users WHERE cachedAt < :olderThan")
    suspend fun deleteOldUsers(olderThan: Long)

    @Query("DELETE FROM users")
    suspend fun clearAll()
}
