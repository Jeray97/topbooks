package com.example.topbooks.data.model

data class Book(
    val id: String,
    val title: String,
    val authors: List<String>,
    val description: String,
    val imageUrl: String,
    val lanzamiento: String
)