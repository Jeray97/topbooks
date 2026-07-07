package com.example.topbooks.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,
    val displayName: String = "",
    val displayNameLowercase: String = "",
    val email: String = "",
    val photoURL: String = "capibara_1",
    val role: String? = "user",
    val bio: String = "",
    val isTutorialCompleted: Boolean = false,
    val favoriteGenres: List<String> = emptyList(),
    val favoriteBooks: List<String> = emptyList(),
    val preferences: Map<String, Boolean> = emptyMap(),
    val lastLoginMillis: Long = System.currentTimeMillis(),
    val reviewsCount: Int = 0,
    val bookmarksCount: Int = 0,
    val commentsCount: Int = 0,
    val friendsCount: Int = 0,
    val booksCompleted: Int = 0,
    val fcmToken: String = "",
    val createdAtMillis: Long? = null,
    val cachedAt: Long = System.currentTimeMillis()
)
