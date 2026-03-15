package com.example.topbooks.ui.book

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Book
import com.example.topbooks.data.model.Review
import com.example.topbooks.data.repository.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 1. DEFINIMOS EL ESTADO DE LA UI
 * * Agrupa toda la información necesaria para pintar la pantalla de [BookDetailScreen].
 *
 * @property book Los datos completos del libro (título, autor, sinopsis, etc).
 * @property isLoading Indica si se está descargando información de internet.
 * @property error Mensaje de error en caso de que falle la carga.
 * @property isFavorite Indica si el usuario actual ha marcado este libro con el corazón (Favoritos).
 * @property savedInList Indica en qué lista mutuamente excluyente está guardado el libro ("Leídos" o "Pendientes"). Si no está en ninguna, es null.
 * @property reviews Lista de reseñas públicas de la comunidad sobre este libro.
 */
data class BookDetailState(
    val book: Book? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFavorite: Boolean = false,
    val savedInList: String? = null,
    val reviews: List<Review> = emptyList()
)

/**
 * ViewModel que gestiona la lógica de la pantalla de Detalles de un Libro.
 * * Conecta la vista con múltiples repositorios para centralizar todas las posibles
 * interacciones del usuario con un libro específico.
 */
class BookDetailViewModel(
    private val booksRepository: BooksRepository = BooksRepository(),
    private val progressRepository: ProgressRepository = ProgressRepositoryImpl(),
    private val userRepository: UserRepository = UserRepositoryImpl(),
    private val authRepository: AuthRepository = AuthRepositoryImpl()
) : ViewModel() {

    // 2. INICIALIZAMOS EL STATEFLOW
    private val _uiState = MutableStateFlow(BookDetailState())
    val uiState: StateFlow<BookDetailState> = _uiState.asStateFlow()

    /**
     * Descarga la información detallada del libro desde la base de datos o desde la API.
     * @param bookId Identificador único del libro.
     */
    fun loadBook(bookId: String) {
        viewModelScope.launch {
            // 1. Cargamos caché del escáner (Rápido)
            val cachedBook = BooksRepository.lastScannedBook
            if (cachedBook?.id == bookId) {
                _uiState.update { it.copy(isLoading = false, book = cachedBook, error = null) }
                // Mantenemos la caché un momento por si la API viene vacía
            } else {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }

            // 2. Pedimos detalles completos (Lento)
            val result = booksRepository.getBookDetail(bookId)

            if (result.isSuccess) {
                val fetchedBook = result.getOrNull()
                _uiState.update { currentState ->
                    val previousBook = currentState.book

                    val finalBook = fetchedBook?.copy(
                        description = if (fetchedBook.description.length < 50 && (previousBook?.description?.length ?: 0) > 50)
                            previousBook!!.description else fetchedBook.description,
                        imageUrl = fetchedBook.imageUrl.ifBlank { previousBook?.imageUrl ?: "" },
                        authors = fetchedBook.authors.ifEmpty { previousBook?.authors ?: emptyList() }
                    ) ?: previousBook

                    currentState.copy(book = finalBook, isLoading = false)
                }
                BooksRepository.lastScannedBook = null // Limpiamos ahora sí
                checkUserLists(bookId)
            } else {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message, isLoading = false) }
            }
        }
    }

    /**
     * Comprueba en qué listas privadas del usuario se encuentra este libro.
     * * TÉCNICA AVANZADA (Corrutinas en paralelo): Utiliza [async] para lanzar las tres
     * consultas a Firebase al mismo tiempo y luego espera los resultados con [.await()],
     * reduciendo el tiempo de carga a una tercera parte.
     */
    private fun checkUserLists(bookId: String) {
        val uid = userRepository.getCurrentUserId() ?: return

        viewModelScope.launch {
            try {
                // Lanzamos las 3 peticiones a la vez
                val readDeferred = async { progressRepository.getReadBooks(uid).getOrDefault(emptyList()) }
                val favsDeferred = async { userRepository.getFavoriteIds(uid).getOrDefault(emptyList()) }
                val marksDeferred = async { progressRepository.getBookmarks(uid).getOrDefault(emptyList()) }

                // Esperamos a que las 3 terminen
                val read = readDeferred.await()
                val favs = favsDeferred.await()
                val marks = marksDeferred.await()

                val isFav = favs.contains(bookId)

                // Lógica excluyente: Un libro puede ser Favorito y estar Leído,
                // pero no puede estar "Leído" y "Pendiente" a la vez.
                val list = when {
                    read.any { it.id == bookId } -> "Leídos"
                    marks.any { it.bookId == bookId } -> "Pendientes"
                    else -> null
                }

                _uiState.update { it.copy(isFavorite = isFav, savedInList = list) }

            } catch (e: Exception) {
                Log.e("BookDetailVM", "Error comprobando listas: ${e.message}")
            }
        }
    }

    /**
     * Alterna el estado de Favorito (Corazón) del libro actual.
     * * Utiliza una "Actualización Optimista": Cambia el color del botón en la UI instantáneamente
     * y luego envía la petición a Firebase en segundo plano. Si falla, revierte el color.
     */
    fun toggleFavorite(book: Book) {
        val currentState = _uiState.value.isFavorite
        val newState = !currentState

        // Actualizamos la UI inmediatamente para que se sienta rápido
        _uiState.update { it.copy(isFavorite = newState) }

        viewModelScope.launch {
            try {
                // Aseguramos que el libro exista físicamente en la BD antes de vincularlo
                booksRepository.ensureBookExists(book)

                if (newState) {
                    progressRepository.toggleFavorite(book, true)
                } else {
                    progressRepository.deleteUserSubdocument("favorites", book.id)
                }
            } catch (e: Exception) {
                // Si hay error de red, revertimos el botón a su estado original
                Log.e("BookDetailVM", "Error al cambiar favorito: ${e.message}")
                _uiState.update { it.copy(isFavorite = currentState) }
            }
        }
    }

    /** Comprueba si el usuario tiene su email verificado (Requisito de seguridad para poder escribir). */
    fun checkEmailVerification(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            authRepository.reloadUser()
            onResult(authRepository.isEmailVerified())
        }
    }

    /**
     * Añade el libro a la lista de "Leídos" o "Pendientes".
     * * Maneja la lógica de exclusión: Si lo añades a "Leídos", se borra automáticamente de "Pendientes" y viceversa.
     */
    fun addToList(book: Book, listName: String) {
        viewModelScope.launch {
            try {
                // Guardamos el libro (Caché local)
                booksRepository.ensureBookExists(book)

                if (listName == "Leídos") {
                    progressRepository.deleteUserSubdocument("bookmarks", book.id)
                    progressRepository.markAsRead(book)
                    _uiState.update { it.copy(savedInList = "Leídos") }

                } else if (listName == "Pendientes") {
                    progressRepository.deleteUserSubdocument("read_books", book.id)
                    progressRepository.saveBookmark(book, "", "", "", false)
                    _uiState.update { it.copy(savedInList = "Pendientes") }
                }
            } catch (e: Exception) {
                Log.e("BookDetailVM", "Error añadiendo a lista: ${e.message}")
            }
        }
    }

    /** Elimina el libro de las listas de estado ("Leídos" o "Pendientes"). */
    fun removeFromList(bookId: String, listName: String) {
        viewModelScope.launch {
            val collection = when (listName) {
                "Leídos" -> "read_books"
                "Pendientes" -> "bookmarks"
                else -> return@launch
            }

            progressRepository.deleteUserSubdocument(collection, bookId)
            _uiState.update { it.copy(savedInList = null) }
        }
    }

    /** Publica una reseña general (Estrellas + Texto) en la comunidad. */
    fun saveReview(book: Book, rating: Int, text: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            booksRepository.ensureBookExists(book)
            progressRepository.saveReview(book, rating, text)
                .onSuccess { onSuccess() }
        }
    }

    /** Publica un comentario en el hilo de discusión del libro. */
    fun saveComment(book: Book, text: String, chapter: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            booksRepository.ensureBookExists(book)
            progressRepository.saveComment(book, text, chapter)
                .onSuccess { onSuccess() }
        }
    }

    /** Guarda un marcador privado (Cita, Página, Capítulo). Lo marca automáticamente como "Pendiente". */
    fun saveBookmark(
        book: Book,
        page: String,
        quote: String,
        chapter: String,
        isPublic: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            booksRepository.ensureBookExists(book)
            // Si estaba en leídos, lo quitamos porque acaba de guardar por donde va
            progressRepository.deleteUserSubdocument("read_books", book.id)

            progressRepository.saveBookmark(book, quote, chapter, page, isPublic)
                .onSuccess {
                    _uiState.update { it.copy(savedInList = "Pendientes") }
                    onSuccess()
                }
        }
    }

    // --- SISTEMA SOCIAL DE SAGAS COMUNITARIAS ---

    /**
     * Permite al usuario proponer una corrección sobre la saga del libro.
     * * Guarda no solo los nuevos datos, sino también el nombre y avatar del usuario
     * para darle crédito como "Editor" de la comunidad.
     */
    fun editSeries(newName: String, newIndex: Int, onSuccess: () -> Unit) {
        val currentBook = _uiState.value.book ?: return
        val currentUser = authRepository.currentUser ?: return

        viewModelScope.launch {
            // Obtenemos los datos bonitos del usuario desde tu UserRepository
            val userProfile = userRepository.getUserProfile(currentUser.uid).getOrNull()
            val editorName = userProfile?.displayName ?: "Usuario"
            val editorAvatar = userProfile?.photoURL ?: "capibara_1"

            booksRepository.updateBookSeries(
                book = currentBook,
                newName = newName,
                newIndex = newIndex,
                editorUid = currentUser.uid,
                editorName = editorName,
                editorAvatar = editorAvatar
            ).onSuccess {
                // Actualizamos la UI localmente de forma optimista para no tener que recargar de internet
                _uiState.update {
                    it.copy(book = currentBook.copy(
                        seriesName = newName, seriesIndex = newIndex,
                        seriesEditorName = editorName, seriesEditorAvatar = editorAvatar,
                        seriesEditDate = System.currentTimeMillis(),
                        seriesUpvotes = 0, seriesDownvotes = 0
                    ))
                }
                onSuccess()
            }
        }
    }

    /**
     * Permite a la comunidad votar si una edición de saga es correcta (Upvote) o incorrecta (Downvote).
     * * Implementa una comprobación para asegurar que un usuario solo pueda votar una vez.
     */
    fun voteSeriesEdit(isUpvote: Boolean) {
        val currentBook = _uiState.value.book ?: return
        val uid = authRepository.currentUser?.uid ?: return

        // Medida antitrolls: Si ya votó, bloqueamos la acción silenciosamente
        if (currentBook.seriesVoters.contains(uid)) return

        viewModelScope.launch {
            // Actualización optimista en la UI (sumamos el voto en pantalla al instante)
            val newUpvotes = if (isUpvote) currentBook.seriesUpvotes + 1 else currentBook.seriesUpvotes
            val newDownvotes = if (!isUpvote) currentBook.seriesDownvotes + 1 else currentBook.seriesDownvotes
            val newVoters = currentBook.seriesVoters + uid

            _uiState.update { it.copy(book = currentBook.copy(
                seriesUpvotes = newUpvotes, seriesDownvotes = newDownvotes, seriesVoters = newVoters
            ))}

            // Enviamos el voto real a la base de datos
            booksRepository.voteSeriesEdit(currentBook.id, uid, isUpvote)
        }
    }
}