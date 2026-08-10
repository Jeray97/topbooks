package com.example.topbooks.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recommendations")
data class RecommendationEntity(
    @PrimaryKey
    val userId: String,
    val bookIds: List<String>,
    val cachedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000) // 7 días
)
