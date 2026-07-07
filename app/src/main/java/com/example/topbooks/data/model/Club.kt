package com.example.topbooks.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class ClubFrequency {
    WEEKLY,
    BIWEEKLY,
    MONTHLY
}

data class ClubMember(
    val userId: String = "",
    val joinedAt: Long = System.currentTimeMillis(),
    val currentProgress: Int = 0,
    var userName: String = "",
    var userPhotoUrl: String = "capibara_1"
)

data class Club(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val createdBy: String = "",
    val coverImageUrl: String = "",
    val currentBookId: String = "",
    val currentBookTitle: String = "",
    val currentBookAuthor: String = "",
    val currentBookImageUrl: String = "",
    val frequency: String = ClubFrequency.MONTHLY.name,
    val memberIds: List<String> = emptyList(),
    val memberCount: Int = 0,
    val isPublic: Boolean = true,
    val genres: List<String> = emptyList(),
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val currentBookStartDate: Date? = null,
    @ServerTimestamp
    val currentBookEndDate: Date? = null,
    var creatorName: String = "",
    var creatorPhotoUrl: String = "capibara_1"
)
