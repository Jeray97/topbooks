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

data class JournalUiState(
    val existingJournal: Journal? = null,
    val isLoadingJournal: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

class ReadingJournalViewModel(
    private val repository: JournalRepository = JournalRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    fun loadJournal(bookId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingJournal = true, existingJournal = null, errorMessage = null) }

            repository.getJournal(bookId).onSuccess { journal ->
                _uiState.update { it.copy(isLoadingJournal = false, existingJournal = journal) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoadingJournal = false, errorMessage = error.message) }
            }
        }
    }

    fun saveJournal(journal: Journal) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            repository.saveJournal(journal).onSuccess {
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            }.onFailure { error ->
                _uiState.update { it.copy(isSaving = false, errorMessage = error.message) }
            }
        }
    }

    fun deleteJournal(bookId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            repository.deleteJournal(bookId).onSuccess {
                // Reutilizamos saveSuccess para navegar hacia atrás
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            }.onFailure { error ->
                _uiState.update { it.copy(isSaving = false, errorMessage = error.message) }
            }
        }
    }

    fun resetSuccessState() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}