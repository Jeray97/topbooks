package com.example.topbooks.ui.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.model.Journal
import com.example.topbooks.data.repository.JournalRepository
import com.example.topbooks.data.repository.JournalRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 1. ESTADO DE LA UI
 * * Agrupa toda la información que la pantalla [ReadingJournalScreen] necesita para pintarse correctamente.
 *
 * @property existingJournal El diario de lectura existente (si el usuario ya lo había creado antes).
 * @property isLoadingJournal Indica si la aplicación está descargando el diario desde Firebase.
 * @property isSaving Indica si la aplicación está actualmente guardando o borrando el diario en la nube.
 * @property saveSuccess Si es 'true', le indica a la vista que la operación terminó con éxito para que navegue hacia atrás.
 * @property errorMessage Mensaje de error a mostrar si falla alguna operación de red.
 */
data class JournalUiState(
    val existingJournal: Journal? = null,
    val isLoadingJournal: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel que gestiona la lógica de la pantalla del Diario de Lectura.
 * * Conecta la UI con el repositorio de Diarios ([JournalRepository]) utilizando corrutinas.
 */
class ReadingJournalViewModel(
    private val repository: JournalRepository = JournalRepositoryImpl()
) : ViewModel() {

    // 2. INICIALIZAMOS EL STATEFLOW (Flujo reactivo de estado)
    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    /**
     * Intenta cargar un diario existente desde Firebase.
     * @param bookId ID del libro del que queremos buscar un diario.
     */
    fun loadJournal(bookId: String) {
        viewModelScope.launch {
            // Mostramos la rueda de carga y limpiamos errores y diarios previos
            _uiState.update { it.copy(isLoadingJournal = true, existingJournal = null, errorMessage = null) }

            repository.getJournal(bookId).onSuccess { journal ->
                // Si lo encontramos (o si no existe pero la petición fue exitosa, journal será null), actualizamos
                _uiState.update { it.copy(isLoadingJournal = false, existingJournal = journal) }
            }.onFailure { error ->
                // Si la red falla, mostramos el error
                _uiState.update { it.copy(isLoadingJournal = false, errorMessage = error.message) }
            }
        }
    }

    /**
     * Guarda o sobrescribe un diario en la base de datos de Firebase.
     * @param journal Objeto con todos los datos recogidos del formulario visual.
     */
    fun saveJournal(journal: Journal) {
        viewModelScope.launch {
            // Activamos el estado de guardado (para bloquear botones y mostrar indicador)
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            repository.saveJournal(journal).onSuccess {
                // Si fue bien, activamos 'saveSuccess' para que la UI sepa que debe volver atrás
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            }.onFailure { error ->
                _uiState.update { it.copy(isSaving = false, errorMessage = error.message) }
            }
        }
    }

    /**
     * Borra el diario asociado a un libro de la base de datos de Firebase.
     * @param bookId ID del libro cuyo diario queremos borrar.
     */
    fun deleteJournal(bookId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            repository.deleteJournal(bookId).onSuccess {
                // TÉCNICA: Reutilizamos saveSuccess = true para que la UI automáticamente
                // lance el 'onBackClick()' y saque al usuario de la pantalla tras borrar el diario.
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            }.onFailure { error ->
                _uiState.update { it.copy(isSaving = false, errorMessage = error.message) }
            }
        }
    }

    /**
     * Restablece el estado de éxito a 'false'.
     * * Esto es importante para evitar que, si el usuario vuelve a entrar en el futuro,
     * la pantalla se cierre inmediatamente por recordar este valor en 'true'.
     */
    fun resetSuccessState() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    /** Limpia los mensajes de error para que dejen de mostrarse en la UI. */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}