package com.example.topbooks.data.model

data class Book(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val authors: List<String> = emptyList(),
    val description: String = "",
    val imageUrl: String = "",
    val lanzamiento: String = "",
    val averageRating: Double = 0.0,
    val ratingsCount: Int = 0,
    val pageCount: Int = 0,
    val isMature: Boolean = false,
    val categories: List<String> = emptyList(),

    // NUEVO
    val seriesName: String = "",
    val seriesIndex: Int = 0
) {

    val isSaga: Boolean
        get() = seriesName.isNotBlank()

}