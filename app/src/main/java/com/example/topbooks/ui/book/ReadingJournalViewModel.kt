package com.example.topbooks.ui.book

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Journal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ReadingJournalViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    private val _existingJournal = MutableStateFlow<Journal?>(null)
    val existingJournal: StateFlow<Journal?> = _existingJournal.asStateFlow()

    private val _isLoadingJournal = MutableStateFlow(false)
    val isLoadingJournal: StateFlow<Boolean> = _isLoadingJournal.asStateFlow()

    fun saveJournal(journal: Journal) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val finalJournal = journal.copy(userId = uid)
                db.collection("users").document(uid)
                    .collection("journals").document(journal.bookId)
                    .set(finalJournal).await()

                Log.d("JournalDebug", "¡Guardado con éxito en Firestore! Título: ${journal.title}")
                _saveSuccess.value = true
            } catch (e: Exception) {
                Log.e("JournalDebug", "Error al guardar el diario: ${e.message}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun loadJournal(bookId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoadingJournal.value = true

            // Forzamos el borrado de la memoria para que no recicle datos viejos
            _existingJournal.value = null

            try {
                Log.d("JournalDebug", "Buscando diario con ID: $bookId")
                val doc = db.collection("users").document(uid)
                    .collection("journals").document(bookId).get().await()

                if (doc.exists()) {
                    val loadedJournal = doc.toObject(Journal::class.java)
                    Log.d("JournalDebug", "¡Diario encontrado! Título cargado: ${loadedJournal?.title}")
                    _existingJournal.value = loadedJournal
                } else {
                    Log.d("JournalDebug", "No existe un diario previo. Se creará uno nuevo.")
                    _existingJournal.value = null
                }
            } catch (e: Exception) {
                Log.e("JournalDebug", "Error al descargar el diario: ${e.message}")
            } finally {
                _isLoadingJournal.value = false
            }
        }
    }

    fun resetSuccessState() {
        _saveSuccess.value = false
    }
}