package com.example.topbooks.ui.progress

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.ReadingGoal
import com.example.topbooks.data.repository.*
import com.example.topbooks.ui.profile.SimpleBook
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Representa el estado de la pantalla de progreso del usuario.
 * @property journals Lista de libros que tienen una entrada en el diario de lectura.
 * @property favorites Lista de libros marcados como favoritos con sus portadas.
 * @property pending Lista de libros en la lista de deseos o pendientes de leer.
 * @property read Lista de libros que el usuario ya ha finalizado.
 * @property readingGoal Objetivo de lectura anual del usuario.
 * @property isLoading Indica si se está realizando una operación de carga en red.
 */
data class ProgressState(
    val journals: List<SimpleBook> = emptyList(),
    val favorites: List<SimpleBook> = emptyList(),
    val pending: List<SimpleBook> = emptyList(),
    val read: List<SimpleBook> = emptyList(),
    val readingGoal: ReadingGoal = ReadingGoal(),
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
    private val journalRepo: JournalRepository = JournalRepositoryImpl(),
    private val context: Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressState())
    val uiState: StateFlow<ProgressState> = _uiState.asStateFlow()

    companion object {
        private const val PREFS_NAME = "reading_goal_prefs"
        private const val KEY_TARGET_BOOKS = "target_books"
        private const val KEY_GOAL_YEAR = "goal_year"
    }

    init {
        loadProgressData()
        loadReadingGoal()
    }

    /**
     * Carga el objetivo de lectura desde SharedPreferences.
     */
    private fun loadReadingGoal() {
        val uid = userRepo.getCurrentUserId() ?: return
        val prefs = context?.getSharedPreferences("${PREFS_NAME}_$uid", Context.MODE_PRIVATE)
        
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val savedYear = prefs?.getInt(KEY_GOAL_YEAR, 0) ?: 0
        val targetBooks = prefs?.getInt(KEY_TARGET_BOOKS, 0) ?: 0
        
        // Si el año guardado no es el actual, reseteamos el objetivo
        if (savedYear != currentYear) {
            _uiState.update { it.copy(readingGoal = ReadingGoal(year = currentYear, targetBooks = 0, booksRead = 0)) }
        } else {
            // Calculamos libros leídos este año
            val booksReadThisYear = calculateBooksReadThisYear()
            _uiState.update { 
                it.copy(readingGoal = ReadingGoal(year = currentYear, targetBooks = targetBooks, booksRead = booksReadThisYear)) 
            }
        }
    }

    /**
     * Calcula cuántos libros ha leído el usuario este año.
     */
    private fun calculateBooksReadThisYear(): Int {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val readBooks = _uiState.value.read
        
        // Por ahora contamos todos los libros leídos
        // En el futuro podríamos filtrar por fecha de lectura si guardamos esa información
        return readBooks.size
    }

    /**
     * Guarda o actualiza el objetivo de lectura anual.
     * @param targetBooks Número de libros que el usuario quiere leer este año.
     */
    fun saveReadingGoal(targetBooks: Int) {
        val uid = userRepo.getCurrentUserId() ?: return
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        
        // Guardar en SharedPreferences
        val prefs = context?.getSharedPreferences("${PREFS_NAME}_$uid", Context.MODE_PRIVATE)
        prefs?.edit()?.apply {
            putInt(KEY_TARGET_BOOKS, targetBooks)
            putInt(KEY_GOAL_YEAR, currentYear)
            apply()
        }
        
        // Actualizar el estado
        val booksRead = calculateBooksReadThisYear()
        _uiState.update { 
            it.copy(readingGoal = ReadingGoal(year = currentYear, targetBooks = targetBooks, booksRead = booksRead)) 
        }
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

            val journalsDeferred = async { journalRepo.getAllJournals(uid).getOrDefault(emptyList()) }

            // Espera y procesamiento de resultados de red
            val readBooks = readDeferred.await()
            val pendingBooks = bookmarksDeferred.await().map { SimpleBook(it.bookId, it.bookTitle, "") }

            val favIds = favIdsDeferred.await()
            val favCovers = favCoversDeferred.await()
            val favoriteBooks = favIds.zip(favCovers).map { SimpleBook(it.first, imageUrl = it.second) }

            val myJournals = journalsDeferred.await().map {
                SimpleBook(id = it.bookId, title = it.bookTitle, imageUrl = it.bookImageUrl)
            }

            // PATRÓN DE ENRIQUECIMIENTO (HIDRATACIÓN):
            val enrichedPending = enrichWithGlobalBooks(pendingBooks)
            val enrichedRead = enrichWithGlobalBooks(readBooks)
            val enrichedJournals = enrichWithGlobalBooks(myJournals)

            // Actualización final del estado unificado
            _uiState.update {
                it.copy(
                    journals = enrichedJournals,
                    read = enrichedRead,
                    pending = enrichedPending,
                    favorites = favoriteBooks,
                    isLoading = false
                )
            }
            
            // Actualizar el progreso del objetivo después de cargar los datos
            loadReadingGoal()
        }
    }

    /**
     * Función auxiliar que recorre una lista de libros simplificados y completa su información
     * consultando la API de Google Books.
     */
    private suspend fun enrichWithGlobalBooks(list: List<SimpleBook>): List<SimpleBook> {
        return list.map { book ->
            viewModelScope.async {
                if (book.id.length > 20) return@async book

                val apiBook = booksRepo.getBookDetail(book.id).getOrNull()
                book.copy(
                    title = apiBook?.title ?: book.title,
                    imageUrl = apiBook?.imageUrl ?: book.imageUrl
                )
            }.await()
        }
    }

    /**
     * Factory para crear el ViewModel con contexto.
     */
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ProgressViewModel(
                progressRepo = ProgressRepositoryImpl(),
                userRepo = UserRepositoryImpl(),
                booksRepo = BooksRepository(),
                journalRepo = JournalRepositoryImpl(),
                context = context
            ) as T
        }
    }
}