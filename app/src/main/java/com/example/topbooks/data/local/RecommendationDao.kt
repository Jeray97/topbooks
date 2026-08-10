package com.example.topbooks.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RecommendationDao {
    @Query("SELECT * FROM recommendations WHERE userId = :userId AND expiresAt > :currentTime")
    suspend fun getValidRecommendations(userId: String, currentTime: Long = System.currentTimeMillis()): RecommendationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecommendations(recommendation: RecommendationEntity)

    @Query("DELETE FROM recommendations WHERE userId = :userId")
    suspend fun deleteRecommendations(userId: String)

    @Query("DELETE FROM recommendations WHERE expiresAt < :olderThan")
    suspend fun deleteExpiredRecommendations(olderThan: Long = System.currentTimeMillis())
}
