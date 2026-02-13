package com.example.topbooks.ui.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.preferences.SettingsManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel que conecta la UI con la lógica de persistencia y sesión.
 */
class ConfigViewModel(private val settingsManager: SettingsManager) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    val darkModeEnabled: StateFlow<Boolean> = settingsManager.darkModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val notificationsEnabled: StateFlow<Boolean> = settingsManager.notificationsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.saveDarkMode(enabled)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.saveNotifications(enabled)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    class Factory(private val settingsManager: SettingsManager) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ConfigViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ConfigViewModel(settingsManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}