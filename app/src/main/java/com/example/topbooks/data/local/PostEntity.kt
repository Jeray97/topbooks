package com.example.topbooks.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val userId: String = "",
    val type: String = "REVIEW",
    val bookId: String = "",
    val text: String = "",
    val rating: Int = 0,
    val quote: String = "",
    val chapter: String = "",
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val savedBy: List<String> = emptyList(),
    val reactions: Map<String, List<String>> = emptyMap(),
    val replyCount: Int = 0,
    val createdAtMillis: Long = 0L,
    val userName: String = "",
    val userPhotoUrl: String = "capibara_1",
    val bookTitle: String = "",
    val bookAuthor: String = "",
    val bookImageUrl: String = "",
    val cachedAt: Long = System.currentTimeMillis()
)
