package com.example.topbooks.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class PostType {
    REVIEW,
    QUOTE,
    FINISHED,
    READING
}

data class PostReply(
    val id: String = "",
    val userId: String = "",
    val text: String = "",
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    @ServerTimestamp
    val createdAt: Date? = null,
    var userName: String = "",
    var userPhotoUrl: String = "capibara_1"
)

data class Post(
    val id: String = "",
    val userId: String = "",
    val type: String = PostType.REVIEW.name,
    val bookId: String = "",
    val text: String = "",
    val rating: Int = 0,
    val quote: String = "",
    val chapter: String = "",
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val savedBy: List<String> = emptyList(),
    val reactions: Map<String, List<String>> = emptyMap(),
    val replies: List<PostReply> = emptyList(),
    val replyCount: Int = 0,
    @ServerTimestamp
    val createdAt: Date? = null,
    var userName: String = "",
    var userPhotoUrl: String = "capibara_1",
    var bookTitle: String = "",
    var bookAuthor: String = "",
    var bookImageUrl: String = ""
)
