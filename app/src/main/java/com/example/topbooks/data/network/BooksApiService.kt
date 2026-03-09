package com.example.topbooks.data.network

import com.example.topbooks.data.model.BookItem
import com.example.topbooks.data.model.GoogleBooksResponse
import com.example.topbooks.data.model.OpenLibrarySearchResponse
import com.example.topbooks.data.model.OpenLibraryWorkDetail
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Interfaz de Retrofit que define las llamadas a las APIs externas de libros.
 * * Actúa como un contrato donde declaramos qué rutas (endpoints) vamos a usar,
 * qué parámetros requieren y qué tipo de datos nos van a devolver.
 */
interface BooksApiService {

    /**
     * Realiza una búsqueda general de libros en Google Books.
     * * Al usar 'suspend', esta función se ejecutará de forma asíncrona mediante Corrutinas.
     * * @param query Texto de búsqueda (ej. título, autor o género).
     * @param apiKey Clave de acceso a la API de Google.
     * @param startIndex Índice de paginación (por defecto 0).
     * @param orderBy Criterio de ordenación (por defecto "relevance" / relevancia).
     * @param maxResults Número máximo de resultados (por defecto 40).
     * @param lang Restricción de idioma (por defecto "es" para traer resultados en español).
     * @param printType Tipo de impreso (por defecto "books" para excluir revistas).
     * @return Una respuesta HTTP que contiene un objeto [GoogleBooksResponse].
     */
    @GET("https://www.googleapis.com/books/v1/volumes")
    suspend fun searchBooksGoogle(
        @Query("q") query: String,
        @Query("key") apiKey: String,
        @Query("startIndex") startIndex: Int = 0,
        @Query("orderBy") orderBy: String = "relevance",
        @Query("maxResults") maxResults: Int = 40,
        @Query("langRestrict") lang: String = "es",
        @Query("printType") printType: String = "books"
    ): Response<GoogleBooksResponse>

    /**
     * Obtiene los detalles completos de un libro específico en Google Books usando su ID.
     * * @param id Identificador único del volumen en Google Books. Sustituye la variable {id} en la URL.
     * @param apiKey Clave de acceso a la API.
     * @return Una respuesta HTTP que contiene un objeto [BookItem].
     */
    @GET("https://www.googleapis.com/books/v1/volumes/{id}")
    suspend fun getBookDetailGoogle(
        @Path("id") id: String,
        @Query("key") apiKey: String
    ): Response<BookItem>

    // ||||                     --- OPEN LIBRARY ---                    ||||

    /**
     * Realiza una búsqueda general de libros en Open Library.
     * * @param query Texto de búsqueda.
     * @param sort Parámetro de ordenación (opcional).
     * @param limit Límite de resultados a devolver (por defecto 20).
     * @return Una respuesta HTTP que contiene un objeto [OpenLibrarySearchResponse].
     */
    @GET("https://openlibrary.org/search.json")
    suspend fun searchBooksOpenLibrary(
        @Query("q") query: String,
        @Query("sort") sort: String? = null,
        @Query("limit") limit: Int = 20
    ): Response<OpenLibrarySearchResponse>


    /**
     * Obtiene la información detallada de una obra (Work) en Open Library.
     * * Esta llamada es fundamental para obtener la sinopsis completa, ya que la búsqueda general
     * no suele incluirla.
     * * @param id Identificador de la obra (se debe pasar sin el prefijo "/works/").
     * @return Una respuesta HTTP que contiene un objeto [OpenLibraryWorkDetail].
     */
    @GET("https://openlibrary.org/works/{id}.json")
    suspend fun getWorkDetailOpenLibrary(
        @Path("id") id: String
    ): Response<OpenLibraryWorkDetail>

    /**
     * Permite realizar una petición GET a una URL externa y completamente dinámica.
     * * Es de gran utilidad para la paginación de Open Library o para navegar por enlaces
     * específicos de autores donde la API devuelve la URL completa a consultar.
     * * @param url La dirección web (endpoint) completa a la que hacer la petición.
     * @return Una respuesta HTTP con los resultados mapeados en [OpenLibrarySearchResponse].
     * PD: NO SE USA ACTUALMENTE PERO ES UTIL PARA LA ESCALABILIDAD DE LA APP
     */
    @GET
    suspend fun searchAuthorExternal(@Url url: String): Response<OpenLibrarySearchResponse>
}