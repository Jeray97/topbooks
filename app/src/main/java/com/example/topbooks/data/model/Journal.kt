package com.example.topbooks.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Journal(
    var bookId: String = "",
    var userId: String = "",
    var bookTitle: String = "",
    var bookImageUrl: String = "",
    var title: String = "",
    var author: String = "",
    var pages: String = "",

    @get:PropertyName("isPublic")
    @set:PropertyName("isPublic")
    var isPublic: Boolean = false,

    var mainRating: Int = 0,

    @get:PropertyName("rRomance")
    @set:PropertyName("rRomance")
    var rRomance: Int = 0,

    @get:PropertyName("rHappy")
    @set:PropertyName("rHappy")
    var rHappy: Int = 0,

    @get:PropertyName("rSad")
    @set:PropertyName("rSad")
    var rSad: Int = 0,

    @get:PropertyName("rSpicy")
    @set:PropertyName("rSpicy")
    var rSpicy: Int = 0,

    var genre: String = "",
    var playlist: String = "",
    var format: String = "",
    var characters: String = "",
    var nicknames: String = "",
    var quotes: String = "",
    var moments: String = "",
    var startDate: String = "",
    var endDate: String = "",

    @ServerTimestamp
    var createdAt: Date? = null
)