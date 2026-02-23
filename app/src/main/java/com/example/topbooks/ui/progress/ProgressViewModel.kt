package com.example.topbooks.ui.progress

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.ui.profile.SimpleBook
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ProgressState(
    val journals: List<SimpleBook> = emptyList(),
    val favorites: List<SimpleBook> = emptyList(),
    val pending: List<SimpleBook> = emptyList(),
    val read: List<SimpleBook> = emptyList(),
    val isLoading: Boolean = true
)

class ProgressViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(ProgressState())
    val uiState: StateFlow<ProgressState> = _uiState.asStateFlow()

    init {
        loadProgressData()
    }

    fun loadProgressData() {
        val uid = auth.currentUser?.uid ?: return
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                // Ejecutamos las 4 búsquedas al mismo tiempo para que la pantalla cargue volando
                val journalsDef = async { fetchJournals(uid) }
                val favsDef = async { fetchFavorites(uid) }
                val pendingDef = async { fetchBookmarks(uid) }
                val readDef = async { fetchRead(uid) }

                _uiState.update {
                    it.copy(
                        journals = journalsDef.await(),
                        favorites = favsDef.await(),
                        pending = pendingDef.await(),
                        read = readDef.await(),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("ProgressVM", "Error cargando progreso: ${e.message}")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun fetchJournals(uid: String): List<SimpleBook> {
        val snap = db.collection("users").document(uid).collection("journals").get().await()
        return snap.documents.mapNotNull { doc ->
            SimpleBook(
                id = doc.getString("bookId") ?: doc.id,
                title = doc.getString("bookTitle") ?: "Diario",
                imageUrl = doc.getString("bookImageUrl") ?: ""
            )
        }
    }

    private suspend fun fetchFavorites(uid: String): List<SimpleBook> {
        val snap = db.collection("users").document(uid).collection("favorites")
            .whereEqualTo("list", "Favoritos").get().await()
        return snap.documents.mapNotNull { doc ->
            SimpleBook(
                id = doc.getString("bookId") ?: doc.id,
                title = doc.getString("title") ?: "Libro",
                imageUrl = doc.getString("imageUrl") ?: ""
            )
        }
    }

    private suspend fun fetchBookmarks(uid: String): List<SimpleBook> {
        val snap = db.collection("users").document(uid).collection("bookmarks").get().await()
        val raw = snap.documents.mapNotNull { doc ->
            SimpleBook(
                id = doc.getString("bookId") ?: doc.id,
                title = "Pendiente",
                imageUrl = ""
            )
        }
        return enrichWithGlobalBooks(raw)
    }

    private suspend fun fetchRead(uid: String): List<SimpleBook> {
        val snap = db.collection("users").document(uid).collection("read_books").get().await()
        return snap.documents.mapNotNull { doc ->
            SimpleBook(
                id = doc.id,
                title = doc.getString("title") ?: "Libro",
                imageUrl = doc.getString("imageUrl") ?: ""
            )
        }
    }

    // --- FUNCIÓN CORREGIDA ---
    // Usamos coroutineScope y envolvermos cada iteración del map en un bloque async
    // para poder usar el awaitAll() de forma correcta.
    private suspend fun enrichWithGlobalBooks(list: List<SimpleBook>): List<SimpleBook> = coroutineScope {
        list.map { book ->
            async {
                var current = book
                try {
                    val bookDoc = db.collection("books").document(book.id).get().await()
                    if (bookDoc.exists()) {
                        current = current.copy(
                            title = bookDoc.getString("title") ?: book.title,
                            imageUrl = bookDoc.getString("thumbnail") ?: book.imageUrl
                        )
                    }
                } catch (e: Exception) {
                    Log.e("ProgressVM", "Error al buscar detalles del marcador: ${e.message}")
                }
                current
            }
        }.awaitAll()
    }
}