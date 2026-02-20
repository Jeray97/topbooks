package com.example.topbooks.data.model

data class Book(
    val id: String,
    val title: String,
    val authors: List<String>,
    val description: String,
    val imageUrl: String,
    val lanzamiento: String,
    val averageRating: Double = 0.0,
    val ratingsCount: Int = 0,
    val pageCount: Int = 0
)