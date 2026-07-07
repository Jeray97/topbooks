package com.example.topbooks.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class DiscussionMessage(
    val id: String = "",
    val userId: String = "",
    val text: String = "",
    val likes: Int = 0,
    @ServerTimestamp
    val createdAt: Date? = null,
    var userName: String = "",
    var userPhotoUrl: String = "capibara_1"
)

data class Discussion(
    val id: String = "",
    val clubId: String = "",
    val chapter: String = "",
    val title: String = "",
    val createdBy: String = "",
    val messages: List<DiscussionMessage> = emptyList(),
    val messageCount: Int = 0,
    val isSpoiler: Boolean = false,
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val lastMessageAt: Date? = null,
    var creatorName: String = "",
    var creatorPhotoUrl: String = "capibara_1"
)
