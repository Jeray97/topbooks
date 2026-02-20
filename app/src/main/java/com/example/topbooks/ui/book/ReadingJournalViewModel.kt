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

    fun saveJournal(journal: Journal) {
        Log.d("JournalDebug", "1. Iniciando guardado... BookID: ${journal.bookId}")

        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.e("JournalDebug", "ERROR FATAL: El usuario no está logueado (UID es null)")
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            Log.d("JournalDebug", "2. Estado de carga activado (isSaving = true).")

            try {
                val finalJournal = journal.copy(userId = uid)
                val path = "users/$uid/journals/${journal.bookId}"
                Log.d("JournalDebug", "3. Intentando escribir en la ruta de Firebase: $path")

                db.collection("users").document(uid)
                    .collection("journals").document(journal.bookId)
                    .set(finalJournal).await()

                Log.d("JournalDebug", "4. ¡ESCRITURA EXITOSA EN FIREBASE!")
                _saveSuccess.value = true

            } catch (e: Exception) {
                Log.e("JournalDebug", "5. ERROR CATCH: Falló la escritura en Firebase.", e)
                Log.e("JournalDebug", "Mensaje de error: ${e.message}")
            } finally {
                _isSaving.value = false
                Log.d("JournalDebug", "6. Fin del proceso de guardado (isSaving = false).")
            }
        }
    }

    // Método vital para evitar que nos expulse nada más entrar
    fun resetSuccessState() {
        _saveSuccess.value = false
    }
}