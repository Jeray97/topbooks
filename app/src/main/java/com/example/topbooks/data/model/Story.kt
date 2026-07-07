package com.example.topbooks.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class StoryType {
    BOOK_COVER,
    QUOTE,
    READING_STATUS
}

data class Story(
    val id: String = "",
    val userId: String = "",
    val type: String = StoryType.BOOK_COVER.name,
    val bookId: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val backgroundColor: String = "",
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val expiresAt: Date? = null,
    val viewers: List<String> = emptyList(),
    var userName: String = "",
    var userPhotoUrl: String = "capibara_1",
    var bookTitle: String = "",
    var bookAuthor: String = "",
    var bookImageUrl: String = ""
)
