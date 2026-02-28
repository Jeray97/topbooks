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

class SettingsManager(context: Context) {

    private val dataStore = context.dataStore

    companion object {
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode_enabled")
        private val NOTIFICATIONS_KEY = booleanPreferencesKey("notifications_enabled")
        // 🟢 NUEVA CLAVE: Para saber si los diarios son públicos por defecto
        private val PUBLIC_JOURNAL_DEFAULT_KEY = booleanPreferencesKey("public_journal_default")
    }

    val darkModeFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[DARK_MODE_KEY] ?: false
        }

    val notificationsFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[NOTIFICATIONS_KEY] ?: true
        }

    // 🟢 NUEVO FLUJO: Leer preferencia de privacidad (por defecto falso/privado)
    val publicJournalDefaultFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PUBLIC_JOURNAL_DEFAULT_KEY] ?: false
        }

    suspend fun saveDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }

    suspend fun saveNotifications(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_KEY] = enabled
        }
    }

    // 🟢 NUEVA FUNCIÓN: Guardar preferencia de privacidad
    suspend fun savePublicJournalDefault(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PUBLIC_JOURNAL_DEFAULT_KEY] = enabled
        }
    }
}