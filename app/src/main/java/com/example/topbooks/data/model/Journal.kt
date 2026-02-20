package com.example.topbooks.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Journal(
    val bookId: String = "",
    val userId: String = "",
    val bookTitle: String = "",
    val bookImageUrl: String = "",
    val title: String = "",
    val author: String = "",
    val pages: String = "",
    val isPublic: Boolean = false,
    val mainRating: Int = 0,
    val rRomance: Int = 0,
    val rHappy: Int = 0,
    val rSad: Int = 0,
    val rSpicy: Int = 0,
    val genre: String = "",
    val playlist: String = "",
    val format: String = "",
    val characters: String = "",
    val nicknames: String = "",
    val quotes: String = "",
    val moments: String = "",
    val startDate: String = "",
    val endDate: String = "",
    @ServerTimestamp val createdAt: Date? = null
)