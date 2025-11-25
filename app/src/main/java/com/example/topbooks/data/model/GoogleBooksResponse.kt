package com.example.topbooks.data.model

import com.google.gson.annotations.SerializedName

// 1. La respuesta fea de la API GoogleBooks (da miedo)
data class GoogleBooksResponse (
    @SerializedName("items") val items: List<BookItem>?
)

//2. Cada item de la lista cruda
data class BookItem (
    @SerializedName("id") val id: String,
    @SerializedName("volumenInfo") val volumeInfo: VolumeInfo
) {
    //Función para pasar a limpio
    fun toDomain(): Book {
        return Book(
            id = id,
            title = volumeInfo.title ?: "Sín título",
            authors = volumeInfo.authors ?: emptyList(),
            description = volumeInfo.description ?: "",
            //Me recomiendan cambiar https por http para que Android no bloquee imagenes
            imageUrl = volumeInfo.imageLinks?.thumbnail ?.replace("http", "https") ?: ""
        )
    }
}

data class VolumeInfo(
    @SerializedName("title") val title: String?,
    @SerializedName("authors") val authors: List<String>?,
    @SerializedName("description") val description: String?,
    @SerializedName("imageLinks") val imageLinks: ImageLinks?
)

data class ImageLinks(
    @SerializedName("thumbnail") val thumbnail: String?
)