package com.example.topbooks.data.model

data class ShelfBookMeta(
    val title: String = "",
    val imageUrl: String = "",
    val pageCount: Int = 0,
    val authors: List<String> = emptyList()
)

data class Shelf(
    val id: String = "",
    val name: String = "",
    val color: Long = 0xFF8D5B4C,
    val bookIds: List<String> = emptyList(),
    val bookMetadata: Map<String, ShelfBookMeta> = emptyMap(),
    val order: Int = 0,
    val isPublic: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
