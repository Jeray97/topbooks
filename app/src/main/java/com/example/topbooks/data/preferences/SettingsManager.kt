package com.example.topbooks.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Extensión para inicializar DataStore (solo una vez en la app)
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

/**
 * Gestor de preferencias basado en Jetpack DataStore.
 * Sustituye a SharedPreferences ofreciendo seguridad de hilos y reactividad con Flows.
 */
class SettingsManager(context: Context) {

    private val dataStore = context.dataStore

    companion object {
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode_enabled")
        private val NOTIFICATIONS_KEY = booleanPreferencesKey("notifications_enabled")
    }

    // Leer el estado del Modo Oscuro (por defecto falso)
    val darkModeFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[DARK_MODE_KEY] ?: false
        }

    // Leer el estado de las Notificaciones (por defecto verdadero)
    val notificationsFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[NOTIFICATIONS_KEY] ?: true
        }

    // Guardar Modo Oscuro
    suspend fun saveDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }

    // Guardar Notificaciones
    suspend fun saveNotifications(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_KEY] = enabled
        }
    }
}