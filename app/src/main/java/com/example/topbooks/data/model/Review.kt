package com.example.topbooks.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Review(
    val id: String = "",
    val bookId: String = "",
    val userId: String = "",
    val rating: Int = 0,
    val text: String = "",
    val likes: Int = 0,


    @ServerTimestamp
    val createAt: Date? = null,

    var userName: String = "Usuario",
    var userPhotoUrl: String = ""
)