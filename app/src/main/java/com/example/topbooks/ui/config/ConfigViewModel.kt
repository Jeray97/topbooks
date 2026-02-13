package com.example.topbooks.ui.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.topbooks.data.preferences.SettingsManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel que conecta la UI con la lógica de persistencia.
 * Expone StateFlows para que la UI se repinte automáticamente al cambiar los datos.
 */
class ConfigViewModel(private val settingsManager: SettingsManager) : ViewModel() {

    // Convertimos los Flows en StateFlows.
    // WhileSubscribed(5000) detiene la observación si la pantalla no es visible tras 5 segundos.
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

    /**
     * Factory necesario para inyectar el SettingsManager manualmente
     * ya que ViewModel() por defecto no recibe parámetros.
     */
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