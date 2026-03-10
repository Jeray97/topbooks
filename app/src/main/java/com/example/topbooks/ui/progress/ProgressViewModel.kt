package com.example.topbooks.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.repository.*
import com.example.topbooks.ui.profile.SimpleBook
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Representa el estado de la pantalla de progreso del usuario.
 * @property journals Lista de libros que tienen una entrada en el diario de lectura.
 * @property favorites Lista de libros marcados como favoritos con sus portadas.
 * @property pending Lista de libros en la lista de deseos o pendientes de leer.
 * @property read Lista de libros que el usuario ya ha finalizado.
 * @property isLoading Indica si se está realizando una operación de carga en red.
 */
data class ProgressState(
    val journals: List<SimpleBook> = emptyList(),
    val favorites: List<SimpleBook> = emptyList(),
    val pending: List<SimpleBook> = emptyList(),
    val read: List<SimpleBook> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * ViewModel que gestiona la lógica de la biblioteca personal y el seguimiento de lectura.
 * Actúa como orquestador central que unifica datos de progreso, perfiles de usuario,
 * detalles de la API de libros y diarios personales.
 */
class ProgressViewModel(
    private val progressRepo: ProgressRepository = ProgressRepositoryImpl(),
    private val userRepo: UserRepository = UserRepositoryImpl(),
    private val booksRepo: BooksRepository = BooksRepository(),
    // 1. AÑADIMOS EL REPOSITORIO DE DIARIOS
    private val journalRepo: JournalRepository = JournalRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressState())
    val uiState: StateFlow<ProgressState> = _uiState.asStateFlow()

    init {
        loadProgressData()
    }

    /**
     * Carga de forma masiva y paralela todos los datos de progreso del usuario.
     * Utiliza bloques 'async' para realizar múltiples consultas a Firebase y la API
     * simultáneamente, optimizando el rendimiento.
     */
    fun loadProgressData() {
        val uid = userRepo.getCurrentUserId() ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Lanzamiento de consultas asíncronas paralelas
            val readDeferred = async { progressRepo.getReadBooks(uid).getOrDefault(emptyList()) }
            val bookmarksDeferred = async { progressRepo.getBookmarks(uid).getOrDefault(emptyList()) }
            val favCoversDeferred = async { userRepo.getFavoriteCovers(uid, 50).getOrDefault(emptyList()) }
            val favIdsDeferred = async { userRepo.getFavoriteIds(uid).getOrDefault(emptySet()).toList() }

            // 2. PEDIMOS LOS DIARIOS DE FORMA ASÍNCRONA
            val journalsDeferred = async { journalRepo.getAllJournals(uid).getOrDefault(emptyList()) }

            // Espera y procesamiento de resultados de red
            val readBooks = readDeferred.await()
            val pendingBooks = bookmarksDeferred.await().map { SimpleBook(it.bookId, it.bookTitle, "") }

            val favIds = favIdsDeferred.await()
            val favCovers = favCoversDeferred.await()
            val favoriteBooks = favIds.zip(favCovers).map { SimpleBook(it.first, imageUrl = it.second) }

            // 3. RECIBIMOS LOS DIARIOS Y LOS CONVERTIMOS A SimpleBook
            val myJournals = journalsDeferred.await().map {
                SimpleBook(id = it.bookId, title = it.bookTitle, imageUrl = it.bookImageUrl)
            }

            // PATRÓN DE ENRIQUECIMIENTO (HIDRATACIÓN):
            // Obtenemos los metadatos completos (portada/título) de la API para las listas que solo tienen IDs.
            val enrichedPending = enrichWithGlobalBooks(pendingBooks)
            val enrichedRead = enrichWithGlobalBooks(readBooks)
            val enrichedJournals = enrichWithGlobalBooks(myJournals) // Buscamos el título/portada en la API

            // Actualización final del estado unificado
            _uiState.update {
                it.copy(
                    journals = enrichedJournals, // 4. LO AÑADIMOS AL ESTADO
                    read = enrichedRead,
                    pending = enrichedPending,
                    favorites = favoriteBooks,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Función auxiliar que recorre una lista de libros simplificados y completa su información
     * consultando la API de Google Books.
     * * @param list Lista de libros que requieren completar sus datos visuales.
     * @return Nueva lista de libros con títulos y portadas actualizados desde la API.
     */
    private suspend fun enrichWithGlobalBooks(list: List<SimpleBook>): List<SimpleBook> {
        return list.map { book ->
            viewModelScope.async {
                // OPTIMIZACIÓN: Evitamos hacer peticiones a la API para los diarios con IDs manuales
                // (Los IDs de la API suelen ser cortos, los de Firestore son largos)
                if (book.id.length > 20) return@async book

                val apiBook = booksRepo.getBookDetail(book.id).getOrNull()
                book.copy(
                    title = apiBook?.title ?: book.title,
                    imageUrl = apiBook?.imageUrl ?: book.imageUrl
                )
            }.await()
        }
    }
}